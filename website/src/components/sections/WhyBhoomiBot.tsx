"use client"

import { motion } from "framer-motion"
import { CheckCircle2, Zap, Settings, Laptop, BarChart3, Package } from "lucide-react"

const features = [
  {
    title: "Reduce Manual Work",
    description: "Automate repetitive and physically demanding field operations to improve efficiency.",
    icon: CheckCircle2
  },
  {
    title: "Multi-Purpose Platform",
    description: "Use the same robotic platform for various different tasks with modular attachments.",
    icon: Package
  },
  {
    title: "Electric Drive",
    description: "Battery-powered robotic operation for a sustainable and clean agricultural future.",
    icon: Zap
  },
  {
    title: "Remote Operation",
    description: "Complete control and monitoring through the dedicated BhoomiBot application.",
    icon: Laptop
  },
  {
    title: "Intelligent Operation",
    description: "Supports sensor-based and vision-based functions for precise field tasks.",
    icon: BarChart3
  },
  {
    title: "Modular Attachments",
    description: "Quickly change the working tool according to the specific seasonal task.",
    icon: Settings
  }
]

export function WhyBhoomiBot() {
  return (
    <section className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="flex flex-col md:flex-row justify-between items-end mb-16 gap-6">
          <div className="max-w-2xl">
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-4">Why BhoomiBot?</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg">
              Designed to solve the real-world challenges of modern field operations through engineering excellence.
            </p>
          </div>
          <div className="hidden md:block">
            <div className="h-px w-32 bg-primary mb-4" />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="group p-8 rounded-3xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-zinc-900/50 hover:bg-primary/5 hover:border-primary/20 transition-all duration-300"
            >
              <div className="w-12 h-12 rounded-2xl bg-white dark:bg-black border border-slate-100 dark:border-slate-800 flex items-center justify-center mb-6 text-primary group-hover:scale-110 transition-transform">
                <feature.icon size={24} />
              </div>
              <h3 className="text-xl font-bold mb-3">{feature.title}</h3>
              <p className="text-slate-500 dark:text-slate-400 leading-relaxed">
                {feature.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
