package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tangai.memento.domain.model.ConnectionMember
import com.tangai.memento.domain.model.MemberRole
import com.tangai.memento.domain.model.MemberStatus

@Entity(
    tableName = "connection_members",
    primaryKeys = ["connectionId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = ConnectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["connectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("connectionId"),
        Index("userId")
    ]
)
data class ConnectionMemberEntity(
    val connectionId: String,
    val userId: String,
    val role: MemberRole,
    val joinedAt: Long,
    val status: MemberStatus,
    val leftAt: Long? = null,
    val invitedBy: String? = null,
    val removedBy: String? = null
)

fun ConnectionMemberEntity.toDomain(): ConnectionMember = ConnectionMember(
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    leftAt = leftAt, invitedBy = invitedBy, removedBy = removedBy,
    status = status
)

fun ConnectionMember.toEntity(connectionId: String): ConnectionMemberEntity = ConnectionMemberEntity(
    connectionId = connectionId,
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    leftAt = leftAt, invitedBy = invitedBy, removedBy = removedBy,
    status = status
)
