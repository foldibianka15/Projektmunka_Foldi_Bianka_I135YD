package com.example.projektmunka.di

import com.example.firstapp.repository.UserDataRepository
import com.example.projektmunka.dataremote.AuthDao
import com.example.projektmunka.remote.UserDataDao
import com.example.projektmunka.remote.UserLocationDao
import com.example.projektmunka.remote.UserRouteDao
import com.example.projektmunka.remote.UserRouteTrackerDao
import com.example.projektmunka.repository.AuthRepository
import com.example.projektmunka.repository.NearbyUsersRepository
import com.example.projektmunka.repository.UserLocationRepository
import com.example.projektmunka.services.LocationService
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
    fun provideAuthDao() = AuthDao()
    @Provides
    @Singleton
    fun provideUserDataDao() = UserDataDao()
    @Provides
    @Singleton
    fun provideUserLocationDao() = UserLocationDao()
    @Provides
    @Singleton
    fun provideUserRouteDao() = UserRouteDao()
    @Provides
    @Singleton
    fun provideUserRouteTrackerDao() = UserRouteTrackerDao()
    @Provides
    @Singleton
    fun provideAuthRepository(authDao:AuthDao, userDataRepository: UserDataRepository) = AuthRepository(authDao, userDataRepository)
    @Provides
    @Singleton
    fun provideFireStoreRepository(userDataDao: UserDataDao, userLocationDao: UserLocationDao) = UserDataRepository(userDataDao, userLocationDao)
    @Provides
    @Singleton
    fun provideUserLocationRepository(userLocationDao: UserLocationDao) = UserLocationRepository(userLocationDao)
    @Provides
    @Singleton
    fun provideUserLocationService(userDataRepository: UserDataRepository ,userLocationRepository: UserLocationRepository) = LocationService(userDataRepository, userLocationRepository)
    @Provides
    @Singleton
    fun provideNearbyUsersRepository(userDataDao: UserDataDao, userLocationDao: UserLocationDao) = NearbyUsersRepository(userDataDao, userLocationDao)

}
