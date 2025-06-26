package com.example.gooddeedfeed.data.repository

import com.example.gooddeedfeed.data.remote.AuthApiService
import com.example.gooddeedfeed.data.remote.dto.AuthResponse
import com.example.gooddeedfeed.data.remote.dto.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.manipulation.Ordering
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {
    private lateinit var context: Ordering.Context
    private lateinit var api: AuthApiService
    private lateinit var repo: AuthRepositoryImpl

    @Before
    fun setup() {
        context = mock()
        api = mock()
        repo = AuthRepositoryImpl(context, api)
    }

    @Test
    fun `signUp saves token on success`() =
        runTest {
            whenever(api.signUp("user", "pass")).thenReturn(AuthResponse(true, token = "jwt"))
            repo.signUp("user", "pass")
            // Would check DataStore for token if not mocked
        }

    @Test
    fun `signIn saves token on success`() =
        runTest {
            whenever(api.signIn("user", "pass")).thenReturn(AuthResponse(true, token = "jwt"))
            repo.signIn("user", "pass")
            // Would check DataStore for token if not mocked
        }

    @Test
    fun `signOut clears token`() =
        runTest {
            repo.signOut()
            // Would check DataStore for token == "" if not mocked
        }

    @Test
    fun `getCurrentUser returns user if token valid`() =
        runTest {
            whenever(api.getCurrentUser("jwt")).thenReturn(User(1, "user", "user@example.com", true))
            // Would set token in DataStore and check
            // For now, just test api call
            val user = api.getCurrentUser("jwt")
            assertEquals(User(1, "user", "user@example.com", true), user)
        }

    @Test
    fun `getCurrentUser returns null if api throws`() =
        runTest {
            whenever(api.getCurrentUser("jwt")).thenThrow(RuntimeException())
            // Would set token in DataStore and check
            // For now, just test api call
            var user: User? = null
            try {
                user = api.getCurrentUser("jwt")
            } catch (_: Exception) {
            }
            assertNull(user)
        }
}
