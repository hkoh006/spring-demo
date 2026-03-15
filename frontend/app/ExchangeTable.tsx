"use client";

import { Order, Trade } from "@/app/page";

function shortId(id: string) {
    return id.slice(0, 8);
}

function formatNumber(n: number | string) {
    return Number(n).toFixed(2);
}

function formatTime(ts: string) {
    const d = new Date(ts);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export function OrderTable({ orders }: { orders: Order[] }) {
    const sorted = [...orders].sort(
        (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    return (
        <section className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
            <div className="px-5 py-3 border-b border-zinc-800 flex items-center justify-between">
                <h2 className="text-sm font-semibold uppercase tracking-widest text-zinc-400">Order Blotter</h2>
                <span className="text-xs font-mono text-zinc-600">{orders.length} orders</span>
            </div>
            <div className="overflow-x-auto max-h-80 overflow-y-auto">
                <table className="w-full text-sm">
                    <thead className="sticky top-0 bg-zinc-950 text-zinc-500 text-xs uppercase tracking-wider">
                        <tr>
                            <Th>ID</Th>
                            <Th>User</Th>
                            <Th>Side</Th>
                            <Th align="right">Price</Th>
                            <Th align="right">Qty</Th>
                            <Th align="right">Remaining</Th>
                            <Th>Status</Th>
                            <Th>Time</Th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800/50">
                        {sorted.length === 0 ? (
                            <tr>
                                <td colSpan={8} className="text-center py-10 text-zinc-600">No orders yet</td>
                            </tr>
                        ) : (
                            sorted.map((order) => (
                                <tr key={order.id} className="hover:bg-zinc-800/40 transition-colors">
                                    <td className="px-4 py-2.5 font-mono text-xs text-zinc-500" title={order.id}>
                                        {shortId(order.id)}…
                                    </td>
                                    <td className="px-4 py-2.5 text-zinc-300">{order.userId}</td>
                                    <td className="px-4 py-2.5">
                                        <SideBadge side={order.side} />
                                    </td>
                                    <td className="px-4 py-2.5 text-right font-mono text-zinc-200">
                                        {formatNumber(order.price)}
                                    </td>
                                    <td className="px-4 py-2.5 text-right font-mono text-zinc-300">
                                        {formatNumber(order.quantity)}
                                    </td>
                                    <td className="px-4 py-2.5 text-right font-mono text-zinc-400">
                                        {formatNumber(order.remainingQuantity)}
                                    </td>
                                    <td className="px-4 py-2.5">
                                        <StatusBadge filled={order.isFilled} />
                                    </td>
                                    <td className="px-4 py-2.5 font-mono text-xs text-zinc-500">
                                        {formatTime(order.timestamp)}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

export function TradeTable({ trades }: { trades: Trade[] }) {
    const sorted = [...trades].sort(
        (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    );

    return (
        <section className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden">
            <div className="px-5 py-3 border-b border-zinc-800 flex items-center justify-between">
                <h2 className="text-sm font-semibold uppercase tracking-widest text-zinc-400">Trade Blotter</h2>
                <span className="text-xs font-mono text-zinc-600">{trades.length} trades</span>
            </div>
            <div className="overflow-x-auto max-h-80 overflow-y-auto">
                <table className="w-full text-sm">
                    <thead className="sticky top-0 bg-zinc-950 text-zinc-500 text-xs uppercase tracking-wider">
                        <tr>
                            <Th>ID</Th>
                            <Th>Buyer</Th>
                            <Th>Seller</Th>
                            <Th align="right">Price</Th>
                            <Th align="right">Qty</Th>
                            <Th>Time</Th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-zinc-800/50">
                        {sorted.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="text-center py-10 text-zinc-600">No trades yet</td>
                            </tr>
                        ) : (
                            sorted.map((trade) => (
                                <tr key={trade.id} className="hover:bg-zinc-800/40 transition-colors">
                                    <td className="px-4 py-2.5 font-mono text-xs text-zinc-500" title={trade.id}>
                                        {shortId(trade.id)}…
                                    </td>
                                    <td className="px-4 py-2.5 text-emerald-400">{trade.buyerId}</td>
                                    <td className="px-4 py-2.5 text-rose-400">{trade.sellerId}</td>
                                    <td className="px-4 py-2.5 text-right font-mono text-zinc-200">
                                        {formatNumber(trade.price)}
                                    </td>
                                    <td className="px-4 py-2.5 text-right font-mono text-zinc-300">
                                        {formatNumber(trade.quantity)}
                                    </td>
                                    <td className="px-4 py-2.5 font-mono text-xs text-zinc-500">
                                        {formatTime(trade.timestamp)}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </section>
    );
}

function Th({ children, align = "left" }: { children: React.ReactNode; align?: "left" | "right" }) {
    return (
        <th className={`px-4 py-2.5 font-medium text-${align}`}>{children}</th>
    );
}

function SideBadge({ side }: { side: 'BUY' | 'SELL' }) {
    return side === 'BUY'
        ? <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">BUY</span>
        : <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20">SELL</span>;
}

function StatusBadge({ filled }: { filled: boolean }) {
    return filled
        ? <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-zinc-700/50 text-zinc-400 border border-zinc-700">FILLED</span>
        : <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">OPEN</span>;
}
