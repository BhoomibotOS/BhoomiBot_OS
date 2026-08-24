"use client"

import React, { useState } from "react"
import { motion } from "framer-motion"
import { Smartphone, Battery, Navigation, Eye, Play, Gauge, Wifi, RefreshCw } from "lucide-react"
import { LiveConsole } from "@/components/LiveConsole"

export function RemoteControl() {
  const [relayStatus, setRelayStatus] = useState<any>(null)
  const [isPinging, setIsPinging] = useState(false)
  const [robotId, setRobotId] = useState('BHOOMI-001')
  const [sessionId, setSessionId] = useState('ALPHA-1')

  const pingRelay = async () => {
    setIsPinging(true)
    try {
      const res = await fetch('/api/status')
      const data = await res.json()
      setRelayStatus(data)
    } catch (err) {
      setRelayStatus({ status: 'error' })
    }
    setIsPinging(false)
  }

  return (
    <section className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-center">
          <div className="lg:col-span-5">
            <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
              Live Operation
            </div>
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Real-time Tele-Operation</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg mb-10 leading-relaxed">
              Connect to your BhoomiBot's live video stream and control field operations from anywhere in the world through our high-performance Edge Relay.
            </p>

            <div className="space-y-6 mb-12">
               <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <p className="text-[10px] font-black uppercase text-slate-500 tracking-widest">Target Robot ID</p>
                    <input
                      value={robotId}
                      onChange={(e) => setRobotId(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 rounded-xl px-4 py-3 text-sm font-bold outline-none focus:border-primary"
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[10px] font-black uppercase text-slate-500 tracking-widest">Session Code</p>
                    <input
                      value={sessionId}
                      onChange={(e) => setSessionId(e.target.value)}
                      className="w-full bg-slate-50 dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 rounded-xl px-4 py-3 text-sm font-bold outline-none focus:border-primary"
                    />
                  </div>
               </div>
            </div>

            <div className="p-8 rounded-[2.5rem] bg-slate-50 dark:bg-zinc-900 border border-slate-200 dark:border-slate-800">
                <div className="flex items-center justify-between mb-4">
                    <h4 className="font-bold">Edge Relay Status</h4>
                    <div className={`px-2 py-1 rounded-full text-[8px] font-black uppercase tracking-widest ${
                      relayStatus?.status === 'online' ? 'bg-green-500/20 text-green-500' : 'bg-slate-200 text-slate-500'
                    }`}>
                      {relayStatus?.status || 'Not Tested'}
                    </div>
                </div>
                <button
                  onClick={pingRelay}
                  disabled={isPinging}
                  className="w-full flex items-center justify-center gap-2 px-6 py-4 rounded-xl bg-primary text-black font-black text-xs uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-50"
                >
                  {isPinging ? <RefreshCw size={14} className="animate-spin" /> : <Wifi size={14} />}
                  Ping Connectivity Bridge
                </button>
            </div>
          </div>

          <div className="lg:col-span-7 h-[600px]">
             <LiveConsole robotId={robotId} sessionId={sessionId} />
          </div>
        </div>
      </div>
    </section>
  )
}
