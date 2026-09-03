package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tangai.memento.domain.model.ConnectionMember
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
    val role: String,
    val joinedAt: Long,
    val status: String
)

fun ConnectionMemberEntity.toDomain(): ConnectionMember = ConnectionMember(
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    status = MemberStatus.valueOf(status)
)

fun ConnectionMember.toEntity(connectionId: String): ConnectionMemberEntity = ConnectionMemberEntity(
    connectionId = connectionId,
    userId = userId,
    role = role,
    joinedAt = joinedAt,
    status = status.name
)
