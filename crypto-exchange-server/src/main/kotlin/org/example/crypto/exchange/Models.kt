package org.example.crypto.exchange

import java.math.BigDecimal
import java.time.Instant
import java.util.*

enum class OrderSide {
    BUY, SELL
}

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val side: OrderSide,
    val price: BigDecimal,
    val quantity: BigDecimal,
    var remainingQuantity: BigDecimal = quantity,
    val timestamp: Instant = Instant.now()
) {
    fun isFilled(): Boolean = remainingQuantity <= BigDecimal.ZERO
}

data class Trade(
    val buyerId: String,
    val sellerId: String,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val timestamp: Instant = Instant.now()
)

class OrderBook {
    // Bids (BUYS): Highest price first. If same price, oldest first.
    val bids = TreeSet<Order>(
        compareByDescending<Order> { it.price }
            .thenBy { it.timestamp }
            .thenBy { it.id }
    )

    // Asks (SELLS): Lowest price first. If same price, oldest first.
    val asks = TreeSet<Order>(
        compareBy<Order> { it.price }
            .thenBy { it.timestamp }
            .thenBy { it.id }
    )

    fun addOrder(order: Order) {
        when (order.side) {
            OrderSide.BUY -> bids.add(order)
            OrderSide.SELL -> asks.add(order)
        }
    }

    fun removeOrder(order: Order) {
        when (order.side) {
            OrderSide.BUY -> bids.remove(order)
            OrderSide.SELL -> asks.remove(order)
        }
    }
}
