"use client"

import { motion } from "framer-motion"
import { ShieldCheck, Cpu, Database, Eye, MessageSquare, Activity } from "lucide-react"

const techCards = [
  {
    title: "Control System",
    desc: "Robust ESP32-based embedded architecture for reliable low-latency execution.",
    icon: Cpu
  },
  {
    title: "Motor Control",
    desc: "Independent dual-motor drive system for precise differential steering.",
    icon: Activity
  },
  {
    title: "Sensors",
    desc: "Integrated feedback loop for real-time monitoring of environment and robot status.",
    icon: Database
  },
  {
    title: "Vision",
    desc: "Camera-based computer vision support for agricultural monitoring and navigation.",
    icon: Eye
  },
  {
    title: "Communication",
    desc: "Dual-layer local and cloud-based communication for seamless remote operation.",
    icon: MessageSquare
  },
  {
    title: "Safety",
    desc: "Hardware-level emergency braking and intelligent fault-handling mechanisms.",
    icon: ShieldCheck
  }
]

export function Technology() {
  return (
    <section id="technology" className="py-24 bg-white dark:bg-black">
      <div className="container mx-auto px-4">
        <div className="flex flex-col md:flex-row justify-between items-end mb-16 gap-6">
          <div className="max-w-2xl">
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-4">The Tech Behind BhoomiBot</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg">
              Engineering a reliable robotic future requires a robust foundation of hardware and software.
            </p>
          </div>
          <div className="text-primary font-black text-xl italic tracking-tighter uppercase">
            BhoomiBot OS v1.0
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {techCards.map((tech, index) => (
            <motion.div
              key={tech.title}
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="p-8 rounded-3xl border border-slate-100 dark:border-slate-800 bg-slate-50 dark:bg-zinc-900/50 hover:shadow-2xl hover:shadow-primary/5 transition-all"
            >
              <div className="w-12 h-12 rounded-xl bg-white dark:bg-black border border-slate-200 dark:border-slate-800 flex items-center justify-center mb-6 text-primary shadow-sm">
                <tech.icon size={24} />
              </div>
              <h3 className="text-xl font-bold mb-3">{tech.title}</h3>
              <p className="text-slate-500 dark:text-slate-400 leading-relaxed text-sm">
                {tech.desc}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
