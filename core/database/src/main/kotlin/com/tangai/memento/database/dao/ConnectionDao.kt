package com.tangai.memento.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tangai.memento.database.model.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnection(connection: ConnectionEntity)

    @Update
    suspend fun updateConnection(connection: ConnectionEntity)

    @Delete
    suspend fun deleteConnection(connection: ConnectionEntity)

    @Query("SELECT * FROM connections WHERE id = :connectionId LIMIT 1")
    suspend fun getConnectionById(connectionId: String): ConnectionEntity?

    @Query("SELECT * FROM connections WHERE id = :connectionId")
    fun getConnectionByIdFlow(connectionId: String): Flow<ConnectionEntity?>

    @Query("SELECT * FROM connections ORDER BY updatedAt DESC")
    suspend fun getAllConnections(): List<ConnectionEntity>

    @Query("SELECT * FROM connections ORDER BY updatedAt DESC")
    fun getAllConnectionsFlow(): Flow<List<ConnectionEntity>>

    @Query("DELETE FROM connections WHERE id = :connectionId")
    suspend fun deleteConnectionById(connectionId: String)
}
