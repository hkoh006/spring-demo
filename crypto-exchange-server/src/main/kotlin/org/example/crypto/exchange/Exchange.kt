package org.example.crypto.exchange

class Exchange {
    private val orderBook = OrderBook()

    @Synchronized
    fun placeOrder(order: Order): List<Trade> {
        val trades = mutableListOf<Trade>()

        if (order.side == OrderSide.BUY) {
            matchBuyOrder(order, trades)
        } else {
            matchSellOrder(order, trades)
        }

        if (!order.isFilled()) {
            orderBook.addOrder(order)
        }

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

                if (ask.isFilled()) {
                    iterator.remove()
                }
            } else {
                break // No more matching asks
            }
        }
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

                if (bid.isFilled()) {
                    iterator.remove()
                }
            } else {
                break // No more matching bids
            }
        }
    }

    fun getOrderBook(): OrderBook = orderBook
}
