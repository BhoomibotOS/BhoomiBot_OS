"use client"

import * as React from "react"
import { motion } from "framer-motion"
import Image from "next/image"
import { CheckCircle2, TrendingUp, Users, Map } from "lucide-react"

const cases = [
  {
    customer: "Green Valley Farms",
    location: "Nashik, Maharashtra",
    stat: "40% Labor Cost Reduction",
    desc: "Implemented BhoomiBot for precision weeding across 50 acres of grape plantations.",
    image: "/robots/case-1.jpg"
  },
  {
    customer: "AgroPulse Logistics",
    location: "Punjab",
    stat: "2.5x Faster Harvesting",
    desc: "Used modular cargo attachments to streamline material movement from field to cold storage.",
    image: "/robots/case-2.jpg"
  }
]

export function CaseStudies() {
  return (
    <section id="cases" className="py-24 bg-slate-950 text-white overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/20 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Success Stories
          </div>
          <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">Real World Impact</h2>
          <p className="text-slate-400 max-w-2xl text-lg">
            See how BhoomiBot is transforming field operations for our early adoption partners.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {cases.map((item, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="group relative bg-zinc-900 rounded-[3rem] border border-white/5 overflow-hidden shadow-2xl"
            >
              <div className="flex flex-col md:flex-row h-full">
                <div className="md:w-1/2 relative h-[300px] md:h-auto overflow-hidden">
                   <Image
                     src={item.image}
                     alt={item.customer}
                     fill
                     className="object-cover transition-transform duration-700 group-hover:scale-110"
                   />
                   <div className="absolute inset-0 bg-gradient-to-r from-zinc-900 via-transparent to-transparent hidden md:block" />
                </div>

                <div className="md:w-1/2 p-10 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center gap-2 text-primary mb-4">
                       <TrendingUp size={16} />
                       <span className="text-xs font-black uppercase tracking-widest">{item.stat}</span>
                    </div>
                    <h3 className="text-3xl font-black tracking-tighter mb-2">{item.customer}</h3>
                    <div className="flex items-center gap-2 text-slate-500 text-xs mb-6 font-bold">
                       <Map size={12} /> {item.location}
                    </div>
                    <p className="text-slate-400 text-sm leading-relaxed mb-8">
                      {item.desc}
                    </p>
                  </div>

                  <div className="space-y-4">
                    <div className="flex items-center gap-3">
                      <div className="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center text-green-500">
                        <CheckCircle2 size={14} />
                      </div>
                      <span className="text-[10px] font-black uppercase tracking-widest text-slate-300">Autonomous Navigation</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center text-green-500">
                        <CheckCircle2 size={14} />
                      </div>
                      <span className="text-[10px] font-black uppercase tracking-widest text-slate-300">Modular Attachment Swap</span>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        <div className="mt-20 p-12 bg-primary rounded-[3rem] text-black relative overflow-hidden text-center md:text-left">
           <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
              <div className="max-w-xl">
                 <h4 className="text-4xl font-black tracking-tighter italic mb-4">Start Your Story Today</h4>
                 <p className="font-bold text-black/70 leading-relaxed">
                   Join our growing network of tech-enabled farms and industrial hubs. Let's calculate your specific ROI together.
                 </p>
              </div>
              <button className="px-10 py-6 bg-black text-white rounded-2xl font-black uppercase tracking-widest text-sm hover:scale-[1.05] transition-transform shadow-2xl">
                 Apply for Pilot Program
              </button>
           </div>
           {/* Decoration */}
           <div className="absolute top-0 right-0 w-64 h-64 bg-white/20 blur-[80px] rounded-full -translate-y-1/2 translate-x-1/2" />
        </div>
      </div>
    </section>
  )
}
