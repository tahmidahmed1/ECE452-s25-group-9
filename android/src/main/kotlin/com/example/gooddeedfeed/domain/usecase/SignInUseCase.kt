package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.repository.AuthRepository

class SignInUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        password: String,
    ) = repo.signIn(username, password)
}
