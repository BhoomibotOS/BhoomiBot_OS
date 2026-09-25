"use client"

import { motion } from "framer-motion"
import { ArrowRight, MapPin } from "lucide-react"
import Image from "next/image"

export function Logistics() {
  return (
    <section className="py-16 sm:py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="w-full px-4 sm:px-6 md:px-8 lg:px-12">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-16 items-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="rounded-3xl sm:rounded-[3rem] overflow-hidden shadow-2xl"
          >
            <Image
              src="/robots/logistics.jpg"
              alt="BhoomiBot Logistics"
              width={800}
              height={600}
              className="w-full h-[280px] sm:h-[400px] md:h-[500px] object-cover"
            />
          </motion.div>

          <div>
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Move More. Handle Less.</h2>
            <p className="text-slate-600 dark:text-slate-400 text-base sm:text-lg mb-8 sm:mb-10 leading-relaxed">
              Beyond field work, BhoomiBot excels at material handling. Automate the transportation of goods across farms, factories, and warehouses.
            </p>

            <div className="space-y-6 sm:space-y-8">
              {[
                "Farm-to-storage transportation",
                "Internal warehouse material movement",
                "Point-to-Point autonomous missions"
              ].map((item, i) => (
                <div key={i} className="flex items-start gap-4">
                  <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center shrink-0 mt-1">
                    <div className="w-2 h-2 rounded-full bg-primary" />
                  </div>
                  <p className="text-base sm:text-xl font-bold text-slate-800 dark:text-slate-200">{item}</p>
                </div>
              ))}
            </div>

            <div className="mt-8 sm:mt-16 p-4 sm:p-8 bg-white dark:bg-zinc-900 rounded-2xl sm:rounded-3xl border border-slate-200 dark:border-slate-800 relative overflow-hidden">
               <div className="flex items-center justify-between relative z-10">
                  <div className="flex flex-col items-center">
                    <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-slate-100 dark:bg-zinc-800 flex items-center justify-center mb-2">
                      <MapPin size={18} className="text-primary" />
                    </div>
                    <span className="text-[10px] sm:text-xs font-bold uppercase tracking-wider text-slate-400">Point A</span>
                  </div>

                  <div className="flex-1 px-3 sm:px-8 relative">
                    <div className="h-1 bg-slate-100 dark:bg-zinc-800 w-full rounded-full" />
                    <motion.div
                      className="absolute top-0 left-3 sm:left-8 right-3 sm:right-8 h-1 bg-primary rounded-full"
                      initial={{ scaleX: 0, originX: 0 }}
                      whileInView={{ scaleX: 1 }}
                      viewport={{ once: true }}
                      transition={{ duration: 2, repeat: Infinity, repeatDelay: 1 }}
                    />
                  </div>

                  <div className="flex flex-col items-center">
                    <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-2xl bg-slate-100 dark:bg-zinc-800 flex items-center justify-center mb-2">
                      <MapPin size={18} className="text-primary" />
                    </div>
                    <span className="text-[10px] sm:text-xs font-bold uppercase tracking-wider text-slate-400">Point B</span>
                  </div>
               </div>

               <div className="mt-6 sm:mt-8 text-center">
                 <p className="text-xs sm:text-sm font-medium text-slate-500 italic">BhoomiBot navigating mission path...</p>
               </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
