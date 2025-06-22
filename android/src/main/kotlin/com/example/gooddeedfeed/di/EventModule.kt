package com.example.gooddeedfeed.di

import com.example.gooddeedfeed.data.remote.EventApiService
import com.example.gooddeedfeed.data.repository.EventRepositoryImpl
import com.example.gooddeedfeed.data.repository.MapRepositoryImpl
import com.example.gooddeedfeed.data.repository.OpportunitiesRepositoryImpl
import com.example.gooddeedfeed.domain.repository.EventRepository
import com.example.gooddeedfeed.domain.repository.MapRepository
import com.example.gooddeedfeed.domain.repository.OpportunitiesRepository
import com.example.gooddeedfeed.domain.usecase.GetMapEventsUseCase
import com.example.gooddeedfeed.domain.usecase.organizer.ManageEventsUseCase
import com.example.gooddeedfeed.domain.usecase.volunteer.ApplyForOpportunityUseCase
import com.example.gooddeedfeed.domain.usecase.volunteer.GetOpportunitiesUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindOpportunitiesRepository(impl: OpportunitiesRepositoryImpl): OpportunitiesRepository

    @Binds
    @Singleton
    abstract fun bindMapRepository(impl: MapRepositoryImpl): MapRepository

    companion object {
        @Provides
        @Singleton
        fun provideEventApiService(client: HttpClient): EventApiService = EventApiService(client)

        @Provides
        fun provideGetMapEventsUseCase(repo: MapRepository) = GetMapEventsUseCase(repo)

        @Provides
        fun provideGetOpportunitiesUseCase(repo: OpportunitiesRepository) = GetOpportunitiesUseCase(repo)

        @Provides
        fun provideApplyForOpportunityUseCase(repo: OpportunitiesRepository) = ApplyForOpportunityUseCase(repo)

        @Provides
        fun provideManageEventsUseCase(repo: EventRepository) = ManageEventsUseCase(repo)
    }
} 
