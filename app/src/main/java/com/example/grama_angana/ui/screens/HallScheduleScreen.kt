package com.example.grama_angana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grama_angana.R
import com.example.grama_angana.data.Booking
import com.example.grama_angana.data.BookingStatus
import com.example.grama_angana.data.TimeSlot
import com.example.grama_angana.ui.MainViewModel
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallScheduleScreen(viewModel: MainViewModel = viewModel()) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val bookings by viewModel.bookingsForSelectedDate.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    var viewedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedSlotForBooking by remember { mutableStateOf<TimeSlot?>(null) }
    var bookingPurpose by remember { mutableStateOf("") }

    LaunchedEffect(selectedDate) {
        val dateMonth = YearMonth.from(selectedDate)
        if (dateMonth != viewedMonth) {
            viewedMonth = dateMonth
        }
    }

    val bookingsMap by remember(bookings) {
        derivedStateOf {
            bookings.filter { it.status != BookingStatus.CANCELLED }
                .associateBy { it.timeSlot }
        }
    }

    val busySlotsCount by remember {
        derivedStateOf { bookingsMap.size }
    }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val formattedDate by remember(selectedDate) {
        derivedStateOf { selectedDate.format(dateFormatter) }
    }

    val onDateSelected: (LocalDate) -> Unit = remember(viewModel) { { viewModel.selectDate(it) } }
    val onMonthYearSelected: (YearMonth) -> Unit = remember { { viewedMonth = it } }
    val onDismissDialog = remember { { showBookingDialog = false; bookingPurpose = "" } }
    
    val onConfirmBooking = remember(viewModel, selectedSlotForBooking, bookingPurpose, userProfile, isUserLoggedIn) {
        {
            if (isUserLoggedIn && userProfile != null) {
                if (userProfile?.phoneNumber.isNullOrBlank()) {
                    viewModel.showStatusMessage("Please register your phone number in profile first")
                } else {
                    selectedSlotForBooking?.let { slot ->
                        viewModel.bookSlot(
                            slot,
                            bookingPurpose.ifBlank { "Community Event" },
                            userProfile!!.id,
                            userProfile!!.name.ifBlank { "Anonymous" }
                        )
                    }
                    showBookingDialog = false
                    bookingPurpose = ""
                }
            }
        }
    }

    if (showBookingDialog && selectedSlotForBooking != null) {
        BookingDialog(
            date = selectedDate,
            slot = selectedSlotForBooking!!,
            purpose = bookingPurpose,
            onPurposeChange = { bookingPurpose = it },
            onDismiss = onDismissDialog,
            onConfirm = onConfirmBooking
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.hall_schedule),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 0.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        MonthYearPicker(viewedMonth = viewedMonth, onMonthYearSelected = onMonthYearSelected)

        Spacer(modifier = Modifier.height(8.dp))

        CalendarGrid(yearMonth = viewedMonth, selectedDate = selectedDate, onDateSelected = onDateSelected)

        Spacer(modifier = Modifier.height(16.dp))
        
        DateHeader(formattedDate = formattedDate, busyCount = busySlotsCount)
        
        Spacer(modifier = Modifier.height(12.dp))

        val timeSlots = remember { TimeSlot.values() }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = timeSlots,
                key = { it.name },
                contentType = { "slot" }
            ) { slot ->
                val booking = bookingsMap[slot]
                
                val onClick = remember(slot, booking == null, isUserLoggedIn) {
                    {
                        if (booking == null) {
                            if (isUserLoggedIn) {
                                selectedSlotForBooking = slot
                                showBookingDialog = true
                            } else {
                                viewModel.showStatusMessage("Please login to book a hall")
                            }
                        }
                    }
                }

                SlotCard(slot = slot, booking = booking, isUserLoggedIn = isUserLoggedIn, onClick = onClick)
            }
        }
    }
}

@Composable
private fun DateHeader(formattedDate: String, busyCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (busyCount == 1) "1 slot busy" else "$busyCount slots busy",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MonthYearPicker(viewedMonth: YearMonth, onMonthYearSelected: (YearMonth) -> Unit) {
    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { monthExpanded = true }) {
                val monthText = remember(viewedMonth) { viewedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
                Text(text = monthText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                val months = remember { Month.values() }
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                        onClick = { onMonthYearSelected(viewedMonth.withMonth(month.value)); monthExpanded = false }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box {
            TextButton(onClick = { yearExpanded = true }) {
                Text(text = viewedMonth.year.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                val currentYear = remember { LocalDate.now().year }
                val years = remember(currentYear) { (currentYear - 1..currentYear + 2).toList() }
                years.forEach { year ->
                    DropdownMenuItem(text = { Text(year.toString()) }, onClick = { onMonthYearSelected(viewedMonth.withYear(year)); yearExpanded = false })
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(yearMonth: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val firstDayOfWeek = remember(yearMonth) { yearMonth.atDay(1).dayOfWeek.value % 7 }
    
    Column {
        val weekDays = remember { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekDays.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val rows = remember(firstDayOfWeek, daysInMonth) { (firstDayOfWeek + daysInMonth + 6) / 7 }
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0..6) {
                    val index = row * 7 + col - firstDayOfWeek + 1
                    if (index in 1..daysInMonth) {
                        val date = remember(yearMonth, index) { yearMonth.atDay(index) }
                        CalendarDay(date = date, isSelected = date == selectedDate, onDateSelected = onDateSelected)
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun CalendarDay(date: LocalDate, isSelected: Boolean, onDateSelected: (LocalDate) -> Unit) {
    val onClick = remember(date, onDateSelected) { { onDateSelected(date) } }
    Box(
        modifier = Modifier.size(36.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SlotCard(slot: TimeSlot, booking: Booking?, isUserLoggedIn: Boolean, onClick: () -> Unit) {
    val slotName = remember(slot) { slot.name.lowercase().replaceFirstChar { it.uppercase() } }
    val containerColor = remember(booking != null) { if (booking != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = slotName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (booking != null) {
                    if (isUserLoggedIn) {
                        Text("Booked: ${booking.purpose}", style = MaterialTheme.typography.bodyMedium)
                        Text("By: ${booking.userName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        Text("Occupied", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                } else {
                    Text("Available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (booking != null) StatusBadge(status = booking.status) else Icon(Icons.Default.Add, "Book", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatusBadge(status: BookingStatus) {
    val (text, color) = remember(status) {
        when (status) {
            BookingStatus.PENDING -> "Pending" to Color(0xFFF57C00)
            BookingStatus.CONFIRMED -> "Booked" to Color(0xFF388E3C)
            BookingStatus.CANCELLED -> "Cancelled" to Color.Red
        }
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(text = text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BookingDialog(date: LocalDate, slot: TimeSlot, purpose: String, onPurposeChange: (String) -> Unit, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val formattedDate = remember(date) { date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Book Now", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F5F9), // Light grayish-blue background
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(formattedDate, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(slot.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Color.DarkGray))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = purpose, onValueChange = onPurposeChange, label = { Text("Purpose e.g. Wedding") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onConfirm, shape = RoundedCornerShape(50)) { Text("Book Now") }
                }
            }
        }
    }
}
