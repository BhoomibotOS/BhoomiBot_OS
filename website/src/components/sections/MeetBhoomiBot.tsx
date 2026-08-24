"use client"

import { Card } from "@/components/ui/card"
import { motion } from "framer-motion"
import { Battery, Cpu, Radio, Shield, Settings2, Eye } from "lucide-react"
import Image from "next/image"

export function MeetBhoomiBot() {
  const specs = [
    { icon: Settings2, title: "Modular Platform", desc: "Designed for versatility with interchangeable attachments." },
    { icon: Battery, title: "Electric Drive", desc: "Clean, efficient power for long-duration operations." },
    { icon: Radio, title: "Remote Operation", desc: "Real-time control via the mobile application." },
    { icon: Cpu, title: "Sensor Integration", desc: "Rich sensor suite for environment awareness." },
    { icon: Eye, title: "Vision Capability", desc: "Integrated cameras for monitoring and navigation." },
    { icon: Shield, title: "Expandable Core", desc: "Open software architecture for future growth." },
  ]

  return (
    <section id="robot" className="py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="container mx-auto px-4">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Meet BhoomiBot</h2>
          <p className="text-slate-600 dark:text-slate-400 text-lg">
            A versatile, electric robotic platform engineered for the rugged demands of field operations. BhoomiBot combines industrial durability with intelligent control.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="rounded-3xl overflow-hidden bg-white dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 p-4 shadow-2xl"
          >
            <Image
              src="/robots/bhoomibot-platform.jpg"
              alt="BhoomiBot Platform"
              width={1000}
              height={600}
              className="w-full h-[400px] object-cover rounded-2xl"
            />
          </motion.div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {specs.map((item, index) => (
              <motion.div
                key={item.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.1 }}
              >
                <Card className="p-6 h-full hover:border-primary/50 transition-colors border-slate-200 dark:border-slate-800 bg-white dark:bg-zinc-900">
                  <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center mb-4 text-primary">
                    <item.icon size={20} />
                  </div>
                  <h3 className="font-bold text-lg mb-2">{item.title}</h3>
                  <p className="text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
                    {item.desc}
                  </p>
                </Card>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
