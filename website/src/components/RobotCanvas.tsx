"use client"

import React, { useState } from "react"
import Image from "next/image"
import { motion, AnimatePresence } from "framer-motion"
import { Zap, Cpu, Settings, Shield, Maximize2, Minimize2, Eye, Radio } from "lucide-react"

interface HotspotProps {
  x: string
  y: string
  title: string
  description: string
  icon: React.ElementType
}

function Hotspot({ x, y, title, description, icon: Icon }: HotspotProps) {
  const [active, setActive] = useState(false)

  return (
    <div className="absolute z-20" style={{ left: x, top: y }}>
      <div className="relative group">
        <motion.button
          whileHover={{ scale: 1.2 }}
          whileTap={{ scale: 0.9 }}
          onClick={() => setActive(!active)}
          className={`w-7 h-7 rounded-full flex items-center justify-center border-2 transition-all duration-300 shadow-lg backdrop-blur-md ${
            active
              ? "bg-primary border-primary text-black shadow-primary/50"
              : "bg-black/70 border-white/40 text-white hover:border-primary hover:text-primary"
          }`}
        >
          <Icon size={14} />
        </motion.button>

        <AnimatePresence>
          {active && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.8, y: 10 }}
              className="absolute bottom-10 left-1/2 -translate-x-1/2 w-56 bg-slate-950/95 text-white p-3.5 rounded-2xl border border-white/15 shadow-2xl z-50 backdrop-blur-xl"
            >
              <div className="flex items-center gap-2 mb-1.5">
                <div className="p-1 rounded bg-primary/20 text-primary">
                  <Icon size={12} />
                </div>
                <p className="text-[11px] font-black uppercase text-primary tracking-widest">{title}</p>
              </div>
              <p className="text-[11px] leading-relaxed text-slate-300">{description}</p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default function RobotCanvas() {
  const [isFullscreen, setIsFullscreen] = useState(false)

  const hotspots = [
    {
      x: "52%",
      y: "40%",
      title: "LiFePO4 Energy Core",
      description: "48V 50Ah LiFePO4 battery pack with integrated smart BMS for continuous field operations.",
      icon: Zap
    },
    {
      x: "35%",
      y: "55%",
      title: "Dual Drive Motors",
      description: "High-torque brushless DC motors enabling differential steering across rough farm terrain.",
      icon: Settings
    },
    {
      x: "50%",
      y: "22%",
      title: "Bhoomi Vision AI",
      description: "Real-time row following, crop health monitoring and autonomous obstacle avoidance.",
      icon: Cpu
    },
    {
      x: "70%",
      y: "60%",
      title: "Heavy Chassis",
      description: "Reinforced steel structural frame rated for rugged agricultural and logistics operations.",
      icon: Shield
    }
  ]

  return (
    <div
      className={`relative overflow-hidden rounded-3xl border border-slate-200 dark:border-slate-800 bg-slate-950 transition-all duration-500 shadow-2xl ${
        isFullscreen ? "fixed inset-4 z-[100] h-[calc(100vh-2rem)]" : "w-full h-[400px] md:h-[600px]"
      }`}
    >
      {/* Background Grid & Radial Lighting Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(34,197,94,0.15)_0,transparent_70%)] pointer-events-none" />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1f293710_1px,transparent_1px),linear-gradient(to_bottom,#1f293710_1px,transparent_1px)] bg-[size:2rem_2rem] pointer-events-none" />

      {/* Main Digital Twin Image Display */}
      <div className="relative w-full h-full flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8 }}
          className="relative w-full h-full flex items-center justify-center"
        >
          <Image
            src="/robots/v2.4-twin.png"
            alt="BhoomiBot v2.4 Digital Twin"
            fill
            priority
            className="object-contain drop-shadow-[0_20px_50px_rgba(34,197,94,0.2)] rounded-2xl"
          />

          {/* Interactive Hotspots */}
          {hotspots.map((spot) => (
            <Hotspot key={spot.title} {...spot} />
          ))}
        </motion.div>
      </div>

      {/* Header Badge */}
      <div className="absolute top-6 left-6 z-30">
        <div className="bg-black/80 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3 shadow-lg">
          <div className="w-2.5 h-2.5 rounded-full bg-primary animate-ping" />
          <span className="text-[11px] font-black uppercase tracking-widest text-white">v2.4 Digital Twin</span>
        </div>
      </div>

      {/* Live Status Telemetry Badge */}
      <div className="absolute top-6 right-6 z-30 hidden sm:flex items-center gap-3">
        <div className="bg-black/80 backdrop-blur-md border border-white/10 px-3 py-1.5 rounded-full flex items-center gap-2 text-slate-300 text-[10px] font-mono">
          <Radio size={12} className="text-primary animate-pulse" />
          <span>LIVE LINK ACTIVE</span>
        </div>
        <button
          onClick={() => setIsFullscreen(!isFullscreen)}
          className="bg-black/80 backdrop-blur-md border border-white/10 p-2 rounded-full text-white hover:text-primary transition-colors"
          title="Toggle Fullscreen"
        >
          {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
        </button>
      </div>

      {/* Footer Hotspot Hint */}
      <div className="absolute bottom-6 left-6 z-30 text-[10px] text-slate-400 font-mono uppercase tracking-[0.2em] flex items-center gap-2 bg-black/60 px-3 py-1.5 rounded-lg border border-white/5 backdrop-blur-sm">
        <Eye size={12} className="text-primary" />
        <span>Click icons on chassis to view twin telemetry</span>
      </div>
    </div>
  )
}
