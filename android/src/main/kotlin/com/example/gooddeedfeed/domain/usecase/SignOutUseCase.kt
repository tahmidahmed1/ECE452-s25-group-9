package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.repository.AuthRepository

class SignOutUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.signOut()
}
