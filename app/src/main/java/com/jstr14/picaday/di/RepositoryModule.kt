package com.jstr14.picaday.di

import com.jstr14.picaday.data.repository.FirebaseAlbumRepositoryImpl
import com.jstr14.picaday.data.repository.FirebaseImageRepositoryImpl
import com.jstr14.picaday.data.repository.ImageRepository
import com.jstr14.picaday.domain.repository.AlbumRepository
import com.jstr14.picaday.domain.repository.SessionClearable
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
    abstract fun bindFirebaseImageRepository(
        firebaseImageRepositoryImpl: FirebaseImageRepositoryImpl
    ): ImageRepository

    @Binds
    @Singleton
    abstract fun bindSessionClearable(
        firebaseImageRepositoryImpl: FirebaseImageRepositoryImpl
    ): SessionClearable

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(
        firebaseAlbumRepositoryImpl: FirebaseAlbumRepositoryImpl
    ): AlbumRepository
}