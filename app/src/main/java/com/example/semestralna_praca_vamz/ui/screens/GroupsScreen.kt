package com.example.semestralna_praca_vamz.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.semestralna_praca_vamz.R
import com.example.semestralna_praca_vamz.data.db.GroupEntity
import com.example.semestralna_praca_vamz.ui.SplitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(viewModel: SplitViewModel, onGroupClick: (Long) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(false) }
    val groups by viewModel.groups.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.my_groups)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_group))
            }
        }
    ) { paddingValues ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_groups))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(groups) { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteGroup(group) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_group))
                            }
                        },
                        modifier = Modifier.clickable { onGroupClick(group.id) }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.new_group)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text(stringResource(R.string.group_name)) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it }
                            )
                            Text(text = stringResource(R.string.enable_notifications))
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.addGroup(newGroupName, notificationsEnabled)
                            newGroupName = ""
                            notificationsEnabled = false
                            showDialog = false
                        }
                    }) {
                        Text(stringResource(R.string.create))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
