package com.example.grama_angana.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.grama_angana.data.MaintenanceItem
import com.example.grama_angana.ui.MainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyMaintenanceRequestsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val myRequests by viewModel.myMaintenanceRequests.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Maintenance Requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (myRequests.isNotEmpty()) {
                        val totalPledged = myRequests.sumOf { it.pledgedAmount }
                        Text(
                            text = "Total: ₹${totalPledged.toInt()}",
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (myRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No requests yet", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                items(myRequests) { request ->
                    MaintenanceRequestTrackCard(request)
                }
            }
        }
    }
}

@Composable
fun MaintenanceRequestTrackCard(request: MaintenanceItem) {
    val progress = remember(request.pledgedAmount, request.goalAmount) {
        if (request.goalAmount > 0) (request.pledgedAmount / request.goalAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    }
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Pledged: ₹${request.pledgedAmount.toInt()} / Goal: ₹${request.goalAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val (statusText, statusColor) = if (request.category == "REQUESTED") {
                    "Pending" to Color(0xFFF57C00)
                } else {
                    "Approved" to Color(0xFF388E3C)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (request.category != "REQUESTED") {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isCompleted) "Target Reached" else "${String.format(Locale.US, "%.1f", progress * 100)}%",
                        modifier = Modifier.align(Alignment.CenterEnd),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
