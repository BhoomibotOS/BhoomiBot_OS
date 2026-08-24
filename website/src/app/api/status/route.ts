import { NextResponse } from 'next/server';

// This is required to make the route run on Cloudflare Workers (Edge Runtime)
export const runtime = 'edge';

export async function GET() {
  return NextResponse.json({
    status: 'online',
    system: 'BhoomiBot VCU Relay',
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV,
    region: 'Cloudflare Edge'
  });
}

export async function POST(request: Request) {
  try {
    const body = await request.json();

    // In the future, this is where we'd process telemetry from the robot
    console.log('Received telemetry:', body);

    return NextResponse.json({
      message: 'Telemetry received',
      receivedAt: new Date().toISOString(),
      ack: true
    });
  } catch (error) {
    return NextResponse.json(
      { error: 'Invalid telemetry packet' },
      { status: 400 }
    );
  }
}
