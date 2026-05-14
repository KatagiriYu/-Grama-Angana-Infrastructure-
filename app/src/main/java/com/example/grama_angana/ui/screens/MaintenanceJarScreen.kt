package com.example.grama_angana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grama_angana.data.MaintenanceItem
import com.example.grama_angana.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceJarScreen(viewModel: MainViewModel = viewModel()) {
    val itemsList by viewModel.maintenanceItems.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val isSuperUser by viewModel.isSuperUser.collectAsState()

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    val totalPledged by remember(itemsList) {
        derivedStateOf { itemsList.sumOf { it.pledgedAmount } }
    }

    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedItemForApproval by remember { mutableStateOf<MaintenanceItem?>(null) }
    var selectedItemForPledge by remember { mutableStateOf<MaintenanceItem?>(null) }

    var requestName by remember { mutableStateOf("") }
    var requestAmount by remember { mutableStateOf("") }
    var pledgeAmountInput by remember { mutableStateOf("") }

    // Track selected section filter; null means no filter (show all)
    var selectedSection by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp)
        ) {
            item(key = "header") {
                MaintenanceJarHeader(totalAmount = totalPledged, formatter = currencyFormatter)
            }

            // Header Row with three sections - clickable to filter
            item(key = "section_headers") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "REQUESTED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSection == "REQUESTED") MaterialTheme.colorScheme.primary else Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { selectedSection = if (selectedSection == "REQUESTED") null else "REQUESTED" }
                    )
                    Text(
                        text = "ONGOING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSection == "ONGOING") MaterialTheme.colorScheme.primary else Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { selectedSection = if (selectedSection == "ONGOING") null else "ONGOING" }
                    )
                    Text(
                        text = "COMPLETED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSection == "COMPLETED") MaterialTheme.colorScheme.primary else Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable { selectedSection = if (selectedSection == "COMPLETED") null else "COMPLETED" }
                    )
                }
            }

            // Requests Section (visible when no filter or REQUESTED selected)
            val requests = itemsList.filter { it.category == "REQUESTED" }
            if (selectedSection == null || selectedSection == "REQUESTED") {
                if (requests.isNotEmpty()) {
                    item(key = "requests_header") {
                        Text(
                            text = "REQUESTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(requests, key = { it.id }) { item ->
                        MaintenanceCard(
                            item = item,
                            formatter = currencyFormatter,
                            onClick = {
                                if (isLoggedIn && isSuperUser) {
                                    selectedItemForApproval = item
                                } else if (isLoggedIn && !isSuperUser) {
                                    viewModel.showStatusMessage("Only superuser can approve requests")
                                } else {
                                    viewModel.showStatusMessage("Please login to review requests")
                                }
                            }
                        )
                    }
                }
            }

            // Ongoing Section (visible when no filter or ONGOING selected)
            val ongoing = itemsList.filter { it.category != "REQUESTED" && (it.goalAmount <= 0 || it.pledgedAmount < it.goalAmount) }
            if (selectedSection == null || selectedSection == "ONGOING") {
                if (ongoing.isNotEmpty()) {
                    item(key = "ongoing_header") {
                        Text(
                            text = "ONGOING REQUESTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(ongoing, key = { it.id }) { item ->
                        MaintenanceCard(
                            item = item,
                            formatter = currencyFormatter,
                            onClick = {
                                if (isLoggedIn) {
                                    if (item.category == "REQUESTED") {
                                        selectedItemForApproval = item
                                    } else {
                                        selectedItemForPledge = item
                                    }
                                } else {
                                    viewModel.showStatusMessage("Please login to pledge or review requests")
                                }
                            }
                        )
                    }
                }
            }

            // Completed Section (visible when no filter or COMPLETED selected)
            val completed = itemsList.filter { it.goalAmount > 0 && it.pledgedAmount >= it.goalAmount }
            if (selectedSection == null || selectedSection == "COMPLETED") {
                if (completed.isNotEmpty()) {
                    item(key = "completed_header") {
                        Text(
                            text = "COMPLETED REQUESTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(completed, key = { it.id }) { item ->
                        MaintenanceCard(
                            item = item,
                            formatter = currencyFormatter,
                            onClick = {
                                if (isLoggedIn) {
                                    if (item.category == "REQUESTED") {
                                        selectedItemForApproval = item
                                    } else {
                                        selectedItemForPledge = item
                                    }
                                } else {
                                    viewModel.showStatusMessage("Please login to pledge or review requests")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isLoggedIn) {
            FloatingActionButton(
                onClick = { showRequestDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Request Maintenance")
            }
        }

        if (showRequestDialog) {
            AlertDialog(
                onDismissRequest = { showRequestDialog = false },
                title = { Text(text = "New Maintenance Request", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = requestName,
                            onValueChange = { requestName = it },
                            label = { Text("What is needed? (e.g. New Fan)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = requestAmount,
                            onValueChange = { requestAmount = it },
                            label = { Text("Estimated Cost (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (userProfile?.phoneNumber.isNullOrBlank()) {
                                viewModel.showStatusMessage("Please register your phone number in profile first")
                            } else {
                                val amount = requestAmount.toDoubleOrNull() ?: 0.0
                                if (requestName.isNotBlank() && amount > 0) {
                                    viewModel.addMaintenanceRequest(requestName, amount)
                                    showRequestDialog = false
                                    requestName = ""
                                    requestAmount = ""
                                }
                            }
                        }
                    ) {
                        Text("Submit Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRequestDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val isSuperUser by viewModel.isSuperUser.collectAsState()

        selectedItemForApproval?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForApproval = null },
                title = { Text(text = "Review Request", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SubtleInfoBlock(items = listOf(
                            Icons.Default.Person to "Requested By: ${item.requesterName.ifBlank { "Anonymous User" }}",
                            Icons.Default.Build to "Item: ${item.name}",
                            Icons.Default.Payments to "Cost: ${currencyFormatter.format(item.goalAmount)}"
                        )
                        )
                    }
                },
                confirmButton = {
                    if (isSuperUser) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.cancelMaintenanceItem(item.id)
                                    selectedItemForApproval = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel Request", fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    viewModel.approveMaintenanceItem(item.id)
                                    selectedItemForApproval = null
                                }
                            ) {
                                Text("Approve")
                            }
                        }
                    }
                }
            )
        }

        selectedItemForPledge?.let { item ->
            val remainingRequired = (item.goalAmount - item.pledgedAmount).coerceAtLeast(0.0)
            val enteredAmount = pledgeAmountInput.toDoubleOrNull() ?: 0.0
            val isError = enteredAmount > remainingRequired

            AlertDialog(
                onDismissRequest = {
                    selectedItemForPledge = null
                    pledgeAmountInput = ""
                },
                title = { Text(text = if (remainingRequired > 0) "Contribute to Fund" else "Target Reached", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SubtleInfoBlock(
                            items = listOf(
                                Icons.Default.Person to "Requested By: ${item.requesterName.ifBlank { "Anonymous User" }}",
                                Icons.Default.Build to "Item: ${item.name}",
                                Icons.Default.Flag to "Total Goal: ${currencyFormatter.format(item.goalAmount)}"
                            )
                        )

                        if (remainingRequired > 0) {
                            OutlinedTextField(
                                value = pledgeAmountInput,
                                onValueChange = { pledgeAmountInput = it },
                                label = { Text("Enter Amount to Pledge (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = isError,
                                supportingText = {
                                    if (isError) {
                                        Text(
                                            text = "Amount cannot exceed remaining: ${currencyFormatter.format(remainingRequired)}",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(text = "Max allowed: ${currencyFormatter.format(remainingRequired)}")
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "Target has been reached",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    if (remainingRequired > 0) {
                        Button(
                            onClick = {
                                if (enteredAmount > 0 && !isError) {
                                    viewModel.updatePledgedAmount(item.id, item.pledgedAmount, enteredAmount)
                                    selectedItemForPledge = null
                                    pledgeAmountInput = ""
                                }
                            },
                            enabled = enteredAmount > 0 && !isError
                        ) {
                            Text("Pledge Money")
                        }
                    } else {
                        Button(onClick = { selectedItemForPledge = null }) {
                            Text("Close")
                        }
                    }
                },
                dismissButton = {
                    if (remainingRequired > 0) {
                        TextButton(onClick = {
                            selectedItemForPledge = null
                            pledgeAmountInput = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SubtleInfoBlock(
    items: List<Pair<ImageVector, String>>,
    containerColor: Color = Color(0xFFF3F5F9)
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { (icon, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NonEditableField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Surface(
            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF3F5F9),
            shadowElevation = 0.dp
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
        }
    }
}

@Composable
fun MaintenanceJarHeader(totalAmount: Double, formatter: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "MAINTENANCE JAR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatter.format(totalAmount).replace("₹", "₹ "),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total collected for community upkeep",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MaintenanceCard(
    item: MaintenanceItem,
    formatter: NumberFormat,
    onClick: () -> Unit = {}
) {
    val progress = remember(item.pledgedAmount, item.goalAmount) {
        if (item.goalAmount > 0) (item.pledgedAmount / item.goalAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    }
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "By: ${item.requesterName.ifBlank { "Anonymous" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (item.category == "REQUESTED") Color.Red else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.category == "REQUESTED") Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatter.format(item.pledgedAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatter.format(item.goalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isCompleted) "TARGET REACHED" else "${String.format(Locale.US, "%.1f", progress * 100)}%",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
