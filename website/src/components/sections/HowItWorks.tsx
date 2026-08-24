"use client"

import { motion } from "framer-motion"

const steps = [
  {
    number: "01",
    title: "Select the Task",
    desc: "Choose the required operation from the BhoomiBot application—whether it's weeding, spraying, or transport."
  },
  {
    number: "02",
    title: "Configure BhoomiBot",
    desc: "Attach the appropriate modular tool and select your preferred operating mode (Manual or Autonomous)."
  },
  {
    number: "03",
    title: "Operate",
    desc: "Deploy the robot and monitor its progress in real-time through the live data feed and camera vision."
  }
]

export function HowItWorks() {
  return (
    <section className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="text-center mb-20">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">How It Works</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
            Get started with BhoomiBot in three simple steps. Automation made accessible for everyone.
          </p>
        </div>

        <div className="relative">
          {/* Animated Line */}
          <div className="absolute top-1/2 left-0 w-full h-1 bg-slate-100 dark:bg-zinc-800 -translate-y-1/2 hidden lg:block" />

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-12 relative z-10">
            {steps.map((step, index) => (
              <motion.div
                key={step.number}
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.2 }}
                className="flex flex-col items-center text-center group"
              >
                <div className="w-24 h-24 rounded-[2rem] bg-white dark:bg-zinc-900 border-4 border-slate-100 dark:border-zinc-800 flex items-center justify-center mb-8 group-hover:border-primary transition-colors duration-500 shadow-xl">
                  <span className="text-3xl font-black text-primary">{step.number}</span>
                </div>
                <h3 className="text-2xl font-bold mb-4">{step.title}</h3>
                <p className="text-slate-500 dark:text-slate-400 leading-relaxed max-w-xs">
                  {step.desc}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
