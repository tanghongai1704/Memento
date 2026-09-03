package com.tangai.memento.domain.model

data class ConnectionMember(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val status: MemberStatus
)

enum class MemberStatus {
    ACTIVE,
    LEFT
}
