package com.example.grama_angana.data

data class MaintenanceItem(
    val id: String = "",
    val name: String = "",
    val goalAmount: Double = 0.0,
    val pledgedAmount: Double = 0.0,
    val category: String = "",
    val requesterId: String = "",
    val requesterName: String = ""
)
