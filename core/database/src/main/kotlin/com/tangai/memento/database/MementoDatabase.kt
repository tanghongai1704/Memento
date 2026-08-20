package com.tangai.memento.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.model.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
