"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, AlertCircle, PlayCircle, ShieldAlert } from 'lucide-react'

export function LiveConsole() {
  const [status, setStatus] = useState('offline')
  const [frameCount, setFrameCount] = useState(0)
  const [isRobotOnline, setIsRobotOnline] = useState(false)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    if (socketRef.current) socketRef.current.close()
    const wsUrl = `wss://${window.location.host}/api/relay?robotId=BHOOMI-001`

    setStatus('connecting')
    const ws = new WebSocket(wsUrl)
    socketRef.current = ws

    ws.onopen = () => {
      ws.send(JSON.stringify({
        type: "HELLO",
        robotId: "BHOOMI-001",
        ts: Date.now(),
        payload: JSON.stringify({ role: "OPERATOR", session: "123" })
      }))
      setStatus('connected')
    }

    ws.onmessage = async (event) => {
      if (event.data instanceof Blob || event.data instanceof ArrayBuffer) {
        setFrameCount(prev => prev + 1)
        setIsRobotOnline(true) // If we get binary data, the robot IS online
        renderFrame(event.data)
        return
      }

      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'PEER_STATUS') {
          setIsRobotOnline(JSON.parse(msg.payload).robot)
        }
        if (msg.type === 'VIDEO_FRAME' && msg.payload) {
          setFrameCount(prev => prev + 1)
          setIsRobotOnline(true)
          const byteCharacters = atob(msg.payload)
          const byteArray = new Uint8Array(byteCharacters.length)
          for (let i = 0; i < byteCharacters.length; i++) byteArray[i] = byteCharacters.charCodeAt(i)
          renderFrame(new Blob([byteArray], {type: 'image/jpeg'}))
        }
      } catch(e) {}
    }

    ws.onerror = () => setStatus('error')
    ws.onclose = () => setStatus('offline')
  }

  const renderFrame = (data: any) => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const blob = data instanceof Blob ? data : new Blob([data], { type: 'image/jpeg' })
    const url = URL.createObjectURL(blob)
    const img = new Image()
    img.onload = () => { ctx.drawImage(img, 0, 0, canvas.width, canvas.height); URL.revokeObjectURL(url); }
    img.src = url
  }

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-[500px] relative">
      <div className="relative flex-1 bg-black flex items-center justify-center">
        <canvas ref={canvasRef} width={1280} height={720} className="w-full h-full object-contain" />

        {status !== 'connected' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80">
            <button onClick={connect} className="bg-primary text-black px-8 py-4 rounded-2xl font-black uppercase">Connect Link</button>
          </div>
        )}

        {!isRobotOnline && status === 'connected' && (
           <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60">
             <ShieldAlert className="text-yellow-500 mb-4 animate-pulse" size={40} />
             <p className="text-white font-bold text-sm uppercase">Robot Detected... Waiting for Stream</p>
           </div>
        )}

        <div className="absolute top-6 left-6 flex items-center gap-3 bg-black/60 px-4 py-2 rounded-full">
            <div className={`w-2 h-2 rounded-full ${isRobotOnline ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase text-white">{isRobotOnline ? 'Live' : 'Ready'}</span>
        </div>
      </div>
      <div className="p-4 bg-black border-t border-white/5 flex justify-between items-center text-white">
         <p className="text-[10px] font-mono">Frames Rx: {frameCount}</p>
         <p className="text-[10px] font-bold text-primary">Relay v12</p>
      </div>
    </div>
  )
}
