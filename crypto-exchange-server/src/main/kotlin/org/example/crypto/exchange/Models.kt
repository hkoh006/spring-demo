package org.example.crypto.exchange

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.TreeSet
import java.util.UUID

enum class OrderSide {
    BUY,
    SELL,
}

@Entity
@Table(name = "orders")
data class Order(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    @Enumerated(EnumType.STRING)
    val side: OrderSide,
    val price: BigDecimal,
    val quantity: BigDecimal,
    var remainingQuantity: BigDecimal = quantity,
    val timestamp: Instant = Instant.now(),
) {
    // JPA requires a no-arg constructor
    constructor() : this("", "", OrderSide.BUY, BigDecimal.ZERO, BigDecimal.ZERO)

    fun isFilled(): Boolean = remainingQuantity <= BigDecimal.ZERO
}

@Entity
@Table(name = "trades")
data class Trade(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val buyerId: String,
    val sellerId: String,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val timestamp: Instant = Instant.now(),
) {
    // JPA requires a no-arg constructor
    constructor() : this("", "", "", BigDecimal.ZERO, BigDecimal.ZERO)
}

data class OrderEvent(
    val userId: String,
    val side: OrderSide,
    val price: BigDecimal,
    val quantity: BigDecimal,
)

data class OrderBook(
    // Bids (BUYS): Highest price first. If same price, oldest first.
    val bids: TreeSet<Order> =
        TreeSet(
            compareByDescending<Order> { it.price }
                .thenBy { it.timestamp }
                .thenBy { it.id },
        ),
    // Asks (SELLS): Lowest price first. If same price, oldest first.
    val asks: TreeSet<Order> =
        TreeSet(
            compareBy<Order> { it.price }
                .thenBy { it.timestamp }
                .thenBy { it.id },
        ),
) {
    fun addOrder(order: Order) {
        when (order.side) {
            OrderSide.BUY -> bids.add(order)
            OrderSide.SELL -> asks.add(order)
        }
    }
}
