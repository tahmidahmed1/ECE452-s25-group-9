package com.example.gooddeedfeed.data.repository

import android.util.Log
import com.example.gooddeedfeed.data.mapper.toDomain
import com.example.gooddeedfeed.data.mapper.toDomainSubscriptionResponse
import com.example.gooddeedfeed.data.mapper.toDomainSubscriptionStatus
import com.example.gooddeedfeed.data.mapper.toDomainOrganizerWithSubscriptionStatus
import com.example.gooddeedfeed.data.remote.SubscriptionApiService
import com.example.gooddeedfeed.domain.model.DomainUser
import com.example.gooddeedfeed.domain.model.DomainSubscriptionResponse
import com.example.gooddeedfeed.domain.model.DomainSubscriptionStatus
import com.example.gooddeedfeed.domain.model.DomainOrganizerWithSubscriptionStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionApiService: SubscriptionApiService
) : SubscriptionRepository {
    
    companion object {
        private const val TAG = "SubscriptionRepositoryImpl"
    }
    
    override suspend fun subscribeToOrganizer(organizerId: Int): Result<DomainSubscriptionResponse> {
        return try {
            Log.d(TAG, "🚀 Subscribing to organizer: $organizerId")
            val response = subscriptionApiService.subscribeToOrganizer(organizerId)
            Log.d(TAG, "✅ Successfully subscribed to organizer: $organizerId")
            Result.success(response.toDomainSubscriptionResponse())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to subscribe to organizer: $organizerId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun unsubscribeFromOrganizer(organizerId: Int): Result<DomainSubscriptionResponse> {
        return try {
            Log.d(TAG, "🚀 Unsubscribing from organizer: $organizerId")
            val response = subscriptionApiService.unsubscribeFromOrganizer(organizerId)
            Log.d(TAG, "✅ Successfully unsubscribed from organizer: $organizerId")
            Result.success(response.toDomainSubscriptionResponse())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to unsubscribe from organizer: $organizerId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserSubscriptions(): Result<List<DomainUser>> {
        return try {
            Log.d(TAG, "🚀 Getting user subscriptions")
            val response = subscriptionApiService.getUserSubscriptions()
            Log.d(TAG, "✅ Successfully retrieved user subscriptions: ${response.subscriptions.size} subscriptions")
            Result.success(response.subscriptions.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get user subscriptions", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSubscriptionStatus(organizerId: Int): Result<DomainSubscriptionStatus> {
        return try {
            Log.d(TAG, "🚀 Getting subscription status for organizer: $organizerId")
            val response = subscriptionApiService.getSubscriptionStatus(organizerId)
            Log.d(TAG, "✅ Successfully retrieved subscription status for organizer: $organizerId")
            Result.success(response.toDomainSubscriptionStatus())
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get subscription status for organizer: $organizerId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getOrganizersWithSubscriptionStatus(query: String?): Result<List<DomainOrganizerWithSubscriptionStatus>> {
        return try {
            Log.d(TAG, "🚀 Getting organizers with subscription status, query: $query")
            val response = subscriptionApiService.getOrganizersWithSubscriptionStatus(query)
            Log.d(TAG, "✅ Successfully retrieved organizers with subscription status: ${response.size} organizers")
            Result.success(response.map { it.toDomainOrganizerWithSubscriptionStatus() })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get organizers with subscription status", e)
            Result.failure(e)
        }
    }
} 