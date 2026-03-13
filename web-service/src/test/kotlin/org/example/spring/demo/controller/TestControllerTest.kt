package org.example.spring.demo.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Slice test for [TestController] using @WebMvcTest.
 * No database or full Spring context required — just the web layer.
 */
@WebMvcTest(TestController::class)
class TestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ping endpoint should return 200 OK`() {
        mockMvc
            .perform(get("/api/v1/ping"))
            .andExpect(status().isOk)
    }

    @Test
    fun `ping endpoint should return the string pong`() {
        val result =
            mockMvc
                .perform(get("/api/v1/ping"))
                .andExpect(status().isOk)
                .andReturn()

        assertThat(result.response.contentAsString).isEqualTo("pong")
    }

    @Test
    fun `ping endpoint should respond to GET only`() {
        // POST should be rejected (405 Method Not Allowed)
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/v1/ping"),
            ).andExpect(status().isMethodNotAllowed)
    }
}
