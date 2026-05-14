package com.example.grama_angana.data

/**
 * Firestore‑compatible representation of a Booking.
 * All fields are primitive types (String) that Firestore can store directly.
 */
 data class BookingEntity(
    val id: String = "",
    val date: String = "",          // ISO‑8601 date string, e.g. "2024-12-25"
    val timeSlot: String = "",       // enum name, e.g. "MORNING"
    val purpose: String = "",
    val status: String = BookingStatus.PENDING.name,
    val userId: String = "",
    val userName: String = ""
) {
    fun toDomain(): Booking = Booking(
        id = id,
        date = date,
        timeSlot = try { TimeSlot.valueOf(timeSlot) } catch (_: Exception) { TimeSlot.MORNING },
        purpose = purpose,
        status = try { BookingStatus.valueOf(status) } catch (_: Exception) { BookingStatus.PENDING },
        userId = userId,
        userName = userName
    )
}

fun Booking.toEntity(): BookingEntity = BookingEntity(
    id = id,
    date = date,
    timeSlot = timeSlot.name,
    purpose = purpose,
    status = status.name,
    userId = userId,
    userName = userName
)
