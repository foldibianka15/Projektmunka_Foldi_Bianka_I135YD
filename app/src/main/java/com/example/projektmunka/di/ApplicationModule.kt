package com.example.projektmunka.di

import com.example.firstapp.repository.FireStoreRepository
import com.example.projektmunka.dataremote.AuthService
import com.example.projektmunka.remote.UserDataService
import com.example.projektmunka.remote.UserLocationService
import com.example.projektmunka.remote.UserRouteService
import com.example.projektmunka.remote.UserRouteTrackerService
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
    fun provideUserDataService() = UserDataService()
    @Provides
    @Singleton
    fun provideUserLocationService() = UserLocationService()
    @Provides
    @Singleton
    fun provideUserRouteService() = UserRouteService()
    @Provides
    @Singleton
    fun provideUserRouteTrackerService() = UserRouteTrackerService()
    @Provides
    @Singleton
    fun provideAuthRepository(authService:AuthService, fireStoreRepository: FireStoreRepository) = AuthRepository(authService, fireStoreRepository )
    @Provides
    @Singleton
    fun provideFireStoreRepository(userDataService: UserDataService, userLocationService: UserLocationService) = FireStoreRepository(userDataService, userLocationService)
}
