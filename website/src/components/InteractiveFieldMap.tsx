"use client"

import * as React from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Map as MapIcon, Navigation, Target, Trash2, Save, Layers, MousePointer2 } from "lucide-react"

interface Point {
  x: number
  y: number
}

export function InteractiveFieldMap() {
  const [points, setPoints] = React.useState<Point[]>([])
  const [isDrawing, setIsDrawing] = React.useState(false)
  const [missionStatus, setMissionStatus] = React.useState<"idle" | "planning" | "ready">("idle")

  const handleMapClick = (e: React.MouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    setPoints([...points, { x, y }])
    setMissionStatus("planning")
  }

  const clearMap = () => {
    setPoints([])
    setMissionStatus("idle")
  }

  const generatePath = () => {
    if (points.length > 2) {
      setMissionStatus("ready")
    }
  }

  return (
    <div className="w-full bg-white dark:bg-zinc-900 rounded-[3rem] border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden flex flex-col h-[600px]">
      {/* Map Header */}
      <div className="p-6 border-b border-slate-100 dark:border-slate-800 flex justify-between items-center bg-slate-50/50 dark:bg-zinc-900/50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <MapIcon size={20} />
          </div>
          <div>
            <h4 className="font-bold text-sm">Mission Planner</h4>
            <p className="text-[10px] text-slate-500 font-black uppercase tracking-widest">Interactive Field Mapping</p>
          </div>
        </div>

        <div className="flex gap-2">
          <button
            onClick={clearMap}
            className="p-2 rounded-lg hover:bg-red-500/10 text-slate-400 hover:text-red-500 transition-colors"
          >
            <Trash2 size={18} />
          </button>
          <button className="p-2 rounded-lg hover:bg-primary/10 text-slate-400 hover:text-primary transition-colors">
            <Layers size={18} />
          </button>
        </div>
      </div>

      {/* Main Map Area */}
      <div className="flex-1 relative bg-green-900/5 dark:bg-green-950/10 cursor-crosshair overflow-hidden">
        {/* Grid Overlay */}
        <div className="absolute inset-0 opacity-[0.03] pointer-events-none"
             style={{ backgroundImage: 'radial-gradient(circle, currentColor 1px, transparent 1px)', backgroundSize: '30px 30px' }}
        />

        <svg
          className="w-full h-full"
          onClick={handleMapClick}
        >
          {/* Farm Boundary / Rows */}
          <pattern id="rows" x="0" y="0" width="40" height="40" patternUnits="userSpaceOnUse">
             <line x1="0" y1="20" x2="40" y2="20" stroke="currentColor" strokeWidth="0.5" className="text-green-600/20" />
          </pattern>
          <rect width="100%" height="100%" fill="url(#rows)" />

          {/* Area Polygon */}
          {points.length > 2 && (
            <motion.path
              d={`M ${points.map(p => `${p.x},${p.y}`).join(' L ')} Z`}
              fill="rgba(16, 185, 129, 0.1)"
              stroke="#10b981"
              strokeWidth="2"
              strokeDasharray="5,5"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            />
          )}

          {/* Points */}
          {points.map((p, i) => (
            <motion.g
              key={i}
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
            >
              <circle cx={p.x} cy={p.y} r="4" fill="#10b981" />
              <circle cx={p.x} cy={p.y} r="8" stroke="#10b981" strokeWidth="1" fill="none" className="animate-ping" />
            </motion.g>
          ))}

          {/* Scanning lines animation if ready */}
          {missionStatus === "ready" && (
            <motion.g initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
               {[...Array(10)].map((_, i) => (
                 <motion.line
                   key={i}
                   x1="0" y1={i * 60} x2="100%" y2={i * 60}
                   stroke="#10b981" strokeWidth="0.5" strokeOpacity="0.3"
                   animate={{ x1: ["-100%", "100%"] }}
                   transition={{ duration: 3, repeat: Infinity, delay: i * 0.2 }}
                 />
               ))}
            </motion.g>
          )}
        </svg>

        {/* Floating Instruction */}
        {points.length === 0 && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
             <div className="bg-slate-900/80 backdrop-blur-md px-6 py-4 rounded-2xl text-white text-center shadow-2xl border border-white/10">
                <MousePointer2 className="mx-auto mb-3 text-primary animate-bounce" />
                <p className="text-sm font-bold">Click to define field boundaries</p>
                <p className="text-[10px] text-slate-400 mt-1 uppercase tracking-widest font-black">Minimum 3 points required</p>
             </div>
          </div>
        )}
      </div>

      {/* Map Footer Controls */}
      <div className="p-6 bg-slate-50 dark:bg-zinc-900/80 border-t border-slate-100 dark:border-slate-800 flex justify-between items-center">
        <div className="flex gap-8">
           <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Total Points</p>
              <p className="text-xl font-black">{points.length}</p>
           </div>
           <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Est. Area</p>
              <p className="text-xl font-black">{points.length > 2 ? (points.length * 0.45).toFixed(2) : "0.00"} Acres</p>
           </div>
        </div>

        <div className="flex gap-3">
          {points.length > 2 && missionStatus !== "ready" && (
            <button
              onClick={generatePath}
              className="px-6 py-3 rounded-xl bg-primary text-black font-black text-xs uppercase tracking-widest shadow-lg shadow-primary/20 hover:scale-[1.05] transition-transform flex items-center gap-2"
            >
              <Navigation size={14} /> Calculate Path
            </button>
          )}
          {missionStatus === "ready" && (
            <button className="px-6 py-3 rounded-xl bg-slate-900 text-white font-black text-xs uppercase tracking-widest shadow-xl hover:bg-slate-800 transition-all flex items-center gap-2">
              <Save size={14} /> Export to Robot
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
