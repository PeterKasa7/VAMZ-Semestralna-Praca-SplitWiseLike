package com.example.semestralna_praca_vamz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.semestralna_praca_vamz.R
import com.example.semestralna_praca_vamz.data.db.ExpenseEntity
import com.example.semestralna_praca_vamz.data.db.UserEntity
import com.example.semestralna_praca_vamz.ui.SplitViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: SplitViewModel,
    groupId: Long,
    onBackClick: () -> Unit,
    onAddExpenseClick: (Long) -> Unit,
    onEditExpenseClick: (Long, Long) -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val group = groups.find { it.id == groupId } ?: return
    
    val members by viewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())
    val expenses by viewModel.getExpenses(groupId).collectAsState(initial = emptyList())
    
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_member))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddExpenseClick(groupId) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SummarySection(viewModel, groupId, members)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(R.string.last_expenses), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            ExpenseList(expenses, members, viewModel, onEditExpenseClick)
        }

        if (showAddMemberDialog) {
            AlertDialog(
                onDismissRequest = { showAddMemberDialog = false },
                title = { Text(stringResource(R.string.new_member)) },
                text = {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text(stringResource(R.string.name)) }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newMemberName.isNotBlank()) {
                            viewModel.addMemberToGroup(groupId, newMemberName)
                            newMemberName = ""
                            showAddMemberDialog = false
                        }
                    }) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMemberDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun SummarySection(viewModel: SplitViewModel, groupId: Long, members: List<UserEntity>) {
    var balances by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }
    
    LaunchedEffect(groupId, members) {
        balances = viewModel.getBalances(groupId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(R.string.balances_overview), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            members.forEach { person ->
                val balance = balances[person.id] ?: 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = person.name)
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f €", balance),
                        color = if (balance >= 0) Color(0xFF4CAF50) else Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseList(
    expenses: List<ExpenseEntity>, 
    members: List<UserEntity>, 
    viewModel: SplitViewModel,
    onEditExpenseClick: (Long, Long) -> Unit
) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_expenses), color = Color.Gray)
        }
    } else {
        LazyColumn {
            items(expenses.size) { index ->
                val expense = expenses[index]
                val payerName = members.find { it.id == expense.paidByUserId }?.name ?: "Neznámy"
                ListItem(
                    headlineContent = { Text(expense.description) },
                    supportingContent = { Text(stringResource(R.string.paid_by, payerName)) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f €", expense.amountCents / 100.0),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onEditExpenseClick(expense.groupId, expense.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
