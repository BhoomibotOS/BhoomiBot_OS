"use client"

import { motion } from "framer-motion"
import { Shield, AlertTriangle, Radio, Activity, Eye, Zap } from "lucide-react"
import Image from "next/image"

const safetyFeatures = [
  { title: "Emergency Braking", icon: Zap, desc: "Immediate motor stop upon detection of critical faults." },
  { title: "Control Monitoring", icon: Activity, desc: "Continuous heartbeat monitoring between app and robot." },
  { title: "Communication Watchdog", icon: Radio, desc: "Automatic safe-state entry if communication is lost." },
  { title: "Fault Handling", icon: AlertTriangle, desc: "Intelligent error detection and diagnostic reporting." },
  { title: "Operator Override", icon: Shield, desc: "Physical and remote emergency stop capabilities." },
  { title: "Obstacle Detection", icon: Eye, desc: "Sensor-based proximity alerts for safe navigation." },
]

export function Safety() {
  return (
    <section className="py-24 bg-slate-900 text-white overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <div>
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Designed With Safety in Mind</h2>
            <p className="text-slate-400 text-lg mb-12">
              BhoomiBot is built on a "Safety First" architecture, ensuring that both the operator and the environment are protected during all stages of operation.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-8">
              {safetyFeatures.map((feature, i) => (
                <div key={i} className="flex gap-4">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0 text-primary">
                    <feature.icon size={24} />
                  </div>
                  <div>
                    <h3 className="font-bold mb-1">{feature.title}</h3>
                    <p className="text-xs text-slate-500 leading-relaxed">{feature.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="relative"
          >
            <div className="rounded-[3rem] overflow-hidden border-8 border-slate-800 shadow-2xl relative">
              <Image
                src="/robots/safety-systems.jpg"
                alt="Safety Systems"
                width={800}
                height={600}
                className="w-full h-[500px] object-cover opacity-50"
              />
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="text-center">
                  <div className="w-20 h-20 rounded-full bg-primary/20 flex items-center justify-center mx-auto mb-6 border-2 border-primary animate-pulse">
                    <Shield size={40} className="text-primary" />
                  </div>
                  <p className="text-2xl font-black tracking-tighter uppercase italic">Safety Active</p>
                </div>
              </div>
            </div>

            {/* HUD element */}
            <div className="absolute -bottom-6 -right-6 bg-slate-800 border border-white/10 p-6 rounded-3xl shadow-2xl z-10 max-w-[240px]">
               <div className="flex items-center gap-2 mb-4">
                 <div className="w-2 h-2 rounded-full bg-green-500" />
                 <span className="text-[10px] font-black uppercase tracking-wider text-slate-400">All Systems Normal</span>
               </div>
               <div className="space-y-2">
                  <div className="h-1 bg-white/5 rounded-full overflow-hidden">
                    <motion.div className="h-full bg-primary" initial={{ width: 0 }} whileInView={{ width: "90%" }} transition={{ duration: 1 }} />
                  </div>
                  <div className="flex justify-between text-[10px] font-bold text-slate-500 uppercase">
                    <span>Stability</span>
                    <span>98%</span>
                  </div>
               </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  )
}
