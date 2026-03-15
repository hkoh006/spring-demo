package org.example.crypto.exchange

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.example.crypto.exchange.OrderStatus.CANCELLED
import org.example.crypto.exchange.OrderStatus.FILLED
import org.example.crypto.exchange.OrderStatus.OPEN
import org.example.crypto.exchange.OrderStatus.PARTIALLY_FILLED
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
    // One-active-order-per-user enforcement
    // -------------------------------------------------------------------------

    @Nested
    inner class OneActiveOrderPerUser {
        @Test
        fun `placing a second order while one is active should throw`() {
            exchange.placeOrder(sellOrder(userId = "alice", price = "100", qty = "1.0"))

            assertThatThrownBy {
                exchange.placeOrder(sellOrder(userId = "alice", price = "99", qty = "1.0"))
            }.isInstanceOf(UserAlreadyHasActiveOrderException::class.java)
        }

        @Test
        fun `user can place a new order after their previous order is fully filled`() {
            exchange.placeOrder(sellOrder(userId = "alice", price = "100", qty = "1.0"))
            exchange.placeOrder(buyOrder(userId = "bob", price = "100", qty = "1.0")) // fills alice's order

            // alice's order is now filled and removed from the book — she can place a new one
            exchange.placeOrder(sellOrder(userId = "alice", price = "100", qty = "1.0"))
            // bob is filled too, so this new sell stays in the book
            assertThat(exchange.getOrderBook().asks).hasSize(1)
        }

        @Test
        fun `user can place a new order after cancelling their active order`() {
            val order = sellOrder(userId = "alice", price = "100", qty = "1.0")
            exchange.placeOrder(order)
            exchange.cancelOrder(order.id)

            // No exception expected
            exchange.placeOrder(sellOrder(userId = "alice", price = "105", qty = "2.0"))
            assertThat(exchange.getOrderBook().asks).hasSize(1)
        }
    }

    // -------------------------------------------------------------------------
    // Cancel order
    // -------------------------------------------------------------------------

    @Nested
    inner class CancelOrder {
        @Test
        fun `cancelOrder should remove the order from the book and mark it CANCELLED`() {
            val order = sellOrder(userId = "alice", price = "100", qty = "1.0")
            exchange.placeOrder(order)

            val cancelled = exchange.cancelOrder(order.id)

            assertThat(cancelled).isNotNull
            assertThat(cancelled!!.status).isEqualTo(CANCELLED)
            assertThat(exchange.getOrderBook().asks).isEmpty()
        }

        @Test
        fun `cancelOrder should return null for an unknown id`() {
            assertThat(exchange.cancelOrder("non-existent")).isNull()
        }

        @Test
        fun `cancelOrder should persist CANCELLED status`() {
            val order = sellOrder(userId = "alice", price = "100", qty = "1.0")
            exchange.placeOrder(order)
            exchange.cancelOrder(order.id)

            verify { orderRepository.save(match<OrderEntity> { it.id == order.id && it.status == CANCELLED }) }
        }
    }

    // -------------------------------------------------------------------------
    // Amend order
    // -------------------------------------------------------------------------

    @Nested
    inner class AmendOrder {
        @Test
        fun `amendOrder should update price and quantity in the book`() {
            val order = sellOrder(userId = "alice", price = "100", qty = "1.0")
            exchange.placeOrder(order)

            val amended = exchange.amendOrder(order.id, BigDecimal("105"), BigDecimal("2.0"))

            assertThat(amended).isNotNull
            assertThat(amended!!.price).isEqualByComparingTo("105")
            assertThat(amended.quantity).isEqualByComparingTo("2.0")
            assertThat(amended.remainingQuantity).isEqualByComparingTo("2.0")
            assertThat(amended.status).isEqualTo(OPEN)
        }

        @Test
        fun `amendOrder should keep the order in the book at the new price`() {
            val order = sellOrder(userId = "alice", price = "100", qty = "1.0")
            exchange.placeOrder(order)
            exchange.amendOrder(order.id, BigDecimal("110"), BigDecimal("1.0"))

            assertThat(exchange.getOrderBook().asks).hasSize(1)
            assertThat(exchange.getOrderBook().asks.first().price).isEqualByComparingTo("110")
        }

        @Test
        fun `amendOrder should return null for an unknown id`() {
            assertThat(exchange.amendOrder("non-existent", BigDecimal("100"), BigDecimal("1.0"))).isNull()
        }

        @Test
        fun `amended order that now crosses should match immediately`() {
            // bob has a buy at 100 in the book
            exchange.placeOrder(buyOrder(userId = "bob", price = "100", qty = "1.0"))
            // alice places a sell at 110 (no match)
            val aliceSell = sellOrder(userId = "alice", price = "110", qty = "1.0")
            exchange.placeOrder(aliceSell)

            // alice amends price down to 99 — now crosses bob's bid
            exchange.amendOrder(aliceSell.id, BigDecimal("99"), BigDecimal("1.0"))

            // After amend the order is re-placed and should match bob's bid
            assertThat(exchange.getOrderBook().asks).isEmpty()
            assertThat(exchange.getOrderBook().bids).isEmpty()
        }
    }

    // -------------------------------------------------------------------------
    // Order status tracking
    // -------------------------------------------------------------------------

    @Nested
    inner class OrderStatusTracking {
        @Test
        fun `unfilled resting order should have OPEN status`() {
            val order = buyOrder(price = "100", qty = "1.0")
            exchange.placeOrder(order)

            assertThat(order.status).isEqualTo(OPEN)
        }

        @Test
        fun `fully filled incoming order should have FILLED status`() {
            exchange.placeOrder(sellOrder(price = "100", qty = "1.0"))
            val buy = buyOrder(price = "100", qty = "1.0")
            exchange.placeOrder(buy)

            assertThat(buy.status).isEqualTo(FILLED)
        }

        @Test
        fun `partially filled incoming order should have PARTIALLY_FILLED status`() {
            exchange.placeOrder(sellOrder(price = "100", qty = "0.5"))
            val buy = buyOrder(price = "100", qty = "1.0")
            exchange.placeOrder(buy)

            assertThat(buy.status).isEqualTo(PARTIALLY_FILLED)
        }

        @Test
        fun `resting order that gets fully consumed should have FILLED status`() {
            val resting = sellOrder(price = "100", qty = "1.0")
            exchange.placeOrder(resting)
            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))

            assertThat(resting.status).isEqualTo(FILLED)
        }

        @Test
        fun `resting order that gets partially consumed should have PARTIALLY_FILLED status`() {
            val resting = sellOrder(price = "100", qty = "2.0")
            exchange.placeOrder(resting)
            exchange.placeOrder(buyOrder(price = "100", qty = "1.0"))

            assertThat(resting.status).isEqualTo(PARTIALLY_FILLED)
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
