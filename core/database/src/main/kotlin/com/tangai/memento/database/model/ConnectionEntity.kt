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
    val type: String,
    val status: String,
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
    type = ConnectionType.valueOf(type),
    members = members,
    status = ConnectionStatus.valueOf(status),
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
    type = type.name,
    status = status.name,
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
