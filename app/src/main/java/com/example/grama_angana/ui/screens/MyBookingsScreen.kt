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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.grama_angana.data.Booking
import com.example.grama_angana.data.BookingStatus
import com.example.grama_angana.ui.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val myBookings by viewModel.myBookings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Hall Bookings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (myBookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No bookings yet", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                items(myBookings) { booking ->
                    BookingTrackCard(booking)
                }
            }
        }
    }
}

@Composable
fun BookingTrackCard(booking: Booking) {
    val formattedDate = remember(booking.date) {
        try {
            LocalDate.parse(booking.date)
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            booking.date
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formattedDate, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    booking.timeSlot.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    booking.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ProfileTrackStatusBadge(status = booking.status)
        }
    }
}

@Composable
fun ProfileTrackStatusBadge(status: BookingStatus) {
    val (text, color) = when (status) {
        BookingStatus.PENDING -> "Pending" to Color(0xFFF57C00)
        BookingStatus.CONFIRMED -> "Booked" to Color(0xFF388E3C)
        BookingStatus.CANCELLED -> "Cancelled" to Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
