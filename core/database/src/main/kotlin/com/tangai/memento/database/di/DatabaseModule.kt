package com.tangai.memento.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.dao.UserDao
import com.tangai.memento.database.dao.ConnectionDao
import com.tangai.memento.database.dao.ConnectionMemberDao
import com.tangai.memento.database.dao.ConnectionRequestDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMementoDatabase(
        @ApplicationContext context: Context
    ): MementoDatabase {
        return Room.databaseBuilder(
            context,
            MementoDatabase::class.java,
            "memento.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: MementoDatabase): UserDao = database.userDao()

    @Provides
    fun provideConnectionDao(database: MementoDatabase): ConnectionDao = database.connectionDao()

    @Provides
    fun provideConnectionMemberDao(database: MementoDatabase): ConnectionMemberDao = database.connectionMemberDao()

    @Provides
    fun provideConnectionRequestDao(database: MementoDatabase): ConnectionRequestDao = database.connectionRequestDao()
}
