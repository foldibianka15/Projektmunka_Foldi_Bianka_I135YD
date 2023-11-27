package com.example.projektmunka.di

import com.example.firstapp.data.models.remote.services.FireStoreService
import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.dataremote.AuthService
import com.example.projektmunka.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @Provides
    @Singleton
    fun provideAuthService() = AuthService()
    @Provides
    @Singleton
    fun provideFireStoreService() = FireStoreService()
    @Provides
    @Singleton
    fun provideAuthRepository(authService:AuthService, fireStoreRepository: FireStoreRepository) = AuthRepository(authService, fireStoreRepository )
    @Provides
    @Singleton
    fun provideFireStoreRepository(fireStoreService: FireStoreService) = FireStoreRepository(fireStoreService)
}
