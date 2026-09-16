package com.tangai.memento.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.dao.ConnectionDao
import com.tangai.memento.database.dao.ConnectionMemberDao
import com.tangai.memento.database.model.UserEntity
import com.tangai.memento.database.model.ConnectionEntity
import com.tangai.memento.database.model.ConnectionMemberEntity

@Database(
    entities = [
        UserEntity::class,
        ConnectionEntity::class,
        com.tangai.memento.database.model.PostEntity::class,
        com.tangai.memento.database.model.MediaItemEntity::class,
        ConnectionMemberEntity::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun postDao(): com.tangai.memento.database.dao.PostDao
    abstract fun userDao(): UserDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun connectionMemberDao(): ConnectionMemberDao
}
