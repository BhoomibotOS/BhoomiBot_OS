"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, AlertCircle, PlayCircle, ShieldAlert, RefreshCw, Camera } from 'lucide-react'

export function LiveConsole() {
  const [status, setStatus] = useState('offline')
  const [frameCount, setFrameCount] = useState(0)
  const [isRobotOnline, setIsRobotOnline] = useState(false)
  const [isCameraActive, setIsCameraActive] = useState(false)
  const [useRearCamera, setUseRearCamera] = useState(true)

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
        setIsCameraActive(true)
        renderFrame(event.data)
        return
      }

      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'PEER_STATUS') {
          const peers = JSON.parse(msg.payload)
          setIsRobotOnline(peers.robot)
        }
        if (msg.type === 'VIDEO_FRAME' && msg.payload) {
          setFrameCount(prev => prev + 1)
          setIsCameraActive(true)
          const byteCharacters = atob(msg.payload)
          const byteArray = new Uint8Array(byteCharacters.length)
          for (let i = 0; i < byteCharacters.length; i++) byteArray[i] = byteCharacters.charCodeAt(i)
          renderFrame(new Blob([byteArray], {type: 'image/jpeg'}))
        }
      } catch(e) {}
    }

    ws.onerror = () => setStatus('error')
    ws.onclose = () => {
        setStatus('offline')
        setIsRobotOnline(false)
        setIsCameraActive(false)
    }
  }

  const sendCommand = (live: boolean, rear: boolean) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({
        type: "COMMAND",
        robotId: "BHOOMI-001",
        ts: Date.now(),
        payload: JSON.stringify({ liveCamera: live, useRearCamera: rear })
      }))
    }
  }

  const toggleCamera = () => {
    const nextState = !isCameraActive
    setIsCameraActive(nextState)
    sendCommand(nextState, useRearCamera)
  }

  const flipCamera = () => {
    const nextRear = !useRearCamera
    setUseRearCamera(nextRear)
    sendCommand(isCameraActive, nextRear)
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
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        URL.revokeObjectURL(url);
    }
    img.src = url
  }

  return (
    <div className="bg-slate-900 rounded-[3rem] border border-white/10 overflow-hidden flex flex-col shadow-2xl h-[600px] relative">
      <div className="relative flex-1 bg-black flex items-center justify-center">
        <canvas ref={canvasRef} width={1280} height={720} className="w-full h-full object-contain" />

        {status !== 'connected' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80">
            <button onClick={connect} className="bg-primary text-black px-8 py-4 rounded-2xl font-black uppercase tracking-tighter hover:scale-105 transition-transform">
                Establish Operator Link
            </button>
          </div>
        )}

        {status === 'connected' && !isRobotOnline && (
           <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm">
             <ShieldAlert className="text-yellow-500 mb-4 animate-pulse" size={48} />
             <p className="text-white font-bold text-sm uppercase tracking-widest">Waiting for Robot to Join...</p>
             <p className="text-slate-500 text-[10px] mt-2 font-mono uppercase tracking-widest">Target: BHOOMI-001</p>
           </div>
        )}

        {isRobotOnline && !isCameraActive && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-[2px]">
                 <Camera className="text-primary mb-4" size={48} />
                 <p className="text-white font-bold text-sm uppercase tracking-widest">Camera Standby</p>
                 <button
                    onClick={toggleCamera}
                    className="mt-6 bg-white text-black px-6 py-3 rounded-xl font-bold text-xs uppercase"
                 >
                    Activate Robot Camera
                 </button>
            </div>
        )}

        <div className="absolute top-6 left-6 flex flex-col gap-3">
          <div className="bg-black/60 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full ${status === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase tracking-widest text-white">Relay {status}</span>
          </div>
          {isRobotOnline && (
            <div className="bg-primary/20 border border-primary/50 px-4 py-2 rounded-full backdrop-blur-md flex items-center gap-3">
              <div className="w-2 h-2 rounded-full bg-primary animate-ping" />
              <span className="text-[10px] font-black uppercase tracking-widest text-primary">Robot Online</span>
            </div>
          )}
        </div>

        {/* Floating Controls */}
        {isRobotOnline && (
            <div className="absolute bottom-6 right-6 flex gap-2">
                <button
                    onClick={flipCamera}
                    className="bg-black/80 hover:bg-primary hover:text-black border border-white/10 p-4 rounded-2xl transition-all"
                    title="Flip Camera"
                >
                    <RefreshCw size={20} />
                </button>
                <button
                    onClick={toggleCamera}
                    className={`p-4 rounded-2xl border transition-all ${isCameraActive ? 'bg-red-500/20 border-red-500 text-red-500' : 'bg-primary/20 border-primary text-primary'}`}
                    title={isCameraActive ? "Stop Camera" : "Start Camera"}
                >
                    <Video size={20} />
                </button>
            </div>
        )}
      </div>

      <div className="p-6 bg-zinc-950 border-t border-white/5 flex justify-between items-center text-white">
         <div className="flex gap-8">
             <div>
                <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Frames</p>
                <p className="text-lg font-black font-mono">{frameCount}</p>
             </div>
             <div>
                <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Session</p>
                <p className="text-lg font-black font-mono">123</p>
             </div>
         </div>
         <div className="text-right">
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Engine</p>
            <p className="text-[10px] font-bold text-primary italic">CloudBridge DO v13</p>
         </div>
      </div>
    </div>
  )
}
