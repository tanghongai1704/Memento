package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tangai.memento.domain.model.ConnectionRequest
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.RequestStatus

@Entity(
    tableName = "connection_requests",
    indices = [
        Index("receiverId"),
        Index("senderId"),
        Index("status")
    ]
)
data class ConnectionRequestEntity(
    @PrimaryKey val id: String,
    val connectionId: String? = null,
    val senderId: String,
    val receiverId: String,
    val connectionType: String,
    val message: String? = null,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long? = null
)

fun ConnectionRequestEntity.toDomain(): ConnectionRequest = ConnectionRequest(
    id = id,
    connectionId = connectionId,
    senderId = senderId,
    receiverId = receiverId,
    connectionType = ConnectionType.valueOf(connectionType),
    message = message,
    status = RequestStatus.valueOf(status),
    createdAt = createdAt,
    respondedAt = respondedAt
)

fun ConnectionRequest.toEntity(): ConnectionRequestEntity = ConnectionRequestEntity(
    id = id,
    connectionId = connectionId,
    senderId = senderId,
    receiverId = receiverId,
    connectionType = connectionType.name,
    message = message,
    status = status.name,
    createdAt = createdAt,
    respondedAt = respondedAt
)
