"use client"

import { motion } from "framer-motion"
import { Card } from "@/components/ui/card"
import { User, Cpu, Zap } from "lucide-react"

const modes = [
  {
    title: "Manual",
    desc: "The operator has direct, real-time control over every movement and function of the robot via the BhoomiBot application.",
    icon: User,
    capability: "Full control"
  },
  {
    title: "Semi-Autonomous",
    desc: "The operator defines the general operation while the robot assists with intelligent navigation, stability, and control.",
    icon: Cpu,
    capability: "Assisted operation"
  },
  {
    title: "Autonomous",
    desc: "The robot performs predefined missions with minimal intervention, utilizing onboard sensors for path following.",
    icon: Zap,
    capability: "Mission-based"
  }
]

export function OperatingModes() {
  return (
    <section className="py-24 bg-slate-900 text-white">
      <div className="container mx-auto px-4">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Operating Modes</h2>
          <p className="text-slate-400 text-lg">
            Flexibility for every task. Choose the level of autonomy that best suits your operational needs.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {modes.map((mode, index) => (
            <motion.div
              key={mode.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
            >
              <Card className="h-full p-8 bg-slate-800 border-white/5 hover:border-primary/50 transition-all duration-300 flex flex-col items-center text-center">
                <div className="w-16 h-16 rounded-2xl bg-primary/20 flex items-center justify-center mb-6 text-primary">
                  <mode.icon size={32} />
                </div>
                <h3 className="text-2xl font-bold mb-4">{mode.title}</h3>
                <p className="text-slate-400 mb-8 flex-1 leading-relaxed">
                  {mode.desc}
                </p>
                <div className="px-4 py-2 rounded-full bg-white/5 border border-white/10 text-xs font-bold uppercase tracking-widest text-primary">
                  {mode.capability}
                </div>
              </Card>
            </motion.div>
          ))}
        </div>

        <div className="mt-16 text-center">
          <p className="text-xs text-slate-500 max-w-xl mx-auto italic">
            *Autonomous capabilities are dependent on the specific sensor configuration and implemented software modules.
          </p>
        </div>
      </div>
    </section>
  )
}
