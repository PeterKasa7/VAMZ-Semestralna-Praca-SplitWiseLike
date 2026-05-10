package com.example.semestralna_praca_vamz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.semestralna_praca_vamz.R
import com.example.semestralna_praca_vamz.data.db.*
import com.example.semestralna_praca_vamz.ui.SplitViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: SplitViewModel, 
    groupId: Long, 
    onBackClick: () -> Unit,
    expenseId: Long? = null
) {
    val groups by viewModel.groups.collectAsState()
    val group = groups.find { it.id == groupId } ?: return
    val members by viewModel.getGroupMembers(groupId).collectAsState(initial = emptyList())

    var description by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedPayerId by rememberSaveable { mutableStateOf(-1L) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }

    val customInputs = remember { mutableStateMapOf<Long, String>() }
    var participantsIds by remember { mutableStateOf(emptyList<Long>()) }
    var expandedPayer by remember { mutableStateOf(false) }
    
    var isInitialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(members) {
        if (!isInitialized) {
            if (expenseId != null) {
                val expense = viewModel.getExpenseById(expenseId)
                if (expense != null) {
                    description = expense.description
                    amountText = (expense.amountCents / 100.0).toString()
                    selectedPayerId = expense.paidByUserId
                    splitType = expense.splitType
                    
                    val shares = viewModel.getSharesForExpense(expenseId)
                    participantsIds = shares.map { it.userId }
                    shares.forEach { share ->
                        val value = if (splitType == SplitType.EXACT) (share.owedAmountCents / 100.0).toString()
                                    else ((share.owedAmountCents.toDouble() / expense.amountCents) * 100).toString()
                        customInputs[share.userId] = value
                    }
                }
            } else {
                if (selectedPayerId == -1L && members.isNotEmpty()) {
                    selectedPayerId = members.first().id
                }
                if (participantsIds.isEmpty() && members.isNotEmpty()) {
                    participantsIds = members.map { it.id }
                }
            }
            isInitialized = true
        }
    }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0
    val currentSum = if (splitType != SplitType.EQUAL) {
        participantsIds.sumOf { customInputs[it]?.toDoubleOrNull() ?: 0.0 }
    } else totalAmount

    val isAmountValid = when (splitType) {
        SplitType.EQUAL -> totalAmount > 0
        SplitType.EXACT -> abs(currentSum - totalAmount) < 0.01 && totalAmount > 0
        SplitType.PERCENTAGE -> abs(currentSum - 100.0) < 0.01 && totalAmount > 0
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (expenseId == null) R.string.add_expense else R.string.edit_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.amount_euro)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.who_paid), style = MaterialTheme.typography.labelLarge)
            Box(modifier = Modifier.fillMaxWidth()) {
                val payerName = members.find { it.id == selectedPayerId }?.name ?: stringResource(R.string.select_member)
                OutlinedCard(onClick = { expandedPayer = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = payerName, modifier = Modifier.padding(16.dp))
                }
                DropdownMenu(expanded = expandedPayer, onDismissRequest = { expandedPayer = false }) {
                    members.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name) },
                            onClick = {
                                selectedPayerId = person.id
                                expandedPayer = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.split_type), style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SplitType.entries.forEach { type ->
                    FilterChip(
                        selected = splitType == type,
                        onClick = { splitType = type },
                        label = {
                            Text(
                                when (type) {
                                    SplitType.EQUAL -> stringResource(R.string.equally)
                                    SplitType.EXACT -> stringResource(R.string.amounts)
                                    SplitType.PERCENTAGE -> stringResource(R.string.percentage)
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.split_among), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (splitType != SplitType.EQUAL) {
                    val label = if (splitType == SplitType.EXACT) stringResource(R.string.total_label_exact, currentSum, totalAmount) 
                                else stringResource(R.string.total_label_percent, currentSum)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAmountValid) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
            
            members.forEach { person ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = participantsIds.contains(person.id),
                        onCheckedChange = { checked ->
                            participantsIds = if (checked) participantsIds + person.id else participantsIds - person.id
                        }
                    )
                    Text(text = person.name, modifier = Modifier.weight(1f))

                    if (splitType != SplitType.EQUAL && participantsIds.contains(person.id)) {
                        OutlinedTextField(
                            value = customInputs[person.id] ?: "",
                            onValueChange = { customInputs[person.id] = it },
                            modifier = Modifier.width(90.dp),
                            label = { Text(if (splitType == SplitType.EXACT) "€" else "%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val details = mutableMapOf<Long, Double>()
                    if (splitType != SplitType.EQUAL) {
                        participantsIds.forEach { id ->
                            details[id] = customInputs[id]?.toDoubleOrNull() ?: 0.0
                        }
                    }

                    if (description.isNotBlank() && isAmountValid && participantsIds.isNotEmpty()) {
                        viewModel.addExpenseToGroup(
                            groupId, description, totalAmount, selectedPayerId,
                            participantsIds, splitType, details, expenseId
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = description.isNotBlank() && isAmountValid && participantsIds.isNotEmpty()
            ) {
                Text(stringResource(if (expenseId == null) R.string.save_expense else R.string.update_expense))
            }
        }
    }
}
