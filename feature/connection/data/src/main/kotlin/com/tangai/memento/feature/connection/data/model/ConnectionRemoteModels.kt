package com.tangai.memento.feature.connection.data.model

import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.ConnectionType
import com.tangai.memento.domain.model.MemberStatus
import com.tangai.memento.domain.model.RequestStatus

data class ConnectionRemote(
    val id: String,
    val type: ConnectionType,
    val createdBy: String,
    val status: ConnectionStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val name: String? = null,
    val description: String? = null
)

data class ConnectionMemberRemote(
    val connectionId: String,
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val status: MemberStatus
)

data class ConnectionRequestRemote(
    val id: String,
    val connectionId: String? = null,
    val senderId: String,
    val receiverId: String,
    val connectionType: ConnectionType,
    val status: RequestStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val message: String? = null
)
