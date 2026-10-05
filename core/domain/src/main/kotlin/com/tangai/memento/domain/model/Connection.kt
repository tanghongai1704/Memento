package com.tangai.memento.domain.model

data class Connection(
    val id: String,
    val type: ConnectionType,
    val members: List<ConnectionMember>,
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
