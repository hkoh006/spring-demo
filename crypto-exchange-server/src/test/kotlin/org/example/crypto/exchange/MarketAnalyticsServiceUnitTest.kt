package org.example.crypto.exchange

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockKExtension::class)
class MarketAnalyticsServiceUnitTest {
    @MockK
    private lateinit var tradeRepository: TradeRepository

    @RelaxedMockK
    private lateinit var orderRepository: OrderRepository

    private lateinit var exchange: Exchange
    private lateinit var service: MarketAnalyticsService

    @BeforeEach
    fun setUp() {
        every { orderRepository.findAll() } returns emptyList()
        every { orderRepository.save(any()) } answers { firstArg() }
        exchange = Exchange(orderRepository, tradeRepository)
        exchange.init()
        service = MarketAnalyticsService(tradeRepository, exchange)
    }

    @Test
    fun `returns null price fields and zero volume when no trades exist`() {
        every { tradeRepository.findAggregatesSince(any()) } returns emptyAggregates()
        every { tradeRepository.findTopByOrderByTimestampDesc() } returns null

        val result = service.getAnalytics()

        assertThat(result.high).isNull()
        assertThat(result.low).isNull()
        assertThat(result.lastPrice).isNull()
        assertThat(result.volume).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `returns correct high, low, volume and lastPrice when trades exist`() {
        every { tradeRepository.findAggregatesSince(any()) } returns
            aggregatesWith(
                high = BigDecimal("150"),
                low = BigDecimal("90"),
                volume = BigDecimal("5.0"),
            )
        every { tradeRepository.findTopByOrderByTimestampDesc() } returns tradeEntity(price = BigDecimal("120"))

        val result = service.getAnalytics()

        assertThat(result.high).isEqualByComparingTo("150")
        assertThat(result.low).isEqualByComparingTo("90")
        assertThat(result.volume).isEqualByComparingTo("5.0")
        assertThat(result.lastPrice).isEqualByComparingTo("120")
    }

    @Test
    fun `returns bestBid and bestAsk from live order book`() {
        every { tradeRepository.findAggregatesSince(any()) } returns emptyAggregates()
        every { tradeRepository.findTopByOrderByTimestampDesc() } returns null
        every { tradeRepository.saveAll(any<List<TradeEntity>>()) } answers { firstArg() }
        every { orderRepository.save(any()) } answers { firstArg() }

        exchange.placeOrder(
            OrderEntity(
                userId = "u1",
                side = OrderSide.BUY,
                price = BigDecimal("100"),
                quantity = BigDecimal("1"),
            ),
        )
        exchange.placeOrder(
            OrderEntity(
                userId = "u2",
                side = OrderSide.SELL,
                price = BigDecimal("105"),
                quantity = BigDecimal("1"),
            ),
        )

        val result = service.getAnalytics()

        assertThat(result.bestBid).isEqualByComparingTo("100")
        assertThat(result.bestAsk).isEqualByComparingTo("105")
    }

    @Test
    fun `returns null bestBid and bestAsk when order book is empty`() {
        every { tradeRepository.findAggregatesSince(any()) } returns emptyAggregates()
        every { tradeRepository.findTopByOrderByTimestampDesc() } returns null

        val result = service.getAnalytics()

        assertThat(result.bestBid).isNull()
        assertThat(result.bestAsk).isNull()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun emptyAggregates() = aggregatesWith(high = null, low = null, volume = null)

    private fun aggregatesWith(
        high: BigDecimal?,
        low: BigDecimal?,
        volume: BigDecimal?,
    ) = object : TradeAggregates {
        override fun getHigh() = high

        override fun getLow() = low

        override fun getVolume() = volume
    }

    private fun tradeEntity(price: BigDecimal) =
        TradeEntity(
            buyerId = "buyer",
            sellerId = "seller",
            price = price,
            quantity = BigDecimal("1"),
            timestamp = Instant.now(),
        )
}
