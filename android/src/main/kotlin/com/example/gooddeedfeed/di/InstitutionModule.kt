package com.example.gooddeedfeed.di

import com.example.gooddeedfeed.data.repository.ReviewRepositoryImpl
import com.example.gooddeedfeed.domain.repository.ReviewRepository
import com.example.gooddeedfeed.domain.usecase.institution.ManageReviewsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InstitutionModule {

    @Binds
    @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
    
    companion object {
        @Provides
        fun provideManageReviewsUseCase(repo: ReviewRepository) = ManageReviewsUseCase(repo)
    }
} 