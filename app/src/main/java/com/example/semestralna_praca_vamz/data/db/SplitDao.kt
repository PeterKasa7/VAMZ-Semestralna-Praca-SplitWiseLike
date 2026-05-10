package com.example.semestralna_praca_vamz.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {
    // Users
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    // Groups
    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Query("SELECT * FROM groups")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    // Group Members
    @Insert
    suspend fun insertGroupMember(member: GroupMemberEntity)

    @Query("""
        SELECT users.* FROM users 
        JOIN group_members ON users.id = group_members.user_id 
        WHERE group_members.group_id = :groupId
    """)
    fun getGroupMembersFlow(groupId: Long): Flow<List<UserEntity>>

    // Expenses
    @Insert
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE group_id = :groupId ORDER BY created_at DESC")
    fun getExpensesForGroupFlow(groupId: Long): Flow<List<ExpenseEntity>>

    // Expense Shares
    @Insert
    suspend fun insertExpenseShares(shares: List<ExpenseShareEntity>)

    @Query("DELETE FROM expense_shares WHERE expense_id = :expenseId")
    suspend fun deleteSharesForExpense(expenseId: Long)

    @Query("SELECT * FROM expense_shares WHERE expense_id = :expenseId")
    suspend fun getSharesForExpense(expenseId: Long): List<ExpenseShareEntity>
    
    @Transaction
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupWithData(groupId: Long): GroupWithData?
}

data class GroupWithData(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(GroupMemberEntity::class, parentColumn = "group_id", entityColumn = "user_id")
    )
    val members: List<UserEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "group_id"
    )
    val expenses: List<ExpenseEntity>
)
