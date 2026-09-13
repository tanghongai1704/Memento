package com.tangai.memento.di

import com.tangai.memento.feature.auth.data.AuthRepositoryImpl
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.history.data.HistoryRepositoryImpl
import com.tangai.memento.feature.history.domain.HistoryRepository
import com.tangai.memento.feature.home.data.HomeRepositoryImpl
import com.tangai.memento.feature.home.domain.HomeRepository
import com.tangai.memento.feature.post.data.PostRepositoryImpl
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
        impl: HomeRepositoryImpl
    ): HomeRepository

    @Binds
    abstract fun bindPostRepository(
        impl: PostRepositoryImpl
    ): PostRepository

    @Binds
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository
}
