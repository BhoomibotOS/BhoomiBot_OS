"use client"

import { motion } from "framer-motion"
import Image from "next/image"

const applications = [
  {
    name: "Weeding",
    desc: "Autonomous and precision weeding between crop rows.",
    image: "/robots/weeding.jpg"
  },
  {
    name: "Spraying",
    desc: "Targeted application of fertilizers and pesticides.",
    image: "/robots/spraying.jpg"
  },
  {
    name: "Ploughing",
    desc: "Soil preparation with heavy-duty modular attachments.",
    image: "/robots/ploughing.jpg"
  },
  {
    name: "Crop Monitoring",
    desc: "Regular field patrolling with high-definition cameras.",
    image: "/robots/monitoring.jpg"
  }
]

export function Agriculture() {
  return (
    <section id="agriculture" className="py-24 bg-slate-900 text-white">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/20 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Indian Agriculture
          </div>
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Built for Indian Farms</h2>
          <p className="text-slate-400 max-w-2xl text-lg">
            BhoomiBot is designed to handle the diverse terrain and specific requirements of Indian agricultural environments.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {applications.map((app, index) => (
            <motion.div
              key={app.name}
              initial={{ opacity: 0, scale: 0.9 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="relative group h-[400px] rounded-3xl overflow-hidden"
            >
              <Image
                src={app.image}
                alt={app.name}
                width={800}
                height={600}
                className="w-full h-full object-cover grayscale group-hover:grayscale-0 group-hover:scale-110 transition-all duration-500"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/20 to-transparent" />
              <div className="absolute bottom-0 left-0 p-8">
                <h3 className="text-2xl font-bold mb-2">{app.name}</h3>
                <p className="text-slate-300 text-sm opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  {app.desc}
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
