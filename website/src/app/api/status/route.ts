import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({
    status: 'online',
    system: 'BhoomiBot VCU Relay',
    timestamp: new Date().toISOString(),
    environment: process.env.NODE_ENV,
    region: 'Cloudflare Workers'
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
