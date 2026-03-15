import { NextRequest, NextResponse } from 'next/server';

const BACKEND = 'http://localhost:8080';

export async function GET(_: NextRequest, { params }: { params: Promise<{ action: string }> }) {
    const { action } = await params;
    const res = await fetch(`${BACKEND}/api/generator/${action}`, { cache: 'no-store' });
    return NextResponse.json(await res.json());
}

export async function POST(_: NextRequest, { params }: { params: Promise<{ action: string }> }) {
    const { action } = await params;
    const res = await fetch(`${BACKEND}/api/generator/${action}`, { method: 'POST' });
    return NextResponse.json(await res.json());
}
