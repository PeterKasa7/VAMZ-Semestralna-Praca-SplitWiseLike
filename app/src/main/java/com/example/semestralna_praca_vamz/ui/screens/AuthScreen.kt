package com.example.semestralna_praca_vamz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.semestralna_praca_vamz.R
import com.example.semestralna_praca_vamz.ui.SplitViewModel

@Composable
fun AuthScreen(viewModel: SplitViewModel, onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isLogin) "Prihlásenie" else "Registrácia", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Meno") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Heslo") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLogin) {
                    viewModel.login(email, password, onAuthSuccess)
                } else {
                    viewModel.register(email, password, name, onAuthSuccess)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Prihlásiť sa" else "Registrovať sa")
        }

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Nemáte účet? Registrujte sa" else "Už máte účet? Prihláste sa")
        }

        if (viewModel.authError != null) {
            AlertDialog(
                onDismissRequest = { viewModel.authError = null },
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(stringResource(R.string.error_login_failed)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.authError = null }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}
