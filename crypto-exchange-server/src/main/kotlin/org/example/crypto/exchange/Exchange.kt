package org.example.crypto.exchange

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

class UserAlreadyHasActiveOrderException(userId: String) :
    IllegalStateException("User '$userId' already has an active order in the book; cancel or amend it first")

@Service
class Exchange(
    private val orderRepository: OrderRepository,
    private val tradeRepository: TradeRepository,
) {
    private val orderBook = OrderBook()

    @PostConstruct
    fun init() {
        // Load existing orders from database to initialize the order book
        val existingOrders = orderRepository.findAll()
        existingOrders.forEach {
            if (!it.isFilled()) {
                orderBook.addOrder(it)
            }
        }
    }

    @Transactional
    @Synchronized
    fun placeOrder(order: OrderEntity): List<TradeEntity> {
        if (orderBook.hasActiveOrderForUser(order.userId)) {
            throw UserAlreadyHasActiveOrderException(order.userId)
        }

        val trades = mutableListOf<TradeEntity>()

        // Persist the new order first
        orderRepository.save(order)

        if (order.side == OrderSide.BUY) {
            matchBuyOrder(order, trades)
        } else {
            matchSellOrder(order, trades)
        }

        order.status = when {
            order.isFilled() -> OrderStatus.FILLED
            order.remainingQuantity < order.quantity -> OrderStatus.PARTIALLY_FILLED
            else -> OrderStatus.OPEN
        }

        if (!order.isFilled() && !wouldCross(order)) {
            orderBook.addOrder(order)
        }

        // Save trades and updated orders
        tradeRepository.saveAll(trades)
        // Order is updated in match methods (remainingQuantity), but we must save it if matched
        // Actually, if it's an entity, the changes might be flushed automatically by Transactional,
        // but we explicitly saved it once, and we should ensure matched existing orders are also updated.
        // We'll explicitly save the orders to be sure.
        return trades
    }

    private fun matchBuyOrder(
        buyOrder: OrderEntity,
        trades: MutableList<TradeEntity>,
    ) {
        val iterator = orderBook.asks.iterator()
        while (iterator.hasNext() && !buyOrder.isFilled()) {
            val ask = iterator.next()
            if (ask.userId == buyOrder.userId) continue
            if (buyOrder.price >= ask.price) {
                val fillQuantity = buyOrder.remainingQuantity.min(ask.remainingQuantity)

                trades.add(
                    TradeEntity(
                        buyerId = buyOrder.userId,
                        sellerId = ask.userId,
                        price = ask.price, // Execution price is the existing order's price
                        quantity = fillQuantity,
                    ),
                )

                buyOrder.remainingQuantity -= fillQuantity
                ask.remainingQuantity -= fillQuantity
                ask.status = if (ask.isFilled()) OrderStatus.FILLED else OrderStatus.PARTIALLY_FILLED

                // Update the matching order in DB
                orderRepository.save(ask)

                if (ask.isFilled()) {
                    iterator.remove()
                    orderBook.deindex(ask.id)
                }
            } else {
                break // No more matching asks
            }
        }
        // Save the incoming order after matching
        orderRepository.save(buyOrder)
    }

    private fun matchSellOrder(
        sellOrder: OrderEntity,
        trades: MutableList<TradeEntity>,
    ) {
        val iterator = orderBook.bids.iterator()
        while (iterator.hasNext() && !sellOrder.isFilled()) {
            val bid = iterator.next()
            if (bid.userId == sellOrder.userId) continue
            if (sellOrder.price <= bid.price) {
                val fillQuantity = sellOrder.remainingQuantity.min(bid.remainingQuantity)

                trades.add(
                    TradeEntity(
                        buyerId = bid.userId,
                        sellerId = sellOrder.userId,
                        price = bid.price, // Execution price is the existing order's price
                        quantity = fillQuantity,
                    ),
                )

                sellOrder.remainingQuantity -= fillQuantity
                bid.remainingQuantity -= fillQuantity
                bid.status = if (bid.isFilled()) OrderStatus.FILLED else OrderStatus.PARTIALLY_FILLED

                // Update the matching order in DB
                orderRepository.save(bid)

                if (bid.isFilled()) {
                    iterator.remove()
                    orderBook.deindex(bid.id)
                }
            } else {
                break // No more matching bids
            }
        }
        // Save the incoming order after matching
        orderRepository.save(sellOrder)
    }

    private fun wouldCross(order: OrderEntity): Boolean =
        when (order.side) {
            OrderSide.BUY -> orderBook.asks.isNotEmpty() && order.price >= orderBook.asks.first().price
            OrderSide.SELL -> orderBook.bids.isNotEmpty() && order.price <= orderBook.bids.first().price
        }

    fun getOrderBook(): OrderBook = orderBook

    @Transactional
    @Synchronized
    fun cancelOrder(orderId: String): OrderEntity? {
        val order = orderBook.findById(orderId) ?: return null
        orderBook.removeOrder(order)
        order.status = OrderStatus.CANCELLED
        orderRepository.save(order)
        return order
    }

    @Transactional
    @Synchronized
    fun amendOrder(orderId: String, newPrice: BigDecimal, newQuantity: BigDecimal): OrderEntity? {
        val order = orderBook.findById(orderId) ?: return null
        // Remove first — price/timestamp are comparator keys, must not change while in the TreeSet
        orderBook.removeOrder(order)
        order.price = newPrice
        order.quantity = newQuantity
        order.remainingQuantity = newQuantity
        order.timestamp = Instant.now()   // price change loses time priority
        order.status = OrderStatus.OPEN
        orderRepository.save(order)

        // Re-run matching — the new price may now cross the opposite side
        val trades = mutableListOf<TradeEntity>()
        if (order.side == OrderSide.BUY) matchBuyOrder(order, trades) else matchSellOrder(order, trades)
        order.status = when {
            order.isFilled() -> OrderStatus.FILLED
            order.remainingQuantity < order.quantity -> OrderStatus.PARTIALLY_FILLED
            else -> OrderStatus.OPEN
        }
        if (!order.isFilled() && !wouldCross(order)) orderBook.addOrder(order)
        tradeRepository.saveAll(trades)
        return order
    }

    @Transactional
    @Synchronized
    fun clear() {
        tradeRepository.deleteAll()
        orderRepository.deleteAll()
        orderBook.clear()
    }
}
