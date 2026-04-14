package com.jstr14.picaday.di

import com.jstr14.picaday.data.repository.FakeImageRepository
import com.jstr14.picaday.domain.repository.ImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        fakeImageRepository: FakeImageRepository
    ): ImageRepository
}