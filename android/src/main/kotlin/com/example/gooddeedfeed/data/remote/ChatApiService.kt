package com.example.gooddeedfeed.data.remote

import io.ktor.client.HttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatApiService @Inject constructor(client: HttpClient) : BaseApiService(client) 
