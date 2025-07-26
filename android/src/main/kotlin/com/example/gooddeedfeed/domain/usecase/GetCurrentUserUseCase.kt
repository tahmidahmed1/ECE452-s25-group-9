package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.repository.AuthRepository

class GetCurrentUserUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.getCurrentUser()
}
