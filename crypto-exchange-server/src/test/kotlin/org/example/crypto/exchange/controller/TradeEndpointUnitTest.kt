package org.example.crypto.exchange.controller

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.example.crypto.exchange.TradeEntity
import org.example.crypto.exchange.TradeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

/**
 * Pure unit tests for [TradeEndpoint] — no Spring context loaded.
 *
 * HTTP wiring is tested at the integration level in [EndpointTest].
 */
@ExtendWith(MockKExtension::class)
class TradeEndpointUnitTest {
    @MockK
    private lateinit var tradeRepository: TradeRepository

    private lateinit var endpoint: TradeEndpoint

    @BeforeEach
    fun setUp() {
        endpoint = TradeEndpoint(tradeRepository)
    }

    // -------------------------------------------------------------------------
    // getAllTrades
    // -------------------------------------------------------------------------

    @Test
    fun `getAllTrades should return 200 with empty list when no trades exist`() {
        every { tradeRepository.findAll() } returns emptyList()

        val response = endpoint.getAllTrades()

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `getAllTrades should map all trade fields correctly`() {
        val entity =
            tradeEntity(
                id = "trade-1",
                buyerId = "buyer-A",
                sellerId = "seller-B",
                price = BigDecimal("99.99"),
                quantity = BigDecimal("0.5"),
            )
        every { tradeRepository.findAll() } returns listOf(entity)

        val body = endpoint.getAllTrades().body!!

        assertThat(body).hasSize(1)
        with(body[0]) {
            assertThat(id).isEqualTo("trade-1")
            assertThat(buyerId).isEqualTo("buyer-A")
            assertThat(sellerId).isEqualTo("seller-B")
            assertThat(price).isEqualByComparingTo("99.99")
            assertThat(quantity).isEqualByComparingTo("0.5")
        }
    }

    @Test
    fun `getAllTrades should return all trades when multiple exist`() {
        val trades =
            listOf(
                tradeEntity(id = "t1"),
                tradeEntity(id = "t2"),
                tradeEntity(id = "t3"),
            )
        every { tradeRepository.findAll() } returns trades

        val body = endpoint.getAllTrades().body!!
        assertThat(body.map { it.id }).containsExactly("t1", "t2", "t3")
    }

    @Test
    fun `getAllTrades should map timestamp to UTC OffsetDateTime`() {
        val instant = Instant.parse("2024-01-01T09:30:00Z")
        val entity = tradeEntity(id = "ts-trade", timestamp = instant)
        every { tradeRepository.findAll() } returns listOf(entity)

        val dto = endpoint.getAllTrades().body!![0]
        assertThat(dto.timestamp.toInstant()).isEqualTo(instant)
    }

    // -------------------------------------------------------------------------
    // getTradeById
    // -------------------------------------------------------------------------

    @Test
    fun `getTradeById should return 200 with trade when found`() {
        val entity = tradeEntity(id = "trade-99", buyerId = "buyer-X", sellerId = "seller-Y")
        every { tradeRepository.findById("trade-99") } returns Optional.of(entity)

        val response = endpoint.getTradeById("trade-99")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        with(response.body!!) {
            assertThat(id).isEqualTo("trade-99")
            assertThat(buyerId).isEqualTo("buyer-X")
            assertThat(sellerId).isEqualTo("seller-Y")
        }
    }

    @Test
    fun `getTradeById should return 404 when trade does not exist`() {
        every { tradeRepository.findById("missing") } returns Optional.empty()

        val response = endpoint.getTradeById("missing")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body).isNull()
    }

    @Test
    fun `getTradeById should map timestamp to UTC OffsetDateTime`() {
        val instant = Instant.parse("2024-06-15T12:00:00Z")
        val entity = tradeEntity(id = "t-trade", timestamp = instant)
        every { tradeRepository.findById("t-trade") } returns Optional.of(entity)

        val dto = endpoint.getTradeById("t-trade").body!!
        assertThat(dto.timestamp.toInstant()).isEqualTo(instant)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun tradeEntity(
        id: String = "trade-id",
        buyerId: String = "buyer",
        sellerId: String = "seller",
        price: BigDecimal = BigDecimal("100"),
        quantity: BigDecimal = BigDecimal("1.0"),
        timestamp: Instant = Instant.now(),
    ) = TradeEntity(
        id = id,
        buyerId = buyerId,
        sellerId = sellerId,
        price = price,
        quantity = quantity,
        timestamp = timestamp,
    )
}
