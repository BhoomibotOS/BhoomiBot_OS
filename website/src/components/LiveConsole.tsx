"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, ShieldAlert } from 'lucide-react'

interface LiveConsoleProps {
  robotId: string
  sessionId: string
}

export function LiveConsole({ robotId, sessionId }: LiveConsoleProps) {
  const [isConnected, setIsConnected] = useState(false)
  const [frameCount, setFrameCount] = useState(0)
  const [lastFrameTime, setLastFrameTime] = useState<number>(0)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/api/relay?robotId=${robotId}`

    console.log(`[Video] Connecting to ${wsUrl}`)
    const ws = new WebSocket(wsUrl)
    socketRef.current = ws

    ws.onopen = () => {
      console.log(`[Video] Socket Open. Sending Handshake for ${robotId}`)
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
      // HANDLE BINARY VIDEO FRAMES
      if (event.data instanceof Blob || event.data instanceof ArrayBuffer) {
        setFrameCount(prev => prev + 1)
        setLastFrameTime(Date.now())
        renderFrame(event.data)
      } else {
        // HANDLE JSON MESSAGES
        try {
          const msg = JSON.parse(event.data)
          if (msg.type === 'PEER_STATUS') {
             console.log("[Video] Peer Status Update:", msg.payload)
          }
        } catch (e) {}
      }
    }

    ws.onclose = () => {
      setIsConnected(false)
      console.log("[Video] Socket Closed. Reconnecting...")
      setTimeout(connect, 3000)
    }
  }

  const renderFrame = (data: Blob | ArrayBuffer) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const blob = data instanceof Blob ? data : new Blob([data], { type: 'image/jpeg' })
    const url = URL.createObjectURL(blob)
    const img = new Image()

    img.onload = () => {
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
      URL.revokeObjectURL(url)
    }
    img.src = url
  }

  useEffect(() => {
    connect()
    return () => socketRef.current?.close()
  }, [robotId, sessionId])

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-full relative">
      {/* Video Feed */}
      <div className="relative flex-1 bg-black flex items-center justify-center overflow-hidden">
        <canvas
          ref={canvasRef}
          width={1280}
          height={720}
          className="w-full h-full object-contain"
        />

        {!isConnected && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80 backdrop-blur-md">
            <Wifi size={40} className="text-primary animate-pulse mb-4" />
            <p className="text-white font-black tracking-tighter uppercase">Initializing Link...</p>
          </div>
        )}

        {isConnected && frameCount === 0 && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-sm">
            <Video size={40} className="text-slate-600 mb-4 animate-bounce" />
            <p className="text-slate-400 font-bold tracking-tighter uppercase">Waiting for Robot Video Stream</p>
            <p className="text-[8px] text-slate-500 mt-2 font-mono">{robotId} :: {sessionId}</p>
          </div>
        )}

        {/* HUD Overlay */}
        <div className="absolute top-6 left-6 flex gap-3 pointer-events-none">
          <div className="bg-black/60 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full ${frameCount > 0 ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase tracking-widest text-white">
              {frameCount > 0 ? 'Live Feed Active' : 'No Data'}
            </span>
          </div>
          {frameCount > 0 && (
            <div className="bg-primary/20 border border-primary/50 px-4 py-2 rounded-full backdrop-blur-md flex items-center gap-2">
               <Activity size={12} className="text-primary" />
               <span className="text-[10px] font-mono text-primary font-bold">
                 {Math.round(1000 / (Date.now() - lastFrameTime + 1))} FPS
               </span>
            </div>
          )}
        </div>
      </div>

      {/* Footer Stats */}
      <div className="p-6 bg-zinc-950 border-t border-white/5 flex justify-between items-center">
         <div className="flex gap-6">
            <div>
               <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Frames Rx</p>
               <p className="text-lg font-black text-white font-mono">{frameCount}</p>
            </div>
            <div>
               <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Bitrate</p>
               <p className="text-lg font-black text-white font-mono">{frameCount > 0 ? 'High' : '--'}</p>
            </div>
         </div>
         <div className="text-right">
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Signal Path</p>
            <p className="text-[10px] font-bold text-primary italic">Cloudflare Edge + DO</p>
         </div>
      </div>
    </div>
  )
}
