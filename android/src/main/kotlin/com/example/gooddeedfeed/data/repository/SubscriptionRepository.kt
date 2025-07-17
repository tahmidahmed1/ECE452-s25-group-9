package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainSubscriptionResponse
import com.example.gooddeedfeed.domain.model.DomainSubscriptionStatus
import com.example.gooddeedfeed.domain.model.DomainOrganizerWithSubscriptionStatus

interface SubscriptionRepository {
    suspend fun subscribeToOrganizer(organizerId: Int): Result<DomainSubscriptionResponse>
    suspend fun unsubscribeFromOrganizer(organizerId: Int): Result<DomainSubscriptionResponse>
    suspend fun getUserSubscriptions(): Result<List<DomainUser>>
    suspend fun getSubscriptionStatus(organizerId: Int): Result<DomainSubscriptionStatus>
    suspend fun getOrganizersWithSubscriptionStatus(query: String? = null): Result<List<DomainOrganizerWithSubscriptionStatus>>
} 