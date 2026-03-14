package org.example.crypto.exchange

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

/**
 * Fast unit tests for [Exchange] business logic using MockK mocks.
 *
 * These do NOT spin up a Spring context or a database — the goal is to verify
 * the matching engine logic in isolation.
 *
 * Integration/persistence tests live in [ExchangeTest] (Testcontainers).
 */
@ExtendWith(MockKExtension::class)
class ExchangeUnitTest {
    @RelaxedMockK
    private lateinit var orderRepository: OrderRepository

    @RelaxedMockK
    private lateinit var tradeRepository: TradeRepository

    private lateinit var exchange: Exchange

    @BeforeEach
    fun setUp() {
        // findAll() during @PostConstruct init() — return empty list by default
        every { orderRepository.findAll() } returns emptyList()
        // save() has a generic return type <S: OrderEntity> — stub it to return the argument
        every { orderRepository.save(any()) } answers { firstArg() }
        exchange = Exchange(orderRepository, tradeRepository)
        exchange.init()
    }

    // -------------------------------------------------------------------------
    // No-match scenarios
    // -------------------------------------------------------------------------

    @Nested
    inner class NoMatch {
        @Test
        fun `should produce no trades when order book is empty`() {
            val buyOrder = buyOrder(price = "100", qty = "1.0")
            val trades = exchange.placeOrder(buyOrder)

            assertThat(trades).isEmpty()
        }

        @Test
        fun `should produce no trades when buy price is below best ask`() {
            exchange.placeOrder(sellOrder(price = "105", qty = "1.0"))
            val buyOrder = buyOrder(price = "100", qty = "1.0")

            val trades = exchange.placeOrder(buyOrder)

            assertThat(trades).isEmpty()
        }

        @Test
        fun `should produce no trades when sell price is above best bid`() {
            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))
            val sellOrder = sellOrder(price = "105", qty = "1.0")

            val trades = exchange.placeOrder(sellOrder)

