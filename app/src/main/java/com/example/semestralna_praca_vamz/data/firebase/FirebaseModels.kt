package com.example.semestralna_praca_vamz.data.firebase

enum class SplitType {
    EQUAL, EXACT, PERCENTAGE
}

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

data class Group(
    val id: String = "",
    val name: String = "",
    val notificationsEnabled: Boolean = false,
    val members: List<String> = emptyList() // List of User IDs
)

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val paidByUserId: String = "",
    val amountCents: Long = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val splitType: SplitType = SplitType.EQUAL,
    val shares: Map<String, Long> = emptyMap() // User ID to owed amount in cents
)
