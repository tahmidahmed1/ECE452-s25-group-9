package com.example.gooddeedfeed.presentation.viewmodel

import app.cash.turbine.test
import com.example.gooddeedfeed.data.remote.AuthResponse
import com.example.gooddeedfeed.data.remote.User
import com.example.gooddeedfeed.data.remote.UserType
import com.example.gooddeedfeed.domain.repository.AuthRepository
import com.example.gooddeedfeed.domain.usecase.GetCurrentUserUseCase
import com.example.gooddeedfeed.domain.usecase.SignInUseCase
import com.example.gooddeedfeed.domain.usecase.SignOutUseCase
import com.example.gooddeedfeed.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeAuthRepository : AuthRepository {
    var shouldFail = false
    var user: User? = null
    private var token: String? = null

    override suspend fun signUp(
        username: String,
        password: String,
    ): AuthResponse = if (shouldFail) AuthResponse(false, message = "fail") else AuthResponse(true, token = "token")

    override suspend fun signIn(
        username: String,
        password: String,
    ): AuthResponse = if (shouldFail) AuthResponse(false, message = "fail") else AuthResponse(true, token = "token")

    override suspend fun signOut() {
        token = null
    }

    override fun getToken() = flowOf(token)

    override suspend fun getCurrentUser(): User? = user
}

class AuthViewModelTest {
    private lateinit var repo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun setup() {
        repo = FakeAuthRepository()
        viewModel =
            AuthViewModel(
                signUp = SignUpUseCase(repo),
                signIn = SignInUseCase(repo),
                signOut = SignOutUseCase(repo),
                getCurrentUser = GetCurrentUserUseCase(repo),
            )
    }

    @Test
    fun `signIn success emits Success`() =
        scope.runTest {
                    repo.user = User(1, "test", "test@example.com", true, UserType.VOLUNTEER, true, "Test User")
        viewModel.signIn("test", "pass")
        viewModel.uiState.test {
            assertEquals(AuthUiState.Loading, awaitItem())
            assertEquals(AuthUiState.Success(User(1, "test", "test@example.com", true, UserType.VOLUNTEER, true, "Test User")), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `signIn fail emits Error`() =
        scope.runTest {
            repo.shouldFail = true
            viewModel.signIn("test", "pass")
            viewModel.uiState.test {
                assertEquals(AuthUiState.Loading, awaitItem())
                assertEquals(AuthUiState.Error("fail"), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `signOut emits SignedOut`() =
        scope.runTest {
            viewModel.signOut()
            viewModel.uiState.test {
                assertEquals(AuthUiState.SignedOut, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
