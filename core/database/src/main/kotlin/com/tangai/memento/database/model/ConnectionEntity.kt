package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.ConnectionMember

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val status: String,
    val name: String? = null,
    val description: String? = null,
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
    description = description,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Connection.toEntity(): ConnectionEntity = ConnectionEntity(
    id = id,
    type = type.name,
    status = status.name,
    name = name,
    description = description,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
