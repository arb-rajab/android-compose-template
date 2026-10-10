package com.example.starterapp.remote

import com.example.starterapp.data.ServerConfig
import com.example.starterapp.data.remote.dto.ActionRequestDto
import com.example.starterapp.di.ApiClientFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

/**
 * Round-trips real HTTP requests through [ApiClientFactory] and
 * [com.example.starterapp.data.remote.StarterApi] against a [MockWebServer]. This catches
 * serialization and URL-construction bugs that unit tests faking the API interface cannot.
 */
class StarterApiIntegrationTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun apiFor(config: ServerConfig = ServerConfig(baseUrl = server.url("/").toString())) =
        ApiClientFactory.create(config)

    @Test
    fun `submitAction posts the payload and parses snake_case fields`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"a1","created_at":"2026-01-01T00:00:00Z"}"""
            )
        )

        val response = apiFor().submitAction(ActionRequestDto("hello"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/actions", request.path)
        assertTrue(request.body.readUtf8().contains("\"payload\":\"hello\""))
        assertEquals("a1", response.id)
        assertEquals("2026-01-01T00:00:00Z", response.createdAt)
    }

    @Test
    fun `unknown response fields are ignored`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id":"a1","created_at":"x","extra":true}"""
            )
        )

        assertEquals("a1", apiFor().submitAction(ActionRequestDto("hello")).id)
    }

    @Test
    fun `configured static auth header is attached to every request`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"id":"a1","created_at":"x"}""")
        )
        val config =
            ServerConfig(
                baseUrl = server.url("/").toString(),
                authHeaderName = "X-Api-Key",
                authHeaderValue = "secret-token"
            )

        apiFor(config).submitAction(ActionRequestDto("hello"))

        assertEquals("secret-token", server.takeRequest().getHeader("X-Api-Key"))
    }

    @Test
    fun `no auth header is sent when none is configured`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"id":"a1","created_at":"x"}""")
        )

        apiFor().submitAction(ActionRequestDto("hello"))

        assertNull(server.takeRequest().getHeader("X-Api-Key"))
    }

    @Test
    fun `a 4xx response surfaces as an HttpException rather than being swallowed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"invalid"}"""))

        try {
            apiFor().submitAction(ActionRequestDto(""))
            throw AssertionError("expected HttpException")
        } catch (exc: HttpException) {
            assertEquals(422, exc.code())
        }
    }
}
