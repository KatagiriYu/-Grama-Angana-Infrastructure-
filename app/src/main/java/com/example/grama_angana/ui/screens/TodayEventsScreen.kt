package com.example.grama_angana.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grama_angana.data.Booking
import com.example.grama_angana.data.BookingStatus
import com.example.grama_angana.ui.MainViewModel
import java.time.format.DateTimeFormatter

@Composable
fun TodayEventsScreen(viewModel: MainViewModel = viewModel()) {
    val bookings by viewModel.bookingsForSelectedDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val formattedDate = remember(selectedDate) { selectedDate.format(dateFormatter) }

    var selectedBookingForDetails by remember { mutableStateOf<Booking?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Booking?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Events for $formattedDate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (bookings.isEmpty()) {
            Text("No bookings for this date.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(bookings, key = { it.id }) { booking ->
                    BookingManagementItem(
                        booking = booking,
                        isUserLoggedIn = isUserLoggedIn,
                        onClick = {
                            if (isUserLoggedIn) {
                                selectedBookingForDetails = booking
                            }
                        }
                    )
                }
            }
        }
    }

    selectedBookingForDetails?.let { booking ->
        BookingDetailsDialog(
            booking = booking,
            viewModel = viewModel,
            onDismiss = { selectedBookingForDetails = null },
            onApprove = {
                viewModel.approveBooking(booking.id)
                selectedBookingForDetails = null
            },
            onDelete = {
                showDeleteConfirmation = booking
                selectedBookingForDetails = null
            }
        )
    }

    showDeleteConfirmation?.let { booking ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this booking for '${booking.purpose}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBooking(booking.id)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BookingDetailsDialog(
    booking: Booking,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onDelete: () -> Unit
) {
    val userProfile by viewModel.getUserProfile(booking.userId).collectAsState(initial = null)
    val isSuperUser by viewModel.isSuperUser.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Booking Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailItem(label = "Purpose", value = booking.purpose)
                DetailItem(label = "Time Slot", value = booking.timeSlot.name.lowercase().replaceFirstChar { it.uppercase() })
                DetailItem(label = "User Name", value = userProfile?.name ?: booking.userName)
                // Show phone number only to superusers
                if (isSuperUser) {
                    DetailItem(label = "Phone Number", value = userProfile?.phoneNumber ?: "Not provided")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (booking.status == BookingStatus.PENDING && isSuperUser) {
                    Button(onClick = onApprove) {
                        Text("Approve")
                    }
                }
                // Delete action also restricted to superusers
                if (isSuperUser) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BookingManagementItem(
    booking: Booking,
    isUserLoggedIn: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isUserLoggedIn) {
                    Text(booking.purpose, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Slot: ${booking.timeSlot.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyMedium)
                    Text("By: ${booking.userName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                } else {
                    Text(
                        text = if (booking.status == BookingStatus.PENDING) "Hall Booking Pending" else "Hall Booked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Slot: ${booking.timeSlot.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyMedium)
                }
                
                val (statusText, statusColor) = remember(booking.status) {
                    when(booking.status) {
                        BookingStatus.PENDING -> "Pending Approval" to Color(0xFFF57C00)
                        BookingStatus.CONFIRMED -> "Confirmed" to Color(0xFF388E3C)
                        else -> "Cancelled" to Color.Gray
                    }
                }
                
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
