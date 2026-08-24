"use client"

import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import { ChevronDown, Play, Zap, Leaf, Truck, Settings } from "lucide-react"
import dynamic from "next/dynamic"

import Skeleton from 'react-loading-skeleton'
import 'react-loading-skeleton/dist/skeleton.css'

const RobotCanvas = dynamic(() => import("@/components/RobotCanvas"), {
  ssr: false,
  loading: () => <Skeleton height="100%" borderRadius="1.5rem" baseColor="#222" highlightColor="#444" />
})

export function Hero() {
  const scrollToNext = () => {
    const nextSection = document.getElementById('robot');
    if (nextSection) {
      nextSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <section id="home" className="relative min-h-screen pt-20 flex items-center overflow-hidden bg-white dark:bg-black">
      <div className="container mx-auto px-4 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
        <motion.div
          initial={{ opacity: 0, x: -50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="z-10"
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2, duration: 0.5 }}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 text-primary text-sm font-bold tracking-wider uppercase mb-6"
          >
            <Zap size={16} className="animate-pulse" />
            BhoomiBot AI Labs
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.8 }}
            className="text-5xl md:text-7xl font-black tracking-tighter leading-tight mb-6 text-slate-900 dark:text-white"
          >
            Precision Robotics for
            <span className="text-primary"> Indian Agriculture</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4, duration: 0.8 }}
            className="text-lg md:text-xl text-slate-600 dark:text-slate-400 mb-8 max-w-xl leading-relaxed"
          >
            Autonomous field operations. Modular attachments. Full remote control. Zero emissions.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.5, duration: 0.6 }}
            className="flex flex-wrap gap-4 mb-8"
          >
            <Button
              size="lg"
              className="px-8 py-6 text-base font-bold rounded-xl shadow-xl shadow-primary/20 hover:shadow-primary/30 transition-all duration-300 hover:scale-105"
              onClick={() => document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' })}
            >
              <Play size={20} className="mr-2" />
              Watch Demo
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="px-8 py-6 text-base font-bold rounded-xl border-2 hover:bg-primary/5 hover:border-primary/50 transition-all duration-300 hover:scale-105"
              onClick={() => document.getElementById('applications-grid')?.scrollIntoView({ behavior: 'smooth' })}
            >
              Explore Applications
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
          className="relative h-[400px] md:h-[600px]"
        >
          <RobotCanvas />
          <div className="absolute inset-0 pointer-events-none bg-gradient-to-t from-white dark:from-black via-transparent to-transparent opacity-20" />

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1.5, duration: 0.5 }}
            className="absolute bottom-8 right-8 hidden md:block"
          >
            <motion.div
              animate={{ y: [0, -8, 0] }}
              transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
              className="flex items-center gap-2 text-sm font-medium text-slate-500 dark:text-slate-400 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm rounded-full px-4 py-2 shadow-lg"
            >
              <span>Scroll to explore</span>
              <ChevronDown size={16} />
            </motion.div>
          </motion.div>
        </motion.div>
      </div>

      <div className="absolute top-1/4 right-0 w-1/3 h-1/3 bg-primary/5 blur-[120px] rounded-full -z-10" />
      <div className="absolute bottom-1/4 left-0 w-1/4 h-1/4 bg-blue-500/5 blur-[100px] rounded-full -z-10" />
    </section>
  )
}