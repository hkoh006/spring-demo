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

enum class OrderStatus {
    OPEN,
    PARTIALLY_FILLED,
    PARTIALLY_FILLED_CANCELLED,
    CANCELLED,
    FILLED,
}

@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    @Enumerated(EnumType.STRING)
    val side: OrderSide,
    var price: BigDecimal,
    var quantity: BigDecimal,
    var remainingQuantity: BigDecimal = quantity,
    var timestamp: Instant = Instant.now(),
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.OPEN,
) {
    // JPA requires a no-arg constructor
    constructor() : this("", "", OrderSide.BUY, BigDecimal.ZERO, BigDecimal.ZERO)

    fun isFilled(): Boolean = remainingQuantity <= BigDecimal.ZERO
}

@Entity
@Table(name = "trades")
data class TradeEntity(
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

class OrderBook {
    // Bids (BUYS): Highest price first. If same price, oldest first.
    val bids: TreeSet<OrderEntity> =
        TreeSet(
            compareByDescending<OrderEntity> { it.price }
                .thenBy { it.timestamp }
                .thenBy { it.id },
        )

    // Asks (SELLS): Lowest price first. If same price, oldest first.
    val asks: TreeSet<OrderEntity> =
        TreeSet(
            compareBy<OrderEntity> { it.price }
                .thenBy { it.timestamp }
                .thenBy { it.id },
        )

    // Fast lookup by orderId for cancel/amend
    private val index: MutableMap<String, OrderEntity> = mutableMapOf()

    fun addOrder(order: OrderEntity) {
        when (order.side) {
            OrderSide.BUY -> bids.add(order)
            OrderSide.SELL -> asks.add(order)
        }
        index[order.id] = order
    }

    fun removeOrder(order: OrderEntity) {
        when (order.side) {
            OrderSide.BUY -> bids.remove(order)
            OrderSide.SELL -> asks.remove(order)
        }
        index.remove(order.id)
    }

    fun findById(id: String): OrderEntity? = index[id]

    fun hasActiveOrderForUser(userId: String): Boolean = index.values.any { it.userId == userId }

    // Called after iterator.remove() during matching to keep the index in sync
    fun deindex(orderId: String) { index.remove(orderId) }

    fun clear() {
        bids.clear()
        asks.clear()
        index.clear()
    }
}
