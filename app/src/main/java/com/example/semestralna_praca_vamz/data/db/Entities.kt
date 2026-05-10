package com.example.semestralna_praca_vamz.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class SplitType {
    EQUAL, EXACT, PERCENTAGE
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notificationsEnabled: Boolean = false
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["group_id", "user_id"],
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, parentColumns = ["id"], childColumns = ["group_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE)
    ]
)
data class GroupMemberEntity(
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "user_id") val userId: Long
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(entity = GroupEntity::class, parentColumns = ["id"], childColumns = ["group_id"]),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["paid_by_user_id"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "paid_by_user_id") val paidByUserId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val description: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    val splitType: SplitType = SplitType.EQUAL
)

@Entity(
    tableName = "expense_shares",
    primaryKeys = ["expense_id", "user_id"],
    foreignKeys = [
        ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["expense_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"])
    ]
)
data class ExpenseShareEntity(
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "owed_amount_cents") val owedAmountCents: Long
)
