"use client"

import React, { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { Wifi, Video, Play, Square, MessageSquare, ShieldAlert } from 'lucide-react'

interface LiveConsoleProps {
  robotId: string
  sessionId: string
}

export function LiveConsole({ robotId, sessionId }: LiveConsoleProps) {
  const [isConnected, setIsConnected] = useState(false)
  const [peerStatus, setPeerStatus] = useState({ robot: false, operator: false })
  const [lastTelemetry, setLastTelemetry] = useState<any>(null)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    // Determine the protocol based on the current environment
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/api/relay?robotId=${robotId}`

    const ws = new WebSocket(wsUrl)
    socketRef.current = ws

    ws.onopen = () => {
      // Send HELLO Handshake as expected by your server.js logic
      const hello = {
        type: "HELLO",
        robotId: robotId,
        ts: Date.now(),
        payload: JSON.stringify({ role: "OPERATOR", session: sessionId })
      }
      ws.send(JSON.stringify(hello))
      setIsConnected(true)
    }

    ws.onmessage = async (event) => {
      if (event.data instanceof Blob) {
        // VIDEO FRAME (Binary)
        renderFrame(event.data)
      } else {
        // JSON DATA (Telemetry or Status)
        try {
          const msg = JSON.parse(event.data)
          if (msg.type === 'PEER_STATUS') {
            setPeerStatus(JSON.parse(msg.payload))
          } else if (msg.type === 'TELEMETRY') {
            setLastTelemetry(msg.payload)
          }
        } catch (e) {
          console.error("Failed to parse JSON message", e)
        }
      }
    }

    ws.onclose = () => {
      setIsConnected(false)
      setPeerStatus({ robot: false, operator: false })
      setTimeout(connect, 3000) // Auto-reconnect
    }
  }

  const renderFrame = (blob: Blob) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const img = new Image()
    img.onload = () => {
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      URL.revokeObjectURL(img.src)
    }
    img.src = URL.createObjectURL(blob)
  }

  const sendCommand = (cmd: string, params: any = {}) => {
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) return

    const envelope = {
      type: "COMMAND",
      robotId: robotId,
      ts: Date.now(),
      payload: JSON.stringify({ command: cmd, ...params })
    }
    socketRef.current.send(JSON.stringify(envelope))
  }

  useEffect(() => {
    connect()
    return () => socketRef.current?.close()
  }, [robotId, sessionId])

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-full">
      {/* Video Feed Area */}
      <div className="relative flex-1 bg-black flex items-center justify-center min-h-[300px]">
        <canvas
          ref={canvasRef}
          width={640}
          height={480}
          className="w-full h-full object-contain"
        />

        {!peerStatus.robot && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm">
            <Video size={48} className="text-slate-700 mb-4 animate-pulse" />
            <p className="text-white font-bold tracking-tighter">WAITING FOR ROBOT FEED...</p>
            <p className="text-slate-500 text-[10px] mt-2 font-mono uppercase tracking-[0.2em]">Robot ID: {robotId}</p>
          </div>
        )}

        <div className="absolute top-6 left-6 flex gap-3">
          <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full backdrop-blur-md border ${
            isConnected ? 'bg-green-500/20 border-green-500/50 text-green-500' : 'bg-red-500/20 border-red-500/50 text-red-500'
          }`}>
             <div className={`w-1.5 h-1.5 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
             <span className="text-[10px] font-black uppercase tracking-widest">{isConnected ? 'Relay Linked' : 'Disconnected'}</span>
          </div>
          {peerStatus.robot && (
            <div className="bg-primary/20 border border-primary/50 text-primary px-3 py-1.5 rounded-full backdrop-blur-md text-[10px] font-black uppercase tracking-widest">
              Live Feed
            </div>
          )}
        </div>
      </div>

      {/* Control Overlay */}
      <div className="p-8 border-t border-white/5 bg-zinc-950">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
           <button
             onMouseDown={() => sendCommand('MOVE', { dir: 'F' })}
             onMouseUp={() => sendCommand('STOP')}
             className="p-4 bg-white/5 hover:bg-primary hover:text-black rounded-2xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Drive Fwd
           </button>
           <button
             onClick={() => sendCommand('ATTACHMENT_ACTION', { action: 'START' })}
             className="p-4 bg-white/5 hover:bg-primary hover:text-black rounded-2xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Actuator Start
           </button>
           <button
             onClick={() => sendCommand('MISSION', { action: 'AUTO_START' })}
             className="p-4 bg-white/5 hover:bg-primary hover:text-black rounded-2xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Auto Mission
           </button>
           <button
             onClick={() => sendCommand('STOP_ALL')}
             className="p-4 bg-destructive/20 border border-destructive/50 text-destructive hover:bg-destructive hover:text-white rounded-2xl transition-all font-black text-[10px] uppercase tracking-widest"
           >
             E-Stop
           </button>
        </div>

        <div className="flex justify-between items-center text-slate-500">
           <div className="flex gap-4">
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest">Robot ID</p>
                <p className="text-xs font-bold text-white">{robotId}</p>
              </div>
              <div>
                <p className="text-[8px] font-black uppercase tracking-widest">Session</p>
                <p className="text-xs font-bold text-white">{sessionId}</p>
              </div>
           </div>
           <div className="text-right">
              <p className="text-[8px] font-black uppercase tracking-widest">Data Stream</p>
              <p className="text-[10px] font-mono text-primary">WebRTC Relay Active</p>
           </div>
        </div>
      </div>
    </div>
  )
}
