package org.example.spring.demo.websocket

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * A minimal WebSocket handler that echoes incoming text messages.
 * You can extend or replace this bean to implement domain-specific behavior.
 */
@Component
class EchoWebSocketHandler : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("WebSocket connected: {}", session.id)
        session.sendMessage(TextMessage($$"connected: ${session.id}"))
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        val payload = message.payload
        log.debug("WebSocket message from {}: {}", session.id, payload)
        session.sendMessage(TextMessage($$"echo: $payload"))
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        log.info("WebSocket disconnected: {} - {}", session.id, status)
    }
}
