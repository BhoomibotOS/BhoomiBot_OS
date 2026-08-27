"use client"

import React, { useEffect, useRef, useState } from 'react'
import { Video, Activity, Wifi, AlertCircle, PlayCircle, ShieldAlert, Cpu } from 'lucide-react'

export function LiveConsole({ robotId, sessionId }: any) {
  const [status, setStatus] = useState('offline')
  const [frameCount, setFrameCount] = useState(0)
  const [isRobotOnline, setIsRobotOnline] = useState(false)

  // HARDCODED OVERRIDE FOR TESTING
  const TARGET_ROBOT = "BHOOMI-001"
  const TARGET_SESSION = "123"

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const socketRef = useRef<WebSocket | null>(null)

  const connect = () => {
    if (socketRef.current) socketRef.current.close()

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}/api/relay?robotId=${TARGET_ROBOT}`

      setStatus('connecting')
      const ws = new WebSocket(wsUrl)
      socketRef.current = ws

      ws.onopen = () => {
        const hello = {
          type: "HELLO",
          robotId: TARGET_ROBOT,
          ts: Date.now(),
          payload: JSON.stringify({ role: "OPERATOR", session: TARGET_SESSION })
        }
        ws.send(JSON.stringify(hello))
        setStatus('connected')
      }

      ws.onmessage = async (event) => {
        // 1. Handle Raw Binary Frames (Hotspot mode)
        if (event.data instanceof Blob || event.data instanceof ArrayBuffer) {
          setFrameCount(prev => prev + 1)
          renderFrame(event.data)
          return
        }

        // 2. Handle JSON Messages (Internet mode)
        try {
          const msg = JSON.parse(event.data)

          // PEER STATUS
          if (msg.type === 'PEER_STATUS') {
            const peers = JSON.parse(msg.payload)
            setIsRobotOnline(peers.robot)

            // If robot just came online, command it to start its camera
            if (peers.robot) {
               const startCameraCmd = JSON.stringify({
                 type: "COMMAND",
                 robotId: TARGET_ROBOT,
                 ts: Date.now(),
                 payload: JSON.stringify({ liveCamera: true, useRearCamera: true })
               });
               socketRef.current?.send(startCameraCmd);
            }
            return
          }

          // VIDEO FRAME (Base64)
          if (msg.type === 'VIDEO_FRAME' && msg.payload) {
            setFrameCount(prev => prev + 1)
            const byteCharacters = atob(msg.payload);
            const byteNumbers = new Array(byteCharacters.length);
            for (let i = 0; i < byteCharacters.length; i++) {
                byteNumbers[i] = byteCharacters.charCodeAt(i);
            }
            const byteArray = new Uint8Array(byteNumbers);
            renderFrame(new Blob([byteArray], {type: 'image/jpeg'}));
          }
        } catch(e) {
          console.error("Parse Error:", e)
        }
      }

      ws.onerror = () => setStatus('error')
      ws.onclose = () => {
        setStatus('offline')
        setIsRobotOnline(false)
      }
    } catch (e: any) {
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
            <p className="text-slate-500 text-[10px] mt-4 font-mono uppercase">TEST MODE: {TARGET_ROBOT}</p>
          </div>
        )}

        {status === 'connecting' && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60">
            <Wifi className="text-primary animate-pulse mb-4" size={40} />
            <p className="text-white font-bold uppercase tracking-widest text-sm">Connecting...</p>
          </div>
        )}

        {!isRobotOnline && status === 'connected' && (
           <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 backdrop-blur-sm">
             <ShieldAlert className="text-yellow-500 mb-4 animate-pulse" size={40} />
             <p className="text-white font-bold uppercase tracking-widest text-sm">Waiting for Robot...</p>
           </div>
        )}

        <div className="absolute top-6 left-6">
          <div className="bg-black/60 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full ${status === 'connected' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-[10px] font-black uppercase tracking-widest text-white">
              {status === 'connected' ? 'Relay Active' : 'Offline'}
            </span>
          </div>
        </div>
      </div>

      <div className="p-4 bg-black border-t border-white/5 flex justify-between items-center">
         <div>
            <p className="text-[8px] font-black uppercase text-slate-500 mb-1">Frames Rx</p>
            <p className="text-lg font-black text-white font-mono">{frameCount}</p>
         </div>
      </div>
    </div>
  )
}
