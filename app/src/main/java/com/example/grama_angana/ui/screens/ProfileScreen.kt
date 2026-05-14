package com.example.grama_angana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.grama_angana.ui.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToBookings: () -> Unit,
    onNavigateToRequests: () -> Unit
) {
    val userProfile by viewModel.currentUserProfile.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            nameInput = it.name
            phoneInput = it.phoneNumber
        }
    }

    val phoneError = remember(phoneInput) {
        val firstDigit = phoneInput.firstOrNull()
        when {
            phoneInput.isEmpty() -> null
            firstDigit != null && firstDigit in '0'..'5' -> "Invalid number"
            phoneInput.length > 10 -> "Phone number should only be 10 digits"
            else -> null
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) phoneInput = it },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (phoneError == null && phoneInput.length == 10) {
                            viewModel.updateProfile(nameInput, phoneInput)
                            showEditDialog = false
                        } else if (phoneInput.length != 10) {
                            viewModel.showStatusMessage("Phone number should be 10 digits")
                        } else {
                            viewModel.showStatusMessage(phoneError ?: "Invalid phone number")
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Section: User Information ---
        item {
            ProfileSectionHeader(title = "Account", icon = Icons.Default.AccountCircle)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userProfile?.name.isNullOrBlank()) "Add Username" else userProfile!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.email ?: "No Email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = if (userProfile?.phoneNumber.isNullOrBlank()) "Add Phone Number" else userProfile!!.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // --- Section: Track Application ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileSectionHeader(title = "Track Application", icon = Icons.Default.History)
            
            NavigationCard(
                title = "Hall Bookings",
                icon = Icons.Default.Home,
                onClick = onNavigateToBookings
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            NavigationCard(
                title = "Maintenance Requests",
                icon = Icons.Default.Build,
                onClick = onNavigateToRequests
            )
        }

        // --- Section: Logout ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun NavigationCard(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
