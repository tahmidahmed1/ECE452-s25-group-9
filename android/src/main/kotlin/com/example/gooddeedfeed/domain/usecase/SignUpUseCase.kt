package com.example.gooddeedfeed.domain.usecase

import com.example.gooddeedfeed.domain.repository.AuthRepository

class SignUpUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(
        username: String,
        email: String,
        password: String,
    ) = repo.signUp(username, email, password)
}
