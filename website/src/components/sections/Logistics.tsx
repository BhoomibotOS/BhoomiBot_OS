"use client"

import { motion } from "framer-motion"
import { ArrowRight, MapPin } from "lucide-react"
import Image from "next/image"

export function Logistics() {
  return (
    <section className="py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="rounded-[3rem] overflow-hidden shadow-2xl"
          >
            <Image
              src="/robots/logistics.jpg"
              alt="BhoomiBot Logistics"
              width={800}
              height={600}
              className="w-full h-[500px] object-cover"
            />
          </motion.div>

          <div>
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Move More. Handle Less.</h2>
            <p className="text-slate-600 dark:text-slate-400 text-lg mb-10">
              Beyond field work, BhoomiBot excels at material handling. Automate the transportation of goods across farms, factories, and warehouses.
            </p>

            <div className="space-y-8">
              {[
                "Farm-to-storage transportation",
                "Internal warehouse material movement",
                "Point-to-Point autonomous missions"
              ].map((item, i) => (
                <div key={i} className="flex items-start gap-4">
                  <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-primary" />
                  </div>
                  <p className="text-xl font-bold text-slate-800 dark:text-slate-200">{item}</p>
                </div>
              ))}
            </div>

            <div className="mt-16 p-8 bg-white dark:bg-zinc-900 rounded-3xl border border-slate-200 dark:border-slate-800 relative overflow-hidden">
               <div className="flex items-center justify-between relative z-10">
                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-zinc-800 flex items-center justify-center mb-2">
                      <MapPin size={20} className="text-primary" />
                    </div>
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Point A</span>
                  </div>

                  <div className="flex-1 px-8 relative">
                    <div className="h-1 bg-slate-100 dark:bg-zinc-800 w-full rounded-full" />
                    <motion.div
                      className="absolute top-0 left-8 right-8 h-1 bg-primary rounded-full"
                      initial={{ scaleX: 0, originX: 0 }}
                      whileInView={{ scaleX: 1 }}
                      viewport={{ once: true }}
                      transition={{ duration: 2, repeat: Infinity, repeatDelay: 1 }}
                    />
                    <motion.div
                      className="absolute -top-2 left-8 w-5 h-5 bg-white dark:bg-zinc-900 border-2 border-primary rounded-lg flex items-center justify-center shadow-lg"
                      animate={{ left: ["calc(0% + 32px)", "calc(100% - 52px)"] }}
                      transition={{ duration: 2, repeat: Infinity, repeatDelay: 1 }}
                    >
                      <div className="w-2 h-2 bg-primary rounded-full" />
                    </motion.div>
                  </div>

                  <div className="flex flex-col items-center">
                    <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-zinc-800 flex items-center justify-center mb-2">
                      <MapPin size={20} className="text-primary" />
                    </div>
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Point B</span>
                  </div>
               </div>

               <div className="mt-8 text-center">
                 <p className="text-sm font-medium text-slate-500 italic">BhoomiBot navigating mission path...</p>
               </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
