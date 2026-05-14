package com.example.grama_angana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.grama_angana.ui.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // Dynamic Validation for Phone Number
    val phoneError = remember(phoneNumber) {
        val firstDigit = phoneNumber.firstOrNull()
        when {
            phoneNumber.isEmpty() -> null
            firstDigit != null && firstDigit in '0'..'5' -> "Invalid number"
            phoneNumber.length > 10 -> "Phone number should only be 10 digits"
            else -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.all { char -> char.isDigit() }) phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = phoneError != null,
                supportingText = {
                    if (phoneError != null) {
                        Text(text = phoneError, color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        if (isSignUp) {
                            if (name.isBlank()) {
                                errorMessage = "Please enter your name"
                                return@launch
                            }
                            
                            // Final validation before submission
                            if (phoneError != null || phoneNumber.length != 10) {
                                errorMessage = phoneError ?: "Phone number must be exactly 10 digits"
                                return@launch
                            }

                            auth.createUserWithEmailAndPassword(email, password).await()
                            viewModel.updateProfile(name, phoneNumber)
                        } else {
                            auth.signInWithEmailAndPassword(email, password).await()
                        }
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage = e.message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignUp) "Sign Up" else "Login")
        }

        TextButton(onClick = { 
            isSignUp = !isSignUp 
            errorMessage = null
        }) {
            Text(if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up")
        }
    }
}
