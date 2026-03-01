import {Suspense} from "react";
import {OrderTable, TradeTable} from "@/app/ProductTable";

async function fetchOrders(): Promise<Order[]> {
    try {
        const response = await fetch('http://localhost:8080/api/orders', {cache: 'no-store'});
        if (!response.ok) return [];
        return await response.json();
    } catch (e) {
        console.error("Failed to fetch orders", e);
        return [];
    }
}

async function fetchTrades(): Promise<Trade[]> {
    try {
        const response = await fetch('http://localhost:8080/api/trades', {cache: 'no-store'});
        if (!response.ok) return [];
        return await response.json();
    } catch (e) {
        console.error("Failed to fetch trades", e);
        return [];
    }
}

export default async function Home() {
    const orders = await fetchOrders();
    const trades = await fetchTrades();

    return (
        <main style={{padding: '2rem'}}>
            <h1>Crypto Exchange Dashboard</h1>
            <Suspense fallback={<div>Loading tables...</div>}>
                <OrderTable orders={orders}/>
                <TradeTable trades={trades}/>
            </Suspense>
        </main>
    );
}

export interface Order {
    id: string;
    userId: string;
    side: 'BUY' | 'SELL';
    price: number;
    quantity: number;
    remainingQuantity: number;
    timestamp: string;
    isFilled: boolean;
}

export interface Trade {
    id: string;
    buyerId: string;
    sellerId: string;
    price: number;
    quantity: number;
    timestamp: string;
}
