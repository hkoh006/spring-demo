package org.example.crypto.exchange.controller

import org.example.crypto.exchange.Exchange
import org.example.crypto.exchange.OrderEntity
import org.example.crypto.exchange.OrderRepository
import org.example.crypto.exchange.OrderSide
import org.example.crypto.exchange.TestcontainersPostgresConfig
import org.example.crypto.exchange.TradeEntity
import org.example.crypto.exchange.TradeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import java.io.File
import java.math.BigDecimal

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.jpa.hibernate.ddl-auto=create-drop"],
)
@AutoConfigureRestTestClient
@Import(TestcontainersPostgresConfig::class)
class EndpointTest {
    @Autowired
    private lateinit var client: RestTestClient

    @Autowired
    private lateinit var exchange: Exchange

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    @BeforeEach
    fun setup() {
        exchange.clear()
    }

    @Test
    fun `test get orders`() {
        val order =
            OrderEntity(
                userId = "user1",
                side = OrderSide.BUY,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            )
        orderRepository.save(order)

        client
            .get()
            .uri("/api/orders")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$[0].id")
            .isEqualTo(order.id)
            .jsonPath("$[0].userId")
            .isEqualTo("user1")

        client
            .get()
            .uri("/api/orders/${order.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(order.id)
    }

    @Test
    fun `test get trades`() {
        val trade =
            TradeEntity(
                buyerId = "buyer",
                sellerId = "seller",
                price = BigDecimal("100"),
                quantity = BigDecimal("0.5"),
            )
        tradeRepository.save(trade)

        client
            .get()
            .uri("/api/trades")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$[0].id")
            .isEqualTo(trade.id)
            .jsonPath("$[0].buyerId")
            .isEqualTo("buyer")

        client
            .get()
            .uri("/api/trades/${trade.id}")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(trade.id)
    }

    @Test
    fun `generate openapi spec`() {
        val result =
            client
                .get()
                .uri("/v3/api-docs.yaml")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

        val file = File("src/main/resources/crypto-exchange-openapi.yaml")
        file.parentFile.mkdirs()
        file.writeText(result!!)
    }
}
