package org.example.spring.demo.websocket

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * Pure unit tests for [EchoWebSocketHandler] — no Spring context needed.
 *
 * Each WebSocket lifecycle event (connect, message, disconnect) is verified
 * in isolation with a mocked [WebSocketSession].
 */
@ExtendWith(MockKExtension::class)
class EchoWebSocketHandlerTest {
    @RelaxedMockK
    private lateinit var session: WebSocketSession

    private lateinit var handler: EchoWebSocketHandler

    @BeforeEach
    fun setUp() {
        handler = EchoWebSocketHandler()
        every { session.id } returns "session-123"
    }

    // -------------------------------------------------------------------------
    // afterConnectionEstablished
    // -------------------------------------------------------------------------

    @Test
    fun `should send connection acknowledgement message after handshake`() {
        handler.afterConnectionEstablished(session)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }

        assertThat(slot.captured.payload).contains("session-123")
    }

    @Test
    fun `connection acknowledgement should start with connected prefix`() {
        handler.afterConnectionEstablished(session)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }

        assertThat(slot.captured.payload).startsWith("connected:")
    }

    // -------------------------------------------------------------------------
    // handleTextMessage
    // -------------------------------------------------------------------------

    @Test
    fun `should echo incoming text message back to the session`() {
        val inbound = TextMessage("hello")

        handler.handleTextMessage(session, inbound)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }

        assertThat(slot.captured.payload).contains("hello")
    }

    @Test
    fun `echoed message should be prefixed with echo`() {
        val inbound = TextMessage("world")

        handler.handleTextMessage(session, inbound)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }

        assertThat(slot.captured.payload).startsWith("echo:")
    }

    @Test
    fun `should echo empty payload without throwing`() {
        val inbound = TextMessage("")

        handler.handleTextMessage(session, inbound)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }
        assertThat(slot.captured.payload).isNotNull()
    }

    @Test
    fun `should echo payload containing special characters`() {
        val payload = "hello <world> & \"quotes\""
        val inbound = TextMessage(payload)

        handler.handleTextMessage(session, inbound)

        val slot = slot<TextMessage>()
        verify { session.sendMessage(capture(slot)) }

        assertThat(slot.captured.payload).contains(payload)
    }

    // -------------------------------------------------------------------------
    // afterConnectionClosed
    // -------------------------------------------------------------------------

    @Test
    fun `should not throw when connection is closed normally`() {
        // Handler just logs — verify it does not throw
        handler.afterConnectionClosed(session, CloseStatus.NORMAL)
    }

    @Test
    fun `should not throw when connection is closed abnormally`() {
        handler.afterConnectionClosed(session, CloseStatus.SERVER_ERROR)
    }
}