            assertThat(trades).isEmpty()
        }

        @Test
        fun `unfilled order should be added to the order book`() {
            val buyOrder = buyOrder(price = "100", qty = "1.0")
            exchange.placeOrder(buyOrder)

            assertThat(exchange.getOrderBook().bids).hasSize(1)
            assertThat(exchange.getOrderBook().asks).isEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // Full-fill scenarios
    // -------------------------------------------------------------------------

    @Nested
    inner class FullFill {
        @Test
        fun `buy order should fully match a resting sell at the same price`() {
            exchange.placeOrder(sellOrder(userId = "seller", price = "100", qty = "1.0"))

            val buyOrder = buyOrder(userId = "buyer", price = "100", qty = "1.0")
            val trades = exchange.placeOrder(buyOrder)

            assertThat(trades).hasSize(1)
            with(trades[0]) {
                assertThat(buyerId).isEqualTo("buyer")
                assertThat(sellerId).isEqualTo("seller")
                assertThat(price).isEqualByComparingTo("100")
                assertThat(quantity).isEqualByComparingTo("1.0")
            }
        }

        @Test
        fun `sell order should fully match a resting buy at the same price`() {
            exchange.placeOrder(buyOrder(userId = "buyer", price = "100", qty = "1.0"))

            val sellOrder = sellOrder(userId = "seller", price = "100", qty = "1.0")
            val trades = exchange.placeOrder(sellOrder)

            assertThat(trades).hasSize(1)
            with(trades[0]) {
                assertThat(buyerId).isEqualTo("buyer")
                assertThat(sellerId).isEqualTo("seller")
            }
        }

        @Test
        fun `buy order matched against an ask uses the ask price (price improvement for buyer)`() {
            // Buyer places at 110, existing ask is at 100 — execution price should be 100 (the ask price)
            exchange.placeOrder(sellOrder(userId = "seller", price = "100", qty = "1.0"))
            val trades = exchange.placeOrder(buyOrder(userId = "buyer", price = "110", qty = "1.0"))

            assertThat(trades).hasSize(1)
            assertThat(trades[0].price).isEqualByComparingTo("100")
        }

        @Test
        fun `sell order matched against a bid uses the bid price (price improvement for seller)`() {
            // Seller places at 90, existing bid is at 100 — execution price should be 100 (the bid price)
            exchange.placeOrder(buyOrder(userId = "buyer", price = "100", qty = "1.0"))
            val trades = exchange.placeOrder(sellOrder(userId = "seller", price = "90", qty = "1.0"))

            assertThat(trades).hasSize(1)
            assertThat(trades[0].price).isEqualByComparingTo("100")
        }

        @Test
        fun `fully filled orders should be removed from the order book`() {
            exchange.placeOrder(sellOrder(price = "100", qty = "1.0"))
            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))

            assertThat(exchange.getOrderBook().bids).isEmpty()
            assertThat(exchange.getOrderBook().asks).isEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // Partial-fill scenarios
    // -------------------------------------------------------------------------

    @Nested
    inner class PartialFill {
        @Test
        fun `buy order larger than ask should partially fill and leave remainder in book`() {
            exchange.placeOrder(sellOrder(price = "100", qty = "0.5"))
            val buyOrder = buyOrder(price = "100", qty = "1.0")
            val trades = exchange.placeOrder(buyOrder)

            assertThat(trades).hasSize(1)
            assertThat(trades[0].quantity).isEqualByComparingTo("0.5")
            assertThat(buyOrder.remainingQuantity).isEqualByComparingTo("0.5")
            // The partially filled buy should sit in the book
            assertThat(exchange.getOrderBook().bids).hasSize(1)
        }

        @Test
        fun `sell order larger than bid should partially fill and leave remainder in book`() {
            exchange.placeOrder(buyOrder(price = "100", qty = "0.5"))
            val sellOrder = sellOrder(price = "100", qty = "1.0")
            val trades = exchange.placeOrder(sellOrder)

            assertThat(trades).hasSize(1)
            assertThat(trades[0].quantity).isEqualByComparingTo("0.5")
            assertThat(sellOrder.remainingQuantity).isEqualByComparingTo("0.5")
            assertThat(exchange.getOrderBook().asks).hasSize(1)
        }

        @Test
        fun `resting order partially consumed should remain in the book with reduced remaining quantity`() {
            val resting = sellOrder(price = "100", qty = "2.0")
            exchange.placeOrder(resting)

            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))

            assertThat(resting.remainingQuantity).isEqualByComparingTo("1.0")
            assertThat(exchange.getOrderBook().asks).hasSize(1)
        }
    }

    // -------------------------------------------------------------------------
    // Multi-level matching
    // -------------------------------------------------------------------------

    @Nested
    inner class MultiLevelMatching {
        @Test
        fun `buy order should match against multiple asks in ascending price order`() {
            exchange.placeOrder(sellOrder(userId = "s1", price = "101", qty = "1.0"))
            exchange.placeOrder(sellOrder(userId = "s2", price = "100", qty = "1.0"))

            val trades = exchange.placeOrder(buyOrder(price = "105", qty = "2.0"))

            assertThat(trades).hasSize(2)
            // Best ask (lowest price) comes first
            assertThat(trades[0].sellerId).isEqualTo("s2")
            assertThat(trades[0].price).isEqualByComparingTo("100")
            assertThat(trades[1].sellerId).isEqualTo("s1")
            assertThat(trades[1].price).isEqualByComparingTo("101")
        }

        @Test
        fun `sell order should match against multiple bids in descending price order`() {
            exchange.placeOrder(buyOrder(userId = "b1", price = "99", qty = "1.0"))
            exchange.placeOrder(buyOrder(userId = "b2", price = "100", qty = "1.0"))

            val trades = exchange.placeOrder(sellOrder(price = "95", qty = "2.0"))

            assertThat(trades).hasSize(2)
            // Best bid (highest price) comes first
            assertThat(trades[0].buyerId).isEqualTo("b2")
            assertThat(trades[0].price).isEqualByComparingTo("100")
            assertThat(trades[1].buyerId).isEqualTo("b1")
            assertThat(trades[1].price).isEqualByComparingTo("99")
        }

        @Test
        fun `buy order should stop matching when ask price exceeds buy price`() {
            exchange.placeOrder(sellOrder(userId = "s1", price = "100", qty = "1.0"))
            exchange.placeOrder(sellOrder(userId = "s2", price = "110", qty = "1.0"))

            val trades = exchange.placeOrder(buyOrder(price = "105", qty = "5.0"))

            // Only the ask at 100 should match; ask at 110 is above the buy limit of 105
            assertThat(trades).hasSize(1)
            assertThat(trades[0].sellerId).isEqualTo("s1")
        }
    }

    // -------------------------------------------------------------------------
    // Order book state after clear()
    // -------------------------------------------------------------------------

    @Nested
    inner class Clear {
        @Test
        fun `clear should empty bids and asks`() {
            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))
            exchange.placeOrder(sellOrder(price = "105", qty = "1.0"))

            exchange.clear()

            assertThat(exchange.getOrderBook().bids).isEmpty()
            assertThat(exchange.getOrderBook().asks).isEmpty()
        }

        @Test
        fun `clear should delegate to tradeRepository deleteAll`() {
            exchange.clear()
            verify { tradeRepository.deleteAll() }
        }

        @Test
        fun `clear should delegate to orderRepository deleteAll`() {
            exchange.clear()
            verify { orderRepository.deleteAll() }
        }
    }

    // -------------------------------------------------------------------------
    // init() — order book pre-population
    // -------------------------------------------------------------------------

    @Nested
    inner class Init {
        @Test
        fun `init should load unfilled orders into the order book`() {
            val unfilled = buyOrder(price = "100", qty = "1.0")
            every { orderRepository.findAll() } returns listOf(unfilled)

            val freshExchange = Exchange(orderRepository, tradeRepository)
            freshExchange.init()

            assertThat(freshExchange.getOrderBook().bids).containsExactly(unfilled)
        }

        @Test
        fun `init should skip fully filled orders`() {
            val filled = buyOrder(price = "100", qty = "1.0", remainingQty = BigDecimal.ZERO)
            every { orderRepository.findAll() } returns listOf(filled)

            val freshExchange = Exchange(orderRepository, tradeRepository)
            freshExchange.init()

            assertThat(freshExchange.getOrderBook().bids).isEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // Repository interaction
    // -------------------------------------------------------------------------

    @Test
    fun `placeOrder should save the incoming order to the repository`() {
        val order = buyOrder(price = "100", qty = "1.0")
        exchange.placeOrder(order)

        verify(atLeast = 1) { orderRepository.save(order) }
    }

    @Test
    fun `placeOrder should save resulting trades to the trade repository`() {
        exchange.placeOrder(sellOrder(price = "100", qty = "1.0"))
        val buyOrder = buyOrder(price = "100", qty = "1.0")
        exchange.placeOrder(buyOrder)

        verify { tradeRepository.saveAll(match<List<TradeEntity>> { it.size == 1 }) }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buyOrder(
        userId: String = "buyer",
        price: String,
        qty: String,
        remainingQty: BigDecimal = BigDecimal(qty),
    ) = OrderEntity(
        userId = userId,
        side = OrderSide.BUY,
        price = BigDecimal(price),
        quantity = BigDecimal(qty),
        remainingQuantity = remainingQty,
    )

    private fun sellOrder(
        userId: String = "seller",
        price: String,
        qty: String,
        remainingQty: BigDecimal = BigDecimal(qty),
    ) = OrderEntity(
        userId = userId,
        side = OrderSide.SELL,
        price = BigDecimal(price),
        quantity = BigDecimal(qty),
        remainingQuantity = remainingQty,
    )
}
