package com.tangai.memento.domain.model

data class Connection(
    val id: String,
    val type: ConnectionType,
    val members: List<ConnectionMember>,
    val status: ConnectionStatus,
    val name: String? = null,
    val description: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)
