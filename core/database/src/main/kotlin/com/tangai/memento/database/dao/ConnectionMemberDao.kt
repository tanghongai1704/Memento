package com.tangai.memento.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tangai.memento.database.model.ConnectionMemberEntity

@Dao
interface ConnectionMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: ConnectionMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<ConnectionMemberEntity>)

    @Update
    suspend fun updateMember(member: ConnectionMemberEntity)

    @Delete
    suspend fun deleteMember(member: ConnectionMemberEntity)

    @Query("SELECT * FROM connection_members WHERE connectionId = :connectionId")
    suspend fun getMembersByConnectionId(connectionId: String): List<ConnectionMemberEntity>

    @Query("SELECT * FROM connection_members WHERE connectionId = :connectionId AND userId = :userId LIMIT 1")
    suspend fun getMember(connectionId: String, userId: String): ConnectionMemberEntity?

    @Query("DELETE FROM connection_members WHERE connectionId = :connectionId")
    suspend fun deleteAllMembersByConnectionId(connectionId: String)

    @Query("DELETE FROM connection_members WHERE connectionId = :connectionId AND userId = :userId")
    suspend fun deleteMemberFromConnection(connectionId: String, userId: String)

    @Query("SELECT * FROM connection_members WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActiveMembershipsForUser(userId: String): List<ConnectionMemberEntity>

    @Query("""UPDATE connection_members SET status = 'LEFT', leftAt = :leftAt, removedBy = NULL
        WHERE connectionId = :connectionId AND status = 'ACTIVE'""")
    suspend fun markMembersLeft(connectionId: String, leftAt: Long)
}
