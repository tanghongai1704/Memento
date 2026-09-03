package com.tangai.memento.domain.model

data class ConnectionRequest(
    val id: String,
    val connectionId: String?,
    val senderId: String,
    val receiverId: String,
    val connectionType: ConnectionType,
    val message: String? = null,
    val status: RequestStatus,
    val createdAt: Long,
    val respondedAt: Long? = null
)
