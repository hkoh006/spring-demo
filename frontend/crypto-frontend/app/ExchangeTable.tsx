"use client";

import {Order, Trade} from "@/app/page";

export function OrderTable({orders}: { orders: Order[] }) {
    return (
        <div style={{marginBottom: '2rem'}}>
            <h2>Order Blotter</h2>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>User ID</th>
                    <th>Side</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Remaining</th>
                    <th>Status</th>
                    <th>Timestamp</th>
                </tr>
                </thead>
                <tbody>
                {orders.length === 0 ? (
                    <tr>
                        <td colSpan={8}>No orders found.</td>
                    </tr>
                ) : (
                    orders.map((order) => (
                        <tr key={order.id}>
                            <td>{order.id}</td>
                            <td>{order.userId}</td>
                            <td style={{color: order.side === 'BUY' ? 'green' : 'red'}}>{order.side}</td>
                            <td>{order.price}</td>
                            <td>{order.quantity}</td>
                            <td>{order.remainingQuantity}</td>
                            <td>{order.isFilled ? 'FILLED' : 'OPEN'}</td>
                            <td>{new Date(order.timestamp).toLocaleString()}</td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </div>
    )
}

export function TradeTable({trades}: { trades: Trade[] }) {
    return (
        <div>
            <h2>Trade Blotter</h2>
            <table>
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Buyer ID</th>
                    <th>Seller ID</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Timestamp</th>
                </tr>
                </thead>
                <tbody>
                {trades.length === 0 ? (
                    <tr>
                        <td colSpan={6}>No trades found.</td>
                    </tr>
                ) : (
                    trades.map((trade) => (
                        <tr key={trade.id}>
                            <td>{trade.id}</td>
                            <td>{trade.buyerId}</td>
                            <td>{trade.sellerId}</td>
                            <td>{trade.price}</td>
                            <td>{trade.quantity}</td>
                            <td>{new Date(trade.timestamp).toLocaleString()}</td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </div>
    )
}
