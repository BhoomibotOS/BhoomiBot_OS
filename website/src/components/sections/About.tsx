"use client"

import { motion } from "framer-motion"
import Image from "next/image"

export function About() {
  return (
    <section id="about" className="py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <div className="order-2 lg:order-1">
             <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">About BhoomiBot AI Labs</h2>
             <p className="text-xl font-bold text-primary mb-6 italic">"Cultivating Intelligence"</p>
             <div className="space-y-6 text-slate-600 dark:text-slate-400 text-lg leading-relaxed">
               <p>
                 BhoomiBot AI Labs is an Indian robotics company dedicated to building practical, reliable, and accessible robotic systems for real-world environments.
               </p>
               <p>
                 Our mission is to reduce repetitive human effort and make precision automation available for agriculture, logistics, and field operations through engineering excellence in embedded systems, AI, and computer vision.
               </p>
               <p>
                 Based in Pune, we are focused on developing the next generation of modular robotic platforms that can adapt to the unique challenges of the Indian landscape.
               </p>
             </div>

             <div className="mt-12 grid grid-cols-2 sm:grid-cols-3 gap-6">
                {[
                  "Robotics",
                  "Embedded Systems",
                  "AI & Vision",
                  "Automation",
                  "Navigation",
                  "Agri-Tech"
                ].map((tag) => (
                  <div key={tag} className="px-4 py-2 rounded-xl bg-white dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 text-xs font-black uppercase tracking-widest text-center shadow-sm">
                    {tag}
                  </div>
                ))}
             </div>
          </div>

          <motion.div
            initial={{ opacity: 0, x: 50 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="order-1 lg:order-2 rounded-[3rem] overflow-hidden shadow-2xl border-8 border-white dark:border-zinc-900"
          >
            <Image
              src="/robots/team-work.jpg"
              alt="BhoomiBot Team Work"
              width={800}
              height={600}
              className="w-full h-[500px] object-cover grayscale"
            />
          </motion.div>
        </div>

        {/* Team Section */}
        <div className="mt-32">
          <div className="text-center mb-16">
            <h3 className="text-3xl font-black tracking-tighter mb-4">Our Core Experts</h3>
            <p className="text-slate-500 dark:text-slate-400 max-w-xl mx-auto">
              The engineering minds behind BhoomiBot's modular architecture and intelligent systems.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-12 max-w-4xl mx-auto">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="group"
            >
              <div className="aspect-[3/4] rounded-[2.5rem] overflow-hidden bg-white dark:bg-zinc-900 border-8 border-white dark:border-zinc-900 shadow-xl mb-6">
                <Image
                  src="/team/ChatGPT Image Aug 23, 2026, 07_20_21 PM.png"
                  alt="Technical Expert"
                  width={600}
                  height={800}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
              </div>
              <div className="text-center">
                <h4 className="text-2xl font-black tracking-tighter">SW and Systems Architect</h4>
                <p className="text-primary font-bold uppercase text-xs tracking-widest mt-1">Technical Expert & All-rounder</p>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.2 }}
              className="group"
            >
              <div className="aspect-[3/4] rounded-[2.5rem] overflow-hidden bg-white dark:bg-zinc-900 border-8 border-white dark:border-zinc-900 shadow-xl mb-6">
                <Image
                  src="/team/ChatGPT Image Aug 23, 2026, 07_23_07 PM.png"
                  alt="Design Expert"
                  width={600}
                  height={800}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
              </div>
              <div className="text-center">
                <h4 className="text-2xl font-black tracking-tighter">Systems Architect</h4>
                <p className="text-primary font-bold uppercase text-xs tracking-widest mt-1">Design Expert & Mechanical Systems</p>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </section>
  )
}
