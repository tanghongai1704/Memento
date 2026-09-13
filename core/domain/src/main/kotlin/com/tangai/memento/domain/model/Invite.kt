package com.tangai.memento.domain.model

/** codeHash is the document ID. Raw codes must never be persisted in public documents. */
data class Invite(
    val codeHash: String,
    val purpose: InvitePurpose,
    val createdBy: String,
    val targetConnectionId: String?,
    val maxUses: Int,
    val usedCount: Int,
    val status: InviteStatus,
    val createdAt: Long,
    val expiresAt: Long,
    val revokedAt: Long? = null,
    val schemaVersion: Int = 1
)
enum class InvitePurpose { DIRECT_PAIR, GROUP_JOIN }
enum class InviteStatus { ACTIVE, USED, EXPIRED, REVOKED }
