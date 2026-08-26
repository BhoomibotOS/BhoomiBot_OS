"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, AlertCircle } from 'lucide-react'

export function LiveConsole({ robotId, sessionId }: any) {
  const [status, setStatus] = useState('initializing')
  const [error, setError] = useState<string | null>(null)
  const [frameCount, setFrameCount] = useState(0)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/api/relay?robotId=${robotId}`

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
        }
      }

      ws.onerror = () => {
        setError("Connection failed. Check if Relay Binding is active.")
        setStatus('error')
      }

      ws.onclose = () => {
        if (status !== 'error') setStatus('disconnected')
        setTimeout(connect, 5000)
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

  useEffect(() => {
    connect()
    return () => socketRef.current?.close()
  }, [robotId, sessionId])

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-full relative">
      <div className="relative flex-1 bg-black flex items-center justify-center">
        <canvas ref={canvasRef} width={1280} height={720} className="w-full h-full object-contain" />

        {status !== 'connected' && status !== 'error' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60">
            <Wifi className="text-primary animate-pulse mb-4" size={40} />
            <p className="text-white font-bold uppercase tracking-widest">{status}...</p>
          </div>
        )}

        {status === 'error' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-red-950/20 backdrop-blur-md">
            <AlertCircle className="text-red-500 mb-4" size={40} />
            <p className="text-white font-bold">RELAY ERROR</p>
            <p className="text-red-400 text-xs mt-2 px-8 text-center">{error}</p>
          </div>
        )}

        <div className="absolute top-6 left-6 flex gap-3">
          <div className="bg-black/60 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full ${status === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase tracking-widest text-white">
              {status === 'connected' ? 'Relay Active' : 'Offline'}
            </span>
          </div>
        </div>
      </div>

      <div className="p-6 bg-zinc-950 border-t border-white/5 flex justify-between">
         <div>
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Frames Received</p>
            <p className="text-lg font-black text-white">{frameCount}</p>
         </div>
         <div className="text-right">
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Protocol</p>
            <p className="text-[10px] font-bold text-primary">WebRTC / DO</p>
         </div>
      </div>
    </div>
  )
}
