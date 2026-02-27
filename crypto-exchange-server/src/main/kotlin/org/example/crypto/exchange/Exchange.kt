package org.example.crypto.exchange

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    fun placeOrder(order: Order): List<Trade> {
        val trades = mutableListOf<Trade>()

        // Persist the new order first
        orderRepository.save(order)

        if (order.side == OrderSide.BUY) {
            matchBuyOrder(order, trades)
        } else {
            matchSellOrder(order, trades)
        }

        if (!order.isFilled()) {
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
        buyOrder: Order,
        trades: MutableList<Trade>,
    ) {
        val iterator = orderBook.asks.iterator()
        while (iterator.hasNext() && !buyOrder.isFilled()) {
            val ask = iterator.next()
            if (buyOrder.price >= ask.price) {
                val fillQuantity = buyOrder.remainingQuantity.min(ask.remainingQuantity)

                trades.add(
                    Trade(
                        buyerId = buyOrder.userId,
                        sellerId = ask.userId,
                        price = ask.price, // Execution price is the existing order's price
                        quantity = fillQuantity,
                    ),
                )

                buyOrder.remainingQuantity -= fillQuantity
                ask.remainingQuantity -= fillQuantity

                // Update the matching order in DB
                orderRepository.save(ask)

                if (ask.isFilled()) {
                    iterator.remove()
                }
            } else {
                break // No more matching asks
            }
        }
        // Save the incoming order after matching
        orderRepository.save(buyOrder)
    }

    private fun matchSellOrder(
        sellOrder: Order,
        trades: MutableList<Trade>,
    ) {
        val iterator = orderBook.bids.iterator()
        while (iterator.hasNext() && !sellOrder.isFilled()) {
            val bid = iterator.next()
            if (sellOrder.price <= bid.price) {
                val fillQuantity = sellOrder.remainingQuantity.min(bid.remainingQuantity)

                trades.add(
                    Trade(
                        buyerId = bid.userId,
                        sellerId = sellOrder.userId,
                        price = bid.price, // Execution price is the existing order's price
                        quantity = fillQuantity,
                    ),
                )

                sellOrder.remainingQuantity -= fillQuantity
                bid.remainingQuantity -= fillQuantity

                // Update the matching order in DB
                orderRepository.save(bid)

                if (bid.isFilled()) {
                    iterator.remove()
                }
            } else {
                break // No more matching bids
            }
        }
        // Save the incoming order after matching
        orderRepository.save(sellOrder)
    }

    fun getOrderBook(): OrderBook = orderBook

    @Transactional
    @Synchronized
    fun clear() {
        tradeRepository.deleteAll()
        orderRepository.deleteAll()
        orderBook.bids.clear()
        orderBook.asks.clear()
    }
}
