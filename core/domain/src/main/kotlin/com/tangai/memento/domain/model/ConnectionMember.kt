package com.tangai.memento.domain.model

data class ConnectionMember(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val status: MemberStatus,
    val leftAt: Long? = null,
    val invitedBy: String? = null,
    val removedBy: String? = null
)
enum class MemberStatus { ACTIVE, LEFT, REMOVED }
