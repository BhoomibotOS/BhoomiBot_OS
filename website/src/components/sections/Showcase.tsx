"use client"

import React, { useState } from "react"
import { motion } from "framer-motion"
import RobotCanvas from "@/components/RobotCanvas"
import { AttachmentSelector, AttachmentType } from "@/components/AttachmentSelector"

export function Showcase() {
  const [attachment, setAttachment] = useState<AttachmentType>("none")

  return (
    <section id="showcase" className="py-24 bg-slate-50 dark:bg-zinc-950 overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Interactive Showcase
          </div>
          <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">Configure Your BhoomiBot</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl text-lg">
            Select different modules to see how BhoomiBot adapts to various agricultural and industrial tasks.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
          {/* 3D Visualizer */}
          <div className="lg:col-span-8 bg-white dark:bg-zinc-900 rounded-[3rem] border border-slate-200 dark:border-slate-800 shadow-2xl overflow-hidden relative">
            <RobotCanvas attachment={attachment} />

            <div className="absolute bottom-8 left-8 right-8 pointer-events-none">
              <div className="flex justify-between items-end">
                 <div className="bg-slate-900/90 backdrop-blur-md border border-white/10 p-6 rounded-3xl text-white shadow-2xl">
                    <p className="text-[10px] font-black uppercase tracking-widest text-slate-400 mb-1">Active Configuration</p>
                    <h4 className="text-xl font-bold text-primary italic uppercase tracking-tighter">
                      {attachment === "none" ? "Standard Chassis" :
                       attachment === "plough" ? "Weed Removal Unit" :
                       attachment === "sprayer" ? "Smart Spraying System" : "Cargo Logistics Hub"}
                    </h4>
                 </div>
              </div>
            </div>
          </div>

          {/* Configuration Panel */}
          <div className="lg:col-span-4">
            <AttachmentSelector
              activeAttachment={attachment}
              onSelect={setAttachment}
            />
          </div>
        </div>
      </div>
    </section>
  )
}
