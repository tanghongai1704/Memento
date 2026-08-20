package com.tangai.memento.di

import com.tangai.memento.feature.auth.data.AuthRepositoryImpl
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.connection.data.FakeConnectionRepository
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import com.tangai.memento.feature.history.data.FakeHistoryRepository
import com.tangai.memento.feature.history.domain.HistoryRepository
import com.tangai.memento.feature.home.data.FakeHomeRepository
import com.tangai.memento.feature.home.domain.HomeRepository
import com.tangai.memento.feature.post.data.FakePostRepository
import com.tangai.memento.feature.post.domain.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindHomeRepository(
        impl: FakeHomeRepository
    ): HomeRepository

    @Binds
    abstract fun bindConnectionRepository(
        impl: FakeConnectionRepository
    ): ConnectionRepository

    @Binds
    abstract fun bindPostRepository(
        impl: FakePostRepository
    ): PostRepository

    @Binds
    abstract fun bindHistoryRepository(
        impl: FakeHistoryRepository
    ): HistoryRepository
}
