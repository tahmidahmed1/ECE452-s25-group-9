package com.example.gooddeedfeed

import com.example.gooddeedfeed.auth.SignInRequest
import com.example.gooddeedfeed.auth.SignUpRequest
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRoutesTest {
    companion object {
        init {
            val env =
                dotenv {
                    directory = "./" // adjust as needed
                    ignoreIfMalformed = false
                    ignoreIfMissing = false
                }
            env.entries().forEach {
                val key = it.key
                val value = it.value
                if (System.getenv(key) == null) {
                    System.setProperty(key, value)
                }
            }
        }
    }

    @Test
    fun testSignUpAndSignInAndMe() =
        testApplication {
            application {
                configureAuth()
            }

            val json =
                Json {
                    ignoreUnknownKeys = true
                }
            val username = "testuser"
            val password = "testpass"

            val signUpResponse =
                client.post("/auth/signup") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(SignUpRequest(username, password)))
                }
            assertEquals(HttpStatusCode.Created, signUpResponse.status)
            val signUpBody = signUpResponse.bodyAsText()
            println("SignUp Response: $signUpBody")
            val token = json.parseToJsonElement(signUpBody).jsonObject["token"]?.jsonPrimitive?.content
            assertNotNull(token)

            val signInResponse =
                client.post("/auth/signin") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(SignInRequest(username, password)))
                }
            assertEquals(HttpStatusCode.OK, signInResponse.status)
            val signInBody = signInResponse.bodyAsText()
            println("SignIn Response: $signInBody")
            val token2 = json.parseToJsonElement(signInBody).jsonObject["token"]?.jsonPrimitive?.content
            assertNotNull(token2)

            val meResponse =
                client.get("/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $token2")
                }
            assertEquals(HttpStatusCode.OK, meResponse.status)
            val meBody = meResponse.bodyAsText()
            println("Me Response: $meBody")
            assertTrue(meBody.contains(username))
        }
}
