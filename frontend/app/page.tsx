import { Suspense } from "react";
import { OrderTable, TradeTable } from "@/app/ExchangeTable";
import { AutoRefresh } from "@/app/AutoRefresh";
import { GeneratorControl } from "@/app/GeneratorControl";

async function fetchOrders(): Promise<Order[]> {
    try {
        const response = await fetch('http://localhost:8080/api/orders', { cache: 'no-store' });
        if (!response.ok) return [];
        return await response.json();
    } catch (e) {
        console.error("Failed to fetch orders", e);
        return [];
    }
}

async function fetchTrades(): Promise<Trade[]> {
    try {
        const response = await fetch('http://localhost:8080/api/trades', { cache: 'no-store' });
        if (!response.ok) return [];
        return await response.json();
    } catch (e) {
        console.error("Failed to fetch trades", e);
        return [];
    }
}

async function fetchMarketAnalytics(): Promise<MarketAnalytics | null> {
    try {
        const response = await fetch('http://localhost:8080/api/market/analytics', { cache: 'no-store' });
        if (!response.ok) return null;
        return await response.json();
    } catch (e) {
        console.error("Failed to fetch market analytics", e);
        return null;
    }
}

export default async function Home() {
    const [orders, trades, analytics] = await Promise.all([fetchOrders(), fetchTrades(), fetchMarketAnalytics()]);

    const openOrders = orders.filter(o => o.status === 'OPEN' || o.status === 'PARTIALLY_FILLED').length;
    const filledOrders = orders.filter(o => o.status === 'FILLED').length;
    const buyOrders = orders.filter(o => o.side === 'BUY').length;
    const sellOrders = orders.filter(o => o.side === 'SELL').length;

    return (
        <div className="min-h-screen bg-[#09090f] text-slate-200">
            <AutoRefresh intervalMs={3000} />

            {/* Header */}
            <header className="border-b border-zinc-800 bg-zinc-950/80 backdrop-blur sticky top-0 z-10">
                <div className="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <span className="text-lg font-bold tracking-tight text-white">⚡ Crypto Exchange</span>
                        <span className="text-xs text-zinc-500 font-mono">BTC/USDT</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <GeneratorControl />
                        <div className="flex items-center gap-2 text-xs text-zinc-500">
                            <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
                            </span>
                            <span>LIVE · refreshes every 3s</span>
                        </div>
                    </div>
                </div>
            </header>

            <main className="max-w-7xl mx-auto px-6 py-6 space-y-6">

                {/* Market Overview */}
                <div>
                    <p className="text-xs text-zinc-600 uppercase tracking-widest mb-2 px-1">Market Overview · 24h</p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
                        <StatCard label="Last Price" value={analytics?.lastPrice != null ? `$${Number(analytics.lastPrice).toFixed(2)}` : '—'} valueClass="text-yellow-400" />
                        <StatCard label="24h High" value={analytics?.high != null ? `$${Number(analytics.high).toFixed(2)}` : '—'} valueClass="text-emerald-400" />
                        <StatCard label="24h Low" value={analytics?.low != null ? `$${Number(analytics.low).toFixed(2)}` : '—'} valueClass="text-rose-400" />
                        <StatCard label="24h Volume" value={Number(analytics?.volume ?? 0).toFixed(2)} valueClass="text-sky-400" />
                        <StatCard label="Best Bid" value={analytics?.bestBid != null ? `$${Number(analytics.bestBid).toFixed(2)}` : '—'} valueClass="text-emerald-300" />
                        <StatCard label="Best Ask" value={analytics?.bestAsk != null ? `$${Number(analytics.bestAsk).toFixed(2)}` : '—'} valueClass="text-rose-300" />
                    </div>
                </div>

                {/* Order Stats */}
                <div>
                    <p className="text-xs text-zinc-600 uppercase tracking-widest mb-2 px-1">Order Stats</p>
                    <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
                        <StatCard label="Total Orders" value={orders.length} />
                        <StatCard label="Open" value={openOrders} valueClass="text-amber-400" />
                        <StatCard label="Filled" value={filledOrders} valueClass="text-zinc-400" />
                        <StatCard label="Buys" value={buyOrders} valueClass="text-emerald-400" />
                        <StatCard label="Sells" value={sellOrders} valueClass="text-rose-400" />
                    </div>
                </div>

                {/* Tables */}
                <Suspense fallback={<LoadingCard />}>
                    <OrderTable orders={orders} />
                    <TradeTable trades={trades} />
                </Suspense>
            </main>
        </div>
    );
}

function StatCard({ label, value, valueClass = "text-white" }: {
    label: string;
    value: number | string;
    valueClass?: string;
}) {
    return (
        <div className="bg-zinc-900 border border-zinc-800 rounded-lg px-4 py-3">
            <p className="text-xs text-zinc-500 uppercase tracking-widest mb-1">{label}</p>
            <p className={`text-xl font-bold font-mono ${valueClass}`}>{value}</p>
        </div>
    );
}

function LoadingCard() {
    return (
        <div className="bg-zinc-900 border border-zinc-800 rounded-lg p-8 text-center text-zinc-500 animate-pulse">
            Loading…
        </div>
    );
}

export interface MarketAnalytics {
    volume: number;
    high?: number;
    low?: number;
    lastPrice?: number;
    bestBid?: number;
    bestAsk?: number;
}

export type OrderStatus = 'OPEN' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'FILLED';

export interface Order {
    id: string;
    userId: string;
    side: 'BUY' | 'SELL';
    price: number;
    quantity: number;
    remainingQuantity: number;
    timestamp: string;
    isFilled: boolean;
    status: OrderStatus;
}

export interface Trade {
    id: string;
    buyerId: string;
    sellerId: string;
    price: number;
    quantity: number;
    timestamp: string;
}
