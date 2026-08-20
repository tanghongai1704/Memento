package com.tangai.memento.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tangai.memento.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val createdAt: Long
)

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    email = email
)

fun User.toEntity(createdAt: Long = System.currentTimeMillis()): UserEntity = UserEntity(
    id = id,
    username = username,
    email = email,
    createdAt = createdAt
)
