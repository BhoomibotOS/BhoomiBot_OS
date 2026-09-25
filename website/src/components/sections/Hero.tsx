"use client"

import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import { Play, Zap, Leaf, Truck, Settings } from "lucide-react"
import dynamic from "next/dynamic"
import Image from "next/image"

import Skeleton from 'react-loading-skeleton'
import 'react-loading-skeleton/dist/skeleton.css'

const RobotCanvas = dynamic(() => import("@/components/RobotCanvas"), {
  ssr: false,
  loading: () => <div className="w-full h-full bg-slate-900 animate-pulse rounded-[2.5rem]" />
})

export function Hero() {
  const scrollToNext = () => {
    const nextSection = document.getElementById('robot');
    if (nextSection) {
      nextSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <section id="home" className="relative min-h-screen pt-20 pb-12 flex items-center overflow-hidden bg-white dark:bg-black">
      <div className="w-full px-4 sm:px-6 md:px-8 lg:px-12 py-6 lg:py-10 grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-16 items-center">
        <motion.div
          initial={{ opacity: 0, x: -50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="z-10"
        >
          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.8 }}
            className="text-3xl sm:text-5xl md:text-6xl lg:text-7xl font-black tracking-tighter leading-tight mb-6 text-slate-900 dark:text-white"
          >
            Precision Robotics for
            <span className="text-primary"> Indian Agriculture</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4, duration: 0.8 }}
            className="text-base sm:text-lg md:text-xl text-slate-600 dark:text-slate-400 mb-8 max-w-xl leading-relaxed"
          >
            Autonomous field operations. Modular attachments. Full remote control. Zero emissions.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.5, duration: 0.6 }}
            className="flex flex-col sm:flex-row flex-wrap gap-3 sm:gap-4 mb-8"
          >
            <Button
              size="lg"
              className="w-full sm:w-auto px-8 py-6 text-base font-bold rounded-xl shadow-xl shadow-primary/20 hover:shadow-primary/30 transition-all duration-300 hover:scale-105"
              onClick={() => window.location.href = '/live'}
            >
              <Play size={20} className="mr-2" />
              Open Live Console
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="w-full sm:w-auto px-8 py-6 text-base font-bold rounded-xl border-2 hover:bg-primary/5 hover:border-primary/50 transition-all duration-300 hover:scale-105"
              onClick={() => document.getElementById('robot')?.scrollIntoView({ behavior: 'smooth' })}
            >
              View Specifications
            </Button>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6, duration: 0.8 }}
            className="grid grid-cols-2 md:grid-cols-4 gap-4 border-t border-slate-100 dark:border-slate-800 pt-8"
          >
            {[
              { icon: Leaf, label: "Electric", desc: "Zero emission, efficient" },
              { icon: Truck, label: "Modular", desc: "Swappable attachments" },
              { icon: Settings, label: "Smart", desc: "AI-powered automation" },
              { icon: Zap, label: "Connected", desc: "Real-time control" }
            ].map((feature, index) => (
              <motion.div
                key={feature.label}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 + index * 0.1, duration: 0.6 }}
                className="space-y-3 group"
              >
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center mb-3 text-primary">
                  <feature.icon size={20} />
                </div>
                <p className="text-sm font-bold text-slate-800 dark:text-slate-200 uppercase tracking-wider">{feature.label}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">{feature.desc}</p>
              </motion.div>
            ))}
          </motion.div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 1, delay: 0.2 }}
          className="relative h-[300px] sm:h-[400px] lg:h-[500px] xl:h-[560px] w-full"
        >
          <RobotCanvas />
        </motion.div>
      </div>

      <div className="absolute top-1/4 right-0 w-1/3 h-1/3 bg-primary/5 blur-[120px] rounded-full -z-10" />
      <div className="absolute bottom-1/4 left-0 w-1/4 h-1/4 bg-blue-500/5 blur-[100px] rounded-full -z-10" />
    </section>
  )
}
