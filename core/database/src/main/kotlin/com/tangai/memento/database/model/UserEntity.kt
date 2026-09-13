package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tangai.memento.domain.model.User

@Entity(tableName = "users", indices = [androidx.room.Index("usernameNormalized")])
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val usernameNormalized: String,
    val avatarPath: String?,
    val bio: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int
)
fun UserEntity.toDomain() = User(id = id, username = username, displayName = displayName,
    usernameNormalized = usernameNormalized, avatarPath = avatarPath, bio = bio,
    createdAt = createdAt, updatedAt = updatedAt, schemaVersion = schemaVersion)
fun User.toEntity() = UserEntity(id, username, displayName, usernameNormalized, avatarPath,
    bio, createdAt, updatedAt, schemaVersion)
