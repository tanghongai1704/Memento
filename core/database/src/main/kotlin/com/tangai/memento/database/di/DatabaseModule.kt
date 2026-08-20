package com.tangai.memento.database.di

import android.content.Context
import androidx.room.Room
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.dao.UserDao
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
        ).build()
    }

    @Provides
    fun provideUserDao(database: MementoDatabase): UserDao = database.userDao()
}
