package com.tangai.memento.feature.connection.data.di

import com.tangai.memento.feature.connection.data.ConnectionRepositoryImpl
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectionDataModule {
    @Binds
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): ConnectionRepository
}
