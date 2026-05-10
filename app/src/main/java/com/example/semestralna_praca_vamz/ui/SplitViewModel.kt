package com.example.semestralna_praca_vamz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.semestralna_praca_vamz.data.db.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SplitViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).splitDao()
    private val context = application.applicationContext

    val groups: StateFlow<List<GroupEntity>> = dao.getAllGroupsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGroup(name: String, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            dao.insertGroup(GroupEntity(name = name, notificationsEnabled = notificationsEnabled))
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            dao.deleteGroup(group)
        }
    }

    fun getGroupMembers(groupId: Long): Flow<List<UserEntity>> = dao.getGroupMembersFlow(groupId)

    fun addMemberToGroup(groupId: Long, name: String) {
        viewModelScope.launch {
            val userId = dao.insertUser(UserEntity(name = name))
            dao.insertGroupMember(GroupMemberEntity(groupId, userId))
        }
    }

    fun getExpenses(groupId: Long): Flow<List<ExpenseEntity>> = dao.getExpensesForGroupFlow(groupId)

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            dao.deleteSharesForExpense(expense.id)
            dao.deleteExpense(expense)
        }
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? = dao.getExpenseById(id)
    suspend fun getSharesForExpense(expenseId: Long): List<ExpenseShareEntity> = dao.getSharesForExpense(expenseId)

    fun addExpenseToGroup(
        groupId: Long,
        description: String,
        amount: Double,
        payerId: Long,
        participantsIds: List<Long>,
        splitType: SplitType,
        splitDetails: Map<Long, Double>,
        existingExpenseId: Long? = null
    ) {
        viewModelScope.launch {
            val amountCents = (amount * 100).toLong()
            
            val expenseId = if (existingExpenseId == null) {
                dao.insertExpense(
                    ExpenseEntity(
                        groupId = groupId,
                        paidByUserId = payerId,
                        amountCents = amountCents,
                        description = description,
                        splitType = splitType
                    )
                )
            } else {
                val existing = dao.getExpenseById(existingExpenseId)
                if (existing != null) {
                    dao.updateExpense(existing.copy(
                        paidByUserId = payerId,
                        amountCents = amountCents,
                        description = description,
                        splitType = splitType
                    ))
                    dao.deleteSharesForExpense(existingExpenseId)
                }
                existingExpenseId
            }

            val shares = when (splitType) {
                SplitType.EQUAL -> {
                    val shareCents = amountCents / participantsIds.size
                    participantsIds.map { id ->
                        ExpenseShareEntity(expenseId, id, shareCents)
                    }
                }
                SplitType.EXACT -> {
                    participantsIds.map { id ->
                        val shareCents = ((splitDetails[id] ?: 0.0) * 100).toLong()
                        ExpenseShareEntity(expenseId, id, shareCents)
                    }
                }
                SplitType.PERCENTAGE -> {
                    participantsIds.map { id ->
                        val percent = splitDetails[id] ?: 0.0
                        val shareCents = (amountCents * (percent / 100.0)).toLong()
                        ExpenseShareEntity(expenseId, id, shareCents)
                    }
                }
            }
            dao.insertExpenseShares(shares)

            // Trigger notification if enabled
            val group = groups.value.find { it.id == groupId }
            if (group?.notificationsEnabled == true) {
                val balances = getBalances(groupId)
                val members = dao.getGroupMembersFlow(groupId).first()
                val namedBalances = balances.mapKeys { entry -> 
                    members.find { it.id == entry.key }?.name ?: "Neznámy"
                }
                NotificationHelper.sendDebtNotification(context, group.name, namedBalances)
            }
        }
    }

    suspend fun getBalances(groupId: Long): Map<Long, Double> {
        val groupData = dao.getGroupWithData(groupId) ?: return emptyMap()
        val balances = mutableMapOf<Long, Long>() // ID to cents
        groupData.members.forEach { balances[it.id] = 0L }

        groupData.expenses.forEach { expense ->
            balances[expense.paidByUserId] = (balances[expense.paidByUserId] ?: 0L) + expense.amountCents
            val shares = dao.getSharesForExpense(expense.id)
            shares.forEach { share ->
                balances[share.userId] = (balances[share.userId] ?: 0L) - share.owedAmountCents
            }
        }
        
        return balances.mapValues { it.value / 100.0 }
    }
    
    fun getUserFlow(userId: Long): Flow<UserEntity?> = flow {
        emit(dao.getUserById(userId))
    }
}
