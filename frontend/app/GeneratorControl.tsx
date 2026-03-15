"use client";

import { useState, useEffect } from "react";

export function GeneratorControl() {
    const [paused, setPaused] = useState<boolean | null>(null);

    useEffect(() => {
        fetch('/api/generator/status')
            .then(r => r.json())
            .then(d => setPaused(d.paused))
            .catch(() => {});
    }, []);

    const toggle = async () => {
        const action = paused ? 'resume' : 'pause';
        const res = await fetch(`/api/generator/${action}`, { method: 'POST' });
        const data = await res.json();
        setPaused(data.paused);
    };

    if (paused === null) return null;

    return paused ? (
        <button
            onClick={toggle}
            className="flex items-center gap-1.5 px-3 py-1 rounded text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/20 transition-colors"
        >
            <span>▶</span> Resume
        </button>
    ) : (
        <button
            onClick={toggle}
            className="flex items-center gap-1.5 px-3 py-1 rounded text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20 hover:bg-amber-500/20 transition-colors"
        >
            <span>⏸</span> Pause
        </button>
    );
}
