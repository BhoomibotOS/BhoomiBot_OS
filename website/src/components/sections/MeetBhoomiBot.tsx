"use client"

import { Card } from "@/components/ui/card"
import { motion } from "framer-motion"
import { Battery, Cpu, Radio, Shield, Settings2, Eye } from "lucide-react"
import Image from "next/image"

export function MeetBhoomiBot() {
  const specs = [
    { icon: Settings2, title: "Modular Platform", desc: "Heavy-duty steel chassis designed for versatility with interchangeable attachments." },
    { icon: Battery, title: "48V 50Ah LiFePO4", desc: "Industrial-grade energy storage for high-torque and long-duration tasks." },
    { icon: Radio, title: "Edge Tele-Op", desc: "Real-time, ultra-low latency control via CloudBridge relay." },
    { icon: Cpu, title: "Dual Motor Drive", desc: "Independent high-torque motors for precise differential steering." },
    { icon: Eye, title: "Vision-First", desc: "Integrated high-speed video pipeline for remote situational awareness." },
    { icon: Shield, title: "Industrial Safety", desc: "Built-in electronic braking and hardware-level emergency stop." },
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
            className="rounded-3xl overflow-hidden bg-white dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 p-2 shadow-2xl relative group"
          >
            <Image
              src="/robots/proto_type.jpeg"
              alt="BhoomiBot Prototype Chassis"
              width={1000}
              height={800}
              className="w-full h-auto object-cover rounded-2xl grayscale-[0.2] group-hover:grayscale-0 transition-all duration-700"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-8">
                <p className="text-white text-xs font-mono uppercase tracking-[0.3em]">Engineering Prototype: v2.4 Chassis</p>
            </div>
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
