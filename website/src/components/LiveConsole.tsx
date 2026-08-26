"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, AlertCircle, PlayCircle, ShieldAlert, Cpu } from 'lucide-react'

export function LiveConsole({ robotId, sessionId }: any) {
  const [status, setStatus] = useState('offline')
  const [error, setError] = useState<string | null>(null)
  const [frameCount, setFrameCount] = useState(0)
  const [isRobotOnline, setIsRobotOnline] = useState(false)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    if (socketRef.current) socketRef.current.close()

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/api/relay?robotId=${robotId}`

      console.log("[Console] Connecting to:", wsUrl)
      setStatus('connecting')
      const ws = new WebSocket(wsUrl)
      socketRef.current = ws

      ws.onopen = () => {
        const hello = {
          type: "HELLO",
          robotId: robotId,
          ts: Date.now(),
          payload: JSON.stringify({ role: "OPERATOR", session: sessionId })
        }
        ws.send(JSON.stringify(hello))
        setStatus('connected')
        setError(null)
      }

      ws.onmessage = (event) => {
        if (event.data instanceof Blob || event.data instanceof ArrayBuffer) {
          setFrameCount(prev => prev + 1)
          renderFrame(event.data)
        } else {
          try {
            const msg = JSON.parse(event.data)
            if (msg.type === 'PEER_STATUS') {
              const peers = JSON.parse(msg.payload)
              setIsRobotOnline(peers.robot)
              console.log("[Console] Robot status:", peers.robot ? "ONLINE" : "OFFLINE")
            }
          } catch(e) {}
        }
      }

      ws.onerror = (e) => {
        console.error("[Console] WebSocket Error:", e)
        setError("WebSocket link failed. Trying again...")
        setStatus('error')
      }

      ws.onclose = () => {
        if (status !== 'error') setStatus('offline')
        setIsRobotOnline(false)
      }
    } catch (e: any) {
      setError(e.message)
      setStatus('error')
    }
  }

  const renderFrame = (data: any) => {
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

  const sendCommand = (command: string, params: any = {}) => {
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) return
    const envelope = {
      type: "COMMAND",
      robotId: robotId,
      ts: Date.now(),
      payload: JSON.stringify({ command, ...params })
    }
    socketRef.current.send(JSON.stringify(envelope))
  }

  useEffect(() => {
    connect()
    return () => socketRef.current?.close()
  }, [robotId, sessionId])

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-full relative">
      <div className="relative flex-1 bg-black flex items-center justify-center">
        <canvas ref={canvasRef} width={1280} height={720} className="w-full h-full object-contain" />

        {status === 'offline' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80">
            <button
              onClick={connect}
              className="bg-primary text-black px-8 py-4 rounded-2xl font-black uppercase tracking-widest flex items-center gap-3 hover:scale-105 transition-transform"
            >
              <PlayCircle size={20} /> Establish Video Link
            </button>
            <p className="text-slate-500 text-[10px] mt-4 font-mono uppercase">Robot ID: {robotId}</p>
          </div>
        )}

        {status === 'connecting' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60">
            <Wifi className="text-primary animate-pulse mb-4" size={40} />
            <p className="text-white font-bold uppercase tracking-widest text-sm">Connecting to Edge Relay...</p>
          </div>
        )}

        {status === 'error' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-red-950/20 backdrop-blur-md">
            <AlertCircle className="text-red-500 mb-4" size={40} />
            <p className="text-white font-bold">RELAY LINK FAILED</p>
            <button onClick={connect} className="mt-4 text-primary text-xs font-black uppercase underline">Retry Connection</button>
          </div>
        )}

        {!isRobotOnline && status === 'connected' && (
           <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm">
             <ShieldAlert className="text-yellow-500 mb-4 animate-pulse" size={40} />
             <p className="text-white font-bold uppercase tracking-widest text-sm">Waiting for Robot...</p>
             <p className="text-slate-500 text-[10px] mt-2 text-center px-12">Ensure your Robot Phone is connected to the same ID and Session.</p>
           </div>
        )}

        <div className="absolute top-6 left-6 flex flex-col gap-3">
          <div className="bg-black/60 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full ${status === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase tracking-widest text-white">
              {status === 'connected' ? 'Relay Active' : 'Offline'}
            </span>
          </div>
          {isRobotOnline && (
            <div className="bg-green-500/20 border border-green-500/50 px-4 py-2 rounded-full backdrop-blur-md flex items-center gap-3">
              <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <span className="text-[10px] font-black uppercase tracking-widest text-green-500">Robot Online</span>
            </div>
          )}
        </div>
      </div>

      {/* Manual Controls Overlay */}
      {isRobotOnline && (
        <div className="p-6 bg-zinc-950 border-t border-white/5 grid grid-cols-2 md:grid-cols-4 gap-4">
           <button
             onMouseDown={() => sendCommand('MOVE', { dir: 'F' })}
             onMouseUp={() => sendCommand('STOP')}
             className="p-3 bg-white/5 hover:bg-primary hover:text-black rounded-xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Drive Fwd
           </button>
           <button
             onClick={() => sendCommand('TOGGLE_ACTION')}
             className="p-3 bg-white/5 hover:bg-primary hover:text-black rounded-xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Tool Toggle
           </button>
           <button
             onClick={() => sendCommand('REQUEST_TELEMETRY')}
             className="p-3 bg-white/5 hover:bg-primary hover:text-black rounded-xl border border-white/10 transition-all font-black text-[10px] uppercase tracking-widest"
           >
             Get Status
           </button>
           <button
             onClick={() => sendCommand('STOP_ALL')}
             className="p-3 bg-red-500/20 border border-red-500/50 text-red-500 hover:bg-red-500 hover:text-white rounded-xl transition-all font-black text-[10px] uppercase tracking-widest"
           >
             E-Stop
           </button>
        </div>
      )}

      <div className="p-4 bg-black border-t border-white/5 flex justify-between items-center">
         <div>
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Frames Rx</p>
            <p className="text-lg font-black text-white font-mono">{frameCount}</p>
         </div>
         <div className="text-right">
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Relay Node</p>
            <p className="text-[10px] font-bold text-primary italic">Cloudflare Durable Object v7</p>
         </div>
      </div>
    </div>
  )
}
