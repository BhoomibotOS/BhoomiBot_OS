"use client"

import * as React from "react"
import { motion } from "framer-motion"
import { InteractiveFieldMap } from "@/components/InteractiveFieldMap"
import { Target, Zap, Shield, Smartphone, Globe } from "lucide-react"

export function MissionPlanner() {
  return (
    <section id="planner" className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-16 items-start">
          <div className="lg:col-span-5">
            <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-6">
              Path Planning
            </div>
            <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">Plan Missions in Seconds</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg mb-12 leading-relaxed">
              Our intuitive mapping interface allows you to define field boundaries, select operations, and generate optimized autonomous paths that are sent directly to your BhoomiBot.
            </p>

            <div className="space-y-8">
              {[
                {
                  icon: Target,
                  title: "Precision Boundaries",
                  desc: "Define exactly where the robot should work with GPS-linked boundary markers."
                },
                {
                  icon: Zap,
                  title: "Optimized Routing",
                  desc: "AI-driven path generation reduces energy consumption and soil compaction."
                },
                {
                  icon: Smartphone,
                  title: "Mobile Sync",
                  desc: "Seamlessly sync mission plans between your desktop and the BhoomiBot mobile app."
                }
              ].map((item, i) => (
                <div key={i} className="flex gap-6 group">
                  <div className="w-14 h-14 rounded-2xl bg-slate-50 dark:bg-zinc-900 border border-slate-100 dark:border-slate-800 flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                    <item.icon size={24} />
                  </div>
                  <div>
                    <h4 className="text-xl font-bold mb-1 tracking-tight">{item.title}</h4>
                    <p className="text-slate-500 text-sm leading-relaxed">{item.desc}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-16 p-8 rounded-[2.5rem] bg-slate-900 text-white shadow-2xl relative overflow-hidden">
               <div className="relative z-10">
                  <div className="flex items-center gap-2 text-primary mb-4">
                     <Globe size={18} />
                     <span className="text-xs font-bold uppercase tracking-widest">Global Operations</span>
                  </div>
                  <p className="text-lg font-bold italic mb-2">"Zero Manual Intervention"</p>
                  <p className="text-xs text-slate-400">Once the mission is uploaded, BhoomiBot handles the rest—navigating terrain, avoiding obstacles, and completing tasks autonomously.</p>
               </div>
               <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 blur-3xl rounded-full" />
            </div>
          </div>

          <div className="lg:col-span-7">
             <InteractiveFieldMap />
          </div>
        </div>
      </div>
    </section>
  )
}
