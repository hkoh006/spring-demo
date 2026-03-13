package org.example.spring.demo.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

/**
 * Pure unit tests for [EchoWebSocketHandler] — no Spring context needed.
 *
 * Each WebSocket lifecycle event (connect, message, disconnect) is verified
 * in isolation with a mocked [WebSocketSession].
 */
@ExtendWith(MockitoExtension::class)
class EchoWebSocketHandlerTest {
    @Mock
    private lateinit var session: WebSocketSession

    private lateinit var handler: EchoWebSocketHandler

    @BeforeEach
    fun setUp() {
        handler = EchoWebSocketHandler()
        `when`(session.id).thenReturn("session-123")
    }

    // -------------------------------------------------------------------------
    // afterConnectionEstablished
    // -------------------------------------------------------------------------

    @Test
    fun `should send connection acknowledgement message after handshake`() {
        handler.afterConnectionEstablished(session)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())

        assertThat(captor.value.payload).contains("session-123")
    }

    @Test
    fun `connection acknowledgement should start with connected prefix`() {
        handler.afterConnectionEstablished(session)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())

        assertThat(captor.value.payload).startsWith("connected:")
    }

    // -------------------------------------------------------------------------
    // handleTextMessage
    // -------------------------------------------------------------------------

    @Test
    fun `should echo incoming text message back to the session`() {
        val inbound = TextMessage("hello")

        handler.handleTextMessage(session, inbound)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())

        assertThat(captor.value.payload).contains("hello")
    }

    @Test
    fun `echoed message should be prefixed with echo`() {
        val inbound = TextMessage("world")

        handler.handleTextMessage(session, inbound)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())

        assertThat(captor.value.payload).startsWith("echo:")
    }

    @Test
    fun `should echo empty payload without throwing`() {
        val inbound = TextMessage("")

        handler.handleTextMessage(session, inbound)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())
        assertThat(captor.value.payload).isNotNull()
    }

    @Test
    fun `should echo payload containing special characters`() {
        val payload = "hello <world> & \"quotes\""
        val inbound = TextMessage(payload)

        handler.handleTextMessage(session, inbound)

        val captor = ArgumentCaptor.forClass(TextMessage::class.java)
        verify(session).sendMessage(captor.capture())

        assertThat(captor.value.payload).contains(payload)
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
