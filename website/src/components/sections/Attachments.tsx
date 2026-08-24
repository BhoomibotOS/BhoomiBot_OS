"use client"

import * as React from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Button } from "@/components/ui/button"
import dynamic from "next/dynamic"

const RobotCanvas = dynamic(() => import("@/components/RobotCanvas"), {
  ssr: false,
  loading: () => <div className="w-full h-[400px] bg-muted/20 animate-pulse rounded-3xl" />
})

const attachments = [
  {
    id: "none",
    name: "Standard Base",
    purpose: "The core electric platform for mobility and sensors.",
    info: "4WD Differential Drive, IP65 rated electronics."
  },
  {
    id: "plough",
    name: "Primary Plough",
    purpose: "Breaking and turning soil for cultivation.",
    info: "Adjustable depth control, heavy-duty steel construction."
  },
  {
    id: "sprayer",
    name: "Precision Sprayer",
    purpose: "Automated liquid application for crops.",
    info: "Dual nozzle system, 20L tank capacity support."
  },
  {
    id: "cargo",
    name: "Cargo Platform",
    purpose: "Transporting crates, tools, and harvest.",
    info: "Flat-bed design, up to 50kg payload support."
  }
]

export function Attachments() {
  const [activeId, setActiveId] = React.useState("none")
  const active = attachments.find(a => a.id === activeId) || attachments[0]

  return (
    <section id="applications" className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <div className="order-2 lg:order-1">
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">One Robot. Multiple Attachments.</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg mb-10">
              The BhoomiBot platform is designed to be truly modular. Swap tools in minutes to adapt to different field requirements throughout the season.
            </p>

            <div className="space-y-4 mb-10">
              {attachments.map((a) => (
                <button
                  key={a.id}
                  onClick={() => setActiveId(a.id)}
                  className={`w-full text-left p-6 rounded-2xl border transition-all duration-300 ${
                    activeId === a.id
                    ? "bg-primary/5 border-primary shadow-lg shadow-primary/5"
                    : "bg-transparent border-slate-100 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-600"
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <span className={`font-bold text-lg ${activeId === a.id ? "text-primary" : ""}`}>
                      {a.name}
                    </span>
                    {activeId === a.id && (
                      <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                    )}
                  </div>
                </button>
              ))}
            </div>

            <AnimatePresence mode="wait">
              <motion.div
                key={activeId}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="p-8 rounded-3xl bg-slate-50 dark:bg-zinc-900 border border-slate-200 dark:border-slate-800"
              >
                <h3 className="font-bold text-slate-900 dark:text-white mb-2 uppercase tracking-tighter text-sm">Application</h3>
                <p className="text-slate-600 dark:text-slate-400 mb-6">{active.purpose}</p>

                <h3 className="font-bold text-slate-900 dark:text-white mb-2 uppercase tracking-tighter text-sm">Capabilities</h3>
                <p className="text-slate-600 dark:text-slate-400">{active.info}</p>
              </motion.div>
            </AnimatePresence>
          </div>

          <div className="order-1 lg:order-2 relative h-[400px] lg:h-[600px] bg-slate-50 dark:bg-zinc-900 rounded-[3rem] border border-slate-200 dark:border-slate-800 p-8">
             <RobotCanvas attachment={activeId} />
             <div className="absolute bottom-8 right-8 text-right hidden md:block">
               <p className="text-[100px] font-black text-slate-200 dark:text-slate-800 leading-none select-none">
                 0{attachments.findIndex(a => a.id === activeId) + 1}
               </p>
             </div>
          </div>
        </div>
      </div>
    </section>
  )
}
