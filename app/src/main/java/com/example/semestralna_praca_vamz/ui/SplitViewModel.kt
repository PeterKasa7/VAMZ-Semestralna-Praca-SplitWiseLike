package com.example.semestralna_praca_vamz.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.semestralna_praca_vamz.data.db.SplitType
import com.example.semestralna_praca_vamz.data.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SplitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val context = application.applicationContext

    // Reactive Auth State
    var currentUser by mutableStateOf(auth.currentUser)
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
    }

    // Real-time Groups
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<Group>> = snapshotFlow { currentUser }
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList<Group>())
            else callbackFlow {
                val subscription = db.collection("groups")
                    .whereArrayContains("members", user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("Firebase", "Groups error: ${error.message}")
                            return@addSnapshotListener
                        }
                        val groupsList = snapshot?.toObjects(Group::class.java) ?: emptyList()
                        trySend(groupsList)
                    }
                awaitClose { subscription.remove() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGroup(name: String, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            val userId = currentUser?.uid ?: return@launch
            val newGroupRef = db.collection("groups").document()
            val newGroup = Group(
                id = newGroupRef.id,
                name = name,
                notificationsEnabled = notificationsEnabled,
                members = listOf(userId)
            )
            newGroupRef.set(newGroup).await()
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            db.collection("groups").document(groupId).delete().await()
        }
    }

    fun getGroupMembers(groupId: String): Flow<List<User>> = callbackFlow {
        val subscription = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, _ ->
                val memberIds = snapshot?.get("members") as? List<String> ?: emptyList()
                if (memberIds.isNotEmpty()) {
                    db.collection("users").whereIn("id", memberIds)
                        .get()
                        .addOnSuccessListener { userSnapshots ->
                            trySend(userSnapshots.toObjects(User::class.java))
                        }
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { subscription.remove() }
    }

    fun addMemberToGroup(groupId: String, email: String) {
        viewModelScope.launch {
            val userSnapshot = db.collection("users").whereEqualTo("email", email).get().await()
            val user = userSnapshot.toObjects(User::class.java).firstOrNull() ?: return@launch
            
            val groupRef = db.collection("groups").document(groupId)
            val group = groupRef.get().await().toObject(Group::class.java) ?: return@launch
            
            if (!group.members.contains(user.id)) {
                groupRef.update("members", group.members + user.id).await()
            }
        }
    }

    fun getExpenses(groupId: String): Flow<List<Expense>> = callbackFlow {
        val subscription = db.collection("expenses")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Expenses error: ${error.message}")
                    return@addSnapshotListener
                }
                val expenses = snapshot?.toObjects(Expense::class.java) ?: emptyList()
                trySend(expenses.sortedByDescending { it.createdAt })
            }
        awaitClose { subscription.remove() }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            db.collection("expenses").document(expenseId).delete().await()
        }
    }

    suspend fun getExpenseById(id: String): Expense? {
        return db.collection("expenses").document(id).get().await().toObject(Expense::class.java)
    }

    fun addExpenseToGroup(
        groupId: String,
        description: String,
        amount: Double,
        payerId: String,
        participantsIds: List<String>,
        splitType: SplitType,
        splitDetails: Map<String, Double>,
        existingExpenseId: String? = null
    ) {
        viewModelScope.launch {
            val amountCents = (amount * 100).toLong()
            val shares = mutableMapOf<String, Long>()

            when (splitType) {
                SplitType.EQUAL -> {
                    val shareCents = amountCents / participantsIds.size
                    participantsIds.forEach { shares[it] = shareCents }
                }
                SplitType.EXACT -> {
                    participantsIds.forEach { shares[it] = ((splitDetails[it] ?: 0.0) * 100).toLong() }
                }
                SplitType.PERCENTAGE -> {
                    participantsIds.forEach { shares[it] = (amountCents * ((splitDetails[it] ?: 0.0) / 100.0)).toLong() }
                }
            }

            val expenseRef = if (existingExpenseId == null) {
                db.collection("expenses").document()
            } else {
                db.collection("expenses").document(existingExpenseId)
            }

            val expense = Expense(
                id = expenseRef.id,
                groupId = groupId,
                paidByUserId = payerId,
                amountCents = amountCents,
                description = description,
                createdAt = System.currentTimeMillis(),
                splitType = splitType,
                shares = shares
            )

            expenseRef.set(expense).await()

            // Trigger notification
            val group = db.collection("groups").document(groupId).get().await().toObject(Group::class.java)
            if (group?.notificationsEnabled == true) {
                val balances = getBalances(groupId)
                val members = getGroupMembers(groupId).first()
                val namedBalances = balances.mapKeys { entry -> 
                    members.find { it.id == entry.key }?.name ?: "Neznámy"
                }
                NotificationHelper.sendDebtNotification(context, group.name, namedBalances)
            }
        }
    }

    suspend fun getBalances(groupId: String): Map<String, Double> {
        val groupSnapshot = db.collection("groups").document(groupId).get().await()
        val group = groupSnapshot.toObject(Group::class.java) ?: return emptyMap()
        val expensesSnapshot = db.collection("expenses").whereEqualTo("groupId", groupId).get().await()
        val expenses = expensesSnapshot.toObjects(Expense::class.java)
        
        val balances = mutableMapOf<String, Long>() // ID to cents
        group.members.forEach { balances[it] = 0L }

        expenses.forEach { expense ->
            balances[expense.paidByUserId] = (balances[expense.paidByUserId] ?: 0L) + expense.amountCents
            expense.shares.forEach { (userId, amount) ->
                balances[userId] = (balances[userId] ?: 0L) - amount
            }
        }
        
        return balances.mapValues { it.value / 100.0 }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener {
            currentUser = it.user
            onSuccess()
        }
    }

    fun register(email: String, pass: String, name: String, onSuccess: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
            val userId = result.user?.uid ?: return@addOnSuccessListener
            val user = User(id = userId, name = name, email = email)
            db.collection("users").document(userId).set(user).addOnSuccessListener {
                currentUser = result.user
                onSuccess()
            }
        }
    }

    fun logout() {
        auth.signOut()
        currentUser = null
    }
}
