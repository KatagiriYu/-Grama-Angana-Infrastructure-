package com.example.grama_angana.data

/**
 * Firestore‑compatible representation of a MaintenanceItem.
 * All fields are primitive types so Firestore can store them directly.
 */
data class MaintenanceItemEntity(
    val id: String = "",
    val name: String = "",
    val goalAmount: Double = 0.0,
    val pledgedAmount: Double = 0.0,
    val category: String = "",
    val requesterId: String = "",
    val requesterName: String = ""
) {
    fun toDomain(): MaintenanceItem = MaintenanceItem(
        id = id,
        name = name,
        goalAmount = goalAmount,
        pledgedAmount = pledgedAmount,
        category = category,
        requesterId = requesterId,
        requesterName = requesterName
    )
}

fun MaintenanceItem.toEntity(): MaintenanceItemEntity = MaintenanceItemEntity(
    id = id,
    name = name,
    goalAmount = goalAmount,
    pledgedAmount = pledgedAmount,
    category = category,
    requesterId = requesterId,
    requesterName = requesterName
)
