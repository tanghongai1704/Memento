package com.tangai.memento.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.dao.ConnectionDao
import com.tangai.memento.database.dao.ConnectionMemberDao
import com.tangai.memento.database.dao.ConnectionRequestDao
import com.tangai.memento.database.model.UserEntity
import com.tangai.memento.database.model.ConnectionEntity
import com.tangai.memento.database.model.ConnectionMemberEntity
import com.tangai.memento.database.model.ConnectionRequestEntity

@Database(
    entities = [
        UserEntity::class,
        ConnectionEntity::class,
        ConnectionMemberEntity::class,
        ConnectionRequestEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun connectionMemberDao(): ConnectionMemberDao
    abstract fun connectionRequestDao(): ConnectionRequestDao
}
