package org.example.crypto.exchange

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.math.BigDecimal

@SpringBootTest(properties = ["spring.jpa.hibernate.ddl-auto=create-drop"])
@Import(TestcontainersPostgresConfig::class)
class ExchangeTest {
    @Autowired
    private lateinit var exchange: Exchange

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    @BeforeEach
    fun clearDatabase() {
        exchange.clear()
    }

    @Test
    fun `test simple match and persistence`() {
        val sellOrder =
            OrderEntity(
                userId = "seller",
                side = OrderSide.SELL,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.5"),
            )
        exchange.placeOrder(sellOrder)

        val buyOrder =
            OrderEntity(
                userId = "buyer",
                side = OrderSide.BUY,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            )
        val trades = exchange.placeOrder(buyOrder)

        assertThat(trades).hasSize(1)
        assertThat(trades[0].quantity).isEqualTo(BigDecimal("1.0"))
        assertThat(trades[0].price).isEqualTo(BigDecimal("100"))
        assertThat(trades[0].buyerId).isEqualTo("buyer")
        assertThat(trades[0].sellerId).isEqualTo("seller")

        assertThat(buyOrder.isFilled()).isTrue()
        assertThat(sellOrder.remainingQuantity).isEqualTo(BigDecimal("0.5"))
        assertThat(exchange.getOrderBook().asks).hasSize(1)
        assertThat(exchange.getOrderBook().bids).isEmpty()

        // Verify persistence
        val savedOrders = orderRepository.findAll()
        assertThat(savedOrders).hasSize(2)
        val savedSellOrder = orderRepository.findById(sellOrder.id).get()
        assertThat(savedSellOrder.remainingQuantity.setScale(2)).isEqualTo(BigDecimal("0.50"))

        val savedTrades = tradeRepository.findAll()
        assertThat(savedTrades).hasSize(1)
        assertThat(savedTrades[0].quantity.setScale(2)).isEqualTo(BigDecimal("1.00"))
    }

    @Test
    fun `test price priority`() {
        // Higher sell price
        exchange.placeOrder(
            OrderEntity(
                userId = "seller1",
                side = OrderSide.SELL,
                price = BigDecimal("101"),
                quantity = BigDecimal("1.0"),
            ),
        )

        // Lower sell price (should be matched first)
        exchange.placeOrder(
            OrderEntity(
                userId = "seller2",
                side = OrderSide.SELL,
                price = BigDecimal("100"),
                quantity = BigDecimal("1.0"),
            ),
        )

        val buyOrder =
            OrderEntity(
                userId = "buyer",
                side = OrderSide.BUY,
                price = BigDecimal("105"),
                quantity = BigDecimal("1.5"),
            )
        val trades = exchange.placeOrder(buyOrder)

        assertThat(trades).hasSize(2)
        assertThat(trades[0].sellerId).isEqualTo("seller2") // Price 100 matched first
        assertThat(trades[0].quantity).isEqualTo(BigDecimal("1.0"))

        assertThat(trades[1].sellerId).isEqualTo("seller1") // Price 101 matched second
        assertThat(trades[1].quantity).isEqualTo(BigDecimal("0.5"))
    }
}
