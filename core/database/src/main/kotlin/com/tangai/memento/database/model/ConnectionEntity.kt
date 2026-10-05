package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.ConnectionMember

@Entity(tableName = "connections", indices = [androidx.room.Index("status")])
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val type: ConnectionType,
    val status: ConnectionStatus,
    val name: String? = null,
    val ownerId: String? = null,
    val maxMembers: Int = 2,
    val directKey: String? = null,
    val lastPostAt: Long? = null,
    val schemaVersion: Int = 1,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun ConnectionEntity.toDomain(members: List<ConnectionMember> = emptyList()): Connection = Connection(
    id = id,
    type = type,
    members = members,
    status = status,
    name = name,
    ownerId = ownerId,
    maxMembers = maxMembers,
    directKey = directKey,
    lastPostAt = lastPostAt,
    schemaVersion = schemaVersion,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Connection.toEntity(): ConnectionEntity = ConnectionEntity(
    id = id,
    type = type,
    status = status,
    name = name,
    ownerId = ownerId,
    maxMembers = maxMembers,
    directKey = directKey,
    lastPostAt = lastPostAt,
    schemaVersion = schemaVersion,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
