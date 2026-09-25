"use client"

import React, { useState, useEffect } from "react"
import { createPortal } from "react-dom"
import Image from "next/image"
import { motion, AnimatePresence } from "framer-motion"
import { Zap, Cpu, Settings, Shield, Maximize2, Minimize2 } from "lucide-react"

interface HotspotProps {
  x: string
  y: string
  title: string
  description: string
  icon: React.ElementType
}

interface RobotCanvasProps {
  attachment?: string
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
          className={`w-7 h-7 sm:w-8 sm:h-8 rounded-full flex items-center justify-center border-2 transition-all duration-300 shadow-lg backdrop-blur-md ${
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
              className="absolute bottom-10 left-1/2 -translate-x-1/2 w-56 sm:w-64 bg-slate-950/95 text-white p-3.5 rounded-2xl border border-white/15 shadow-2xl z-50 backdrop-blur-xl"
            >
              <div className="flex items-center gap-2 mb-1.5">
                <div className="p-1 rounded bg-primary/20 text-primary">
                  <Icon size={12} />
                </div>
                <p className="text-[11px] font-black uppercase text-primary tracking-widest">{title}</p>
              </div>
              <p className="text-[11px] sm:text-xs leading-relaxed text-slate-300">{description}</p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

export default function RobotCanvas({ attachment }: RobotCanvasProps = {}) {
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && isFullscreen) {
        setIsFullscreen(false)
      }
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [isFullscreen])

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

  const canvasContent = (
    <div className="relative w-full h-full flex items-center justify-center p-4 sm:p-6">
      {/* Background Grid & Radial Lighting Glow */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(34,197,94,0.15)_0,transparent_70%)] pointer-events-none" />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1f293710_1px,transparent_1px),linear-gradient(to_bottom,#1f293710_1px,transparent_1px)] bg-[size:2rem_2rem] pointer-events-none" />

      {/* Main Digital Twin Image Display */}
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

      {/* Header Badge */}
      <div className="absolute top-4 left-4 sm:top-6 sm:left-6 z-30">
        <div className="bg-black/80 backdrop-blur-md border border-white/10 px-3 py-1.5 sm:px-4 sm:py-2 rounded-full flex items-center gap-2.5 sm:gap-3 shadow-lg">
          <div className="w-2 h-2 sm:w-2.5 sm:h-2.5 rounded-full bg-primary animate-ping" />
          <span className="text-[10px] sm:text-[11px] font-black uppercase tracking-widest text-white">v2.4 Digital Twin</span>
        </div>
      </div>

      {/* Fullscreen Toggle Button */}
      <div className="absolute top-4 right-4 sm:top-6 sm:right-6 z-30 flex items-center gap-3">
        <button
          onClick={() => setIsFullscreen(!isFullscreen)}
          className="bg-black/80 backdrop-blur-md border border-white/10 p-2.5 rounded-full text-white hover:text-primary transition-colors shadow-lg"
          title={isFullscreen ? "Exit Fullscreen" : "Toggle Fullscreen"}
        >
          {isFullscreen ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
        </button>
      </div>
    </div>
  )

  return (
    <>
      <div className="relative w-full h-full min-h-[300px] sm:min-h-[400px] overflow-hidden rounded-2xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 bg-slate-950 transition-all duration-500 shadow-2xl">
        {canvasContent}
      </div>

      {isFullscreen && mounted && createPortal(
        <div className="fixed inset-0 z-[9999] bg-black/95 backdrop-blur-2xl p-2 sm:p-6 flex items-center justify-center animate-in fade-in duration-300">
          <div className="relative w-full h-full max-w-[1800px] max-h-[1000px] overflow-hidden rounded-3xl border border-white/15 bg-slate-950 shadow-2xl">
            {canvasContent}
          </div>
        </div>,
        document.body
      )}
    </>
  )
}
