package com.tangai.memento.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tangai.memento.database.model.ConnectionRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRequest(request: ConnectionRequestEntity)

    @Update
    suspend fun updateRequest(request: ConnectionRequestEntity)

    @Delete
    suspend fun deleteRequest(request: ConnectionRequestEntity)

    @Query("SELECT * FROM connection_requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): ConnectionRequestEntity?

    @Query("SELECT * FROM connection_requests WHERE receiverId = :receiverId AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getPendingRequestsForUser(receiverId: String): List<ConnectionRequestEntity>

    @Query("SELECT * FROM connection_requests WHERE receiverId = :receiverId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingRequestsForUserFlow(receiverId: String): Flow<List<ConnectionRequestEntity>>

    @Query("SELECT * FROM connection_requests WHERE senderId = :senderId AND status = 'PENDING' ORDER BY createdAt DESC")
    suspend fun getSentPendingRequestsByUser(senderId: String): List<ConnectionRequestEntity>

    @Query("SELECT * FROM connection_requests WHERE senderId = :senderId AND receiverId = :receiverId AND status = 'PENDING' LIMIT 1")
    suspend fun checkPendingRequest(senderId: String, receiverId: String): ConnectionRequestEntity?

    @Query("SELECT * FROM connection_requests WHERE senderId = :senderId AND receiverId = :receiverId AND connectionType = :connectionType LIMIT 1")
    suspend fun findRequestBetweenUsers(senderId: String, receiverId: String, connectionType: String): ConnectionRequestEntity?

    @Query("DELETE FROM connection_requests WHERE id = :requestId")
    suspend fun deleteRequestById(requestId: String)

    @Query("DELETE FROM connection_requests WHERE senderId = :senderId AND receiverId = :receiverId")
    suspend fun deleteRequestBetweenUsers(senderId: String, receiverId: String)

    @Query("SELECT * FROM connection_requests WHERE receiverId = :receiverId OR senderId = :receiverId ORDER BY createdAt DESC")
    suspend fun getAllRequestsForUser(receiverId: String): List<ConnectionRequestEntity>
}
