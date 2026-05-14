package com.example.grama_angana.data

data class Booking(
    val id: String = "",
    val date: String = "",           // ISO string e.g. "2024-12-25"
    val timeSlot: TimeSlot = TimeSlot.MORNING,
    val purpose: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val userId: String = "",
    val userName: String = ""
)
