"use client"

import * as React from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
  Play,
  Settings2,
  Map as MapIcon,
  Target,
  CheckCircle2,
  Navigation,
  Eye,
  ArrowRight,
  Package,
  Trash2,
  RefreshCw,
  Smartphone
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import dynamic from "next/dynamic"

import Skeleton from 'react-loading-skeleton'
import 'react-loading-skeleton/dist/skeleton.css'

const RobotCanvas = dynamic(() => import("@/components/RobotCanvas"), {
  ssr: false,
  loading: () => <Skeleton height="100%" borderRadius="1.5rem" baseColor="#f0f0f0" />
})

type Scenario = "weeding" | "logistics"

export function ActionDemo() {
  const [scenario, setScenario] = React.useState<Scenario>("weeding")
  const [step, setStep] = React.useState(0)
  const [isAutoPlaying, setIsAutoPlaying] = React.useState(false)

  const weedingSteps = [
    {
      title: "Select the Task",
      desc: "Operator selects 'Weed Removal' and defines the mission area in the BhoomiBot application.",
      icon: Smartphone,
      status: "Task: Weed Removal | Field Area: North A1",
      visual: "app"
    },
    {
      title: "Select the Attachment",
      desc: "The weed-removal attachment is mounted. Designed to remove weeds between crop rows with precision.",
      icon: Settings2,
      status: "Attachment: Weed Removal | Status: Ready",
      visual: "attachment"
    },
    {
      title: "Teach Once",
      desc: "Operator guides the robot through one row. BhoomiBot records path, spacing, and turning points.",
      icon: MapIcon,
      status: "Pattern Learning... | Row Pattern Stored",
      visual: "teach"
    },
    {
      title: "Follow the Crop Row",
      desc: "Robot uses vision to identify row centerlines and follows the learned pattern autonomously.",
      icon: Navigation,
      status: "Row Following: ACTIVE | Current Row: 01",
      visual: "follow"
    },
    {
      title: "Detect & Remove",
      desc: "Vision system identifies weeds. Robot slows down, positions the tool, and removes the weed.",
      icon: Eye,
      status: "Weed Detected | Action: Removal",
      visual: "detect"
    },
    {
      title: "End of Row & Turn",
      desc: "Reaching the row end, BhoomiBot performs a safe turning maneuver to enter the next row.",
      icon: RefreshCw,
      status: "Row 01 Complete | Turning to Row 02",
      visual: "turn"
    },
    {
      title: "Mission Complete",
      desc: "All rows processed. The robot returns to the service area for attachment swap or recharge.",
      icon: CheckCircle2,
      status: "Mission Complete | Area Covered: 100%",
      visual: "complete"
    }
  ]

  const logisticsSteps = [
    {
      title: "Select Task",
      desc: "Operator selects 'Material Transport' and sets Point A (Field) and Point B (Storage).",
      icon: Smartphone,
      status: "Task: Transport | Route: Field -> Storage",
      visual: "app-logistics"
    },
    {
      title: "Load Cargo",
      desc: "BhoomiBot enters the field with the Cargo Platform. Crates/Material are loaded for transport.",
      icon: Package,
      status: "Attachment: Cargo | Load: Nominal",
      visual: "load"
    },
    {
      title: "Autonomous Delivery",
      desc: "The robot follows the predefined farm route to the storage area autonomously.",
      icon: Navigation,
      status: "Status: Transporting | Mission: Delivery",
      visual: "delivery"
    },
    {
      title: "Unload & Return",
      desc: "Material is unloaded at the destination, and the robot returns for the next mission.",
      icon: CheckCircle2,
      status: "Delivery Complete | Returning to Base",
      visual: "complete-logistics"
    }
  ]

  const currentSteps = scenario === "weeding" ? weedingSteps : logisticsSteps
  const progress = ((step + 1) / currentSteps.length) * 100

  const handleNext = () => {
    if (step < currentSteps.length - 1) {
      setStep(s => s + 1)
    } else {
      setStep(0)
    }
  }

  const handlePrev = () => {
    if (step > 0) {
      setStep(s => s - 1)
    }
  }

  React.useEffect(() => {
    setStep(0)
  }, [scenario])

  return (
    <section id="demo" className="py-24 bg-slate-50 dark:bg-zinc-950 overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Interactive Demo
          </div>
          <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">See BhoomiBot in Action</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl text-lg">
            Experience the complete workflow of an autonomous field mission, from task selection to completion.
          </p>
        </div>

        <div className="flex flex-wrap justify-center gap-4 mb-12">
          <Button
            variant={scenario === "weeding" ? "default" : "outline"}
            onClick={() => setScenario("weeding")}
            className="rounded-xl px-8 py-6 font-bold"
          >
            1. Weed Removal
          </Button>
          <Button
            variant={scenario === "logistics" ? "default" : "outline"}
            onClick={() => setScenario("logistics")}
            className="rounded-xl px-8 py-6 font-bold"
          >
            2. Farm Logistics
          </Button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch min-h-[700px]">
          {/* Main Visual Area */}
          <div className="lg:col-span-8 bg-white dark:bg-zinc-900 rounded-[3rem] border border-slate-200 dark:border-slate-800 shadow-2xl relative overflow-hidden flex flex-col">

            <div className="flex-1 relative">
              <AnimatePresence mode="wait">
                <motion.div
                  key={`${scenario}-${step}`}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.5 }}
                  className="w-full h-full"
                >
                  {/* Visual Renderings based on Step */}
                  <VisualRenderer scenario={scenario} step={step} />
                </motion.div>
              </AnimatePresence>
            </div>

            {/* Mission Status Overlay */}
            <div className="absolute top-8 left-8 right-8 pointer-events-none">
              <div className="flex justify-between items-start">
                <div className="bg-slate-900/90 backdrop-blur-md border border-white/10 p-4 rounded-2xl text-white shadow-2xl min-w-[240px]">
                  <div className="flex items-center gap-2 mb-2">
                    <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                    <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">Live Mission Data</span>
                  </div>
                  <p className="font-mono text-sm text-primary">{currentSteps[step].status}</p>
                </div>

                <div className="bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border border-slate-200 dark:border-slate-800 p-4 rounded-2xl shadow-xl">
                  <p className="text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Progress</p>
                  <div className="w-32 h-2 bg-slate-100 dark:bg-zinc-800 rounded-full overflow-hidden">
                    <motion.div
                      className="h-full bg-primary"
                      initial={{ width: 0 }}
                      animate={{ width: `${progress}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Controls */}
            <div className="p-8 bg-slate-50/50 dark:bg-zinc-900/50 border-t border-slate-100 dark:border-slate-800 flex justify-between items-center">
              <div className="flex gap-2">
                <Button variant="outline" size="icon" onClick={handlePrev} disabled={step === 0} className="rounded-xl">
                  <ArrowRight className="rotate-180" size={18} />
                </Button>
                <Button variant="outline" size="icon" onClick={handleNext} className="rounded-xl">
                  <ArrowRight size={18} />
                </Button>
              </div>

              <div className="flex items-center gap-4">
                <span className="text-sm font-bold text-slate-400">Step {step + 1} of {currentSteps.length}</span>
                <Button onClick={handleNext} className="rounded-xl font-bold px-6">
                  {step === currentSteps.length - 1 ? "Restart Demo" : "Next Step"}
                </Button>
              </div>
            </div>
          </div>

          {/* Info Side Panel */}
          <div className="lg:col-span-4 flex flex-col gap-6">
            <Card className="p-8 bg-white dark:bg-zinc-900 border-slate-200 dark:border-slate-800 rounded-[2.5rem] flex-1">
              <AnimatePresence mode="wait">
                <motion.div
                  key={step}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                >
                  <div className="w-16 h-16 rounded-2xl bg-primary/10 flex items-center justify-center mb-8 text-primary shadow-inner">
                    {React.createElement(currentSteps[step].icon, { size: 32 })}
                  </div>
                  <h3 className="text-3xl font-black tracking-tighter mb-4">{currentSteps[step].title}</h3>
                  <p className="text-slate-600 dark:text-slate-400 text-lg leading-relaxed mb-8">
                    {currentSteps[step].desc}
                  </p>
                </motion.div>
              </AnimatePresence>

              {step === 2 && scenario === "weeding" && (
                <div className="p-6 rounded-2xl bg-primary/5 border border-primary/20">
                  <h4 className="font-bold text-primary mb-2 uppercase text-xs tracking-widest">Teach Once Concept</h4>
                  <p className="text-sm text-slate-500 italic">
                    Teach the robot one row → reuse the learned pattern for the remaining rows.
                  </p>
                </div>
              )}
            </Card>

            <Card className="p-8 bg-slate-900 text-white rounded-[2.5rem] border-none shadow-2xl relative overflow-hidden">
              <div className="relative z-10">
                <h4 className="text-xl font-bold mb-4 italic text-primary">"Teach Once. Repeat Everywhere."</h4>
                <p className="text-sm text-slate-400 leading-relaxed">
                  BhoomiBot is designed around repeatable robotic workflows, allowing an operator to define an operation once and let the robot execute the supported task across the field.
                </p>
              </div>
              <div className="absolute top-0 right-0 w-32 h-32 bg-primary/20 blur-[60px] rounded-full -translate-y-1/2 translate-x-1/2" />
            </Card>
          </div>
        </div>
      </div>
    </section>
  )
}

function VisualRenderer({ scenario, step }: { scenario: Scenario, step: number }) {
  if (scenario === "weeding") {
    switch (step) {
      case 0: // Select Task
        return (
          <div className="w-full h-full flex items-center justify-center p-12 bg-slate-100 dark:bg-zinc-950">
             <motion.div
               initial={{ y: 20, opacity: 0 }}
               animate={{ y: 0, opacity: 1 }}
               className="w-64 h-[500px] bg-slate-900 rounded-[3rem] border-8 border-slate-800 shadow-2xl overflow-hidden p-2"
             >
                <div className="w-full h-full bg-black rounded-[2.5rem] overflow-hidden flex flex-col p-6 text-white">
                  <div className="mb-8">
                    <p className="text-[10px] font-black uppercase text-slate-500">Task Selection</p>
                    <h4 className="text-xl font-bold">Select Operation</h4>
                  </div>
                  <div className="space-y-3">
                    <div className="p-4 rounded-2xl bg-primary border border-primary text-black font-bold flex items-center justify-between">
                      <span>Weed Removal</span>
                      <Target size={16} />
                    </div>
                    <div className="p-4 rounded-2xl bg-zinc-900 border border-white/5 text-slate-400 font-bold flex items-center justify-between">
                      <span>Spraying</span>
                    </div>
                    <div className="p-4 rounded-2xl bg-zinc-900 border border-white/5 text-slate-400 font-bold flex items-center justify-between">
                      <span>Logistics</span>
                    </div>
                  </div>
                  <div className="mt-8">
                    <p className="text-[10px] font-black uppercase text-slate-500 mb-2">Selected Area</p>
                    <div className="aspect-square bg-zinc-800 rounded-2xl border border-white/10 flex items-center justify-center relative overflow-hidden">
                       <div className="absolute inset-4 border-2 border-primary/50 border-dashed rounded-lg bg-primary/10" />
                       <MapIcon size={40} className="text-zinc-700" />
                    </div>
                  </div>
                  <div className="mt-auto">
                    <Button className="w-full py-6 rounded-2xl font-black">START MISSION</Button>
                  </div>
                </div>
             </motion.div>
          </div>
        )
      case 1: // Attachment
        return (
          <div className="w-full h-full flex flex-col">
            <div className="flex-1 bg-slate-50 dark:bg-zinc-950 relative">
              <RobotCanvas attachment="none" />
              <motion.div
                initial={{ y: 100, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                className="absolute bottom-1/4 left-1/2 -translate-x-1/2 w-48 h-12 bg-slate-400 dark:bg-zinc-800 rounded-lg flex items-center justify-center text-xs font-bold text-white border-2 border-primary"
              >
                MOUNTING WEED REMOVER...
              </motion.div>
            </div>
          </div>
        )
      case 2: // Teach Once
        return (
          <div className="w-full h-full bg-green-900/10 p-12 overflow-hidden">
            <div className="relative w-full h-full border-4 border-slate-200 dark:border-slate-800 rounded-[2rem] bg-slate-50 dark:bg-zinc-900 p-8">
              <div className="grid grid-cols-5 gap-8 h-full">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="flex flex-col gap-4 items-center">
                    {[...Array(8)].map((_, j) => (
                      <div key={j} className="w-4 h-4 rounded-full bg-green-600/30" />
                    ))}
                  </div>
                ))}
              </div>
              {/* Path recording line */}
              <svg className="absolute inset-0 w-full h-full pointer-events-none">
                 <motion.path
                   d="M 120,450 L 120,50"
                   stroke="#10b981"
                   strokeWidth="8"
                   fill="none"
                   strokeDasharray="20,10"
                   initial={{ pathLength: 0 }}
                   animate={{ pathLength: 1 }}
                   transition={{ duration: 2, repeat: Infinity }}
                 />
                 <motion.circle
                   cx="120" cy="450" r="10" fill="#10b981"
                   animate={{ cy: [450, 50] }}
                   transition={{ duration: 2, repeat: Infinity }}
                 />
              </svg>
              <div className="absolute bottom-8 right-8 bg-slate-900 p-4 rounded-xl text-white font-mono text-[10px]">
                RECORDING ROW 01...<br/>
                SPACING: 80cm<br/>
                END POINT: DETECTED
              </div>
            </div>
          </div>
        )
      case 3: // Follow Row
      case 4: // Detect & Remove
        return (
          <div className="w-full h-full bg-slate-900 relative overflow-hidden">
            {/* Field Background */}
            <div className="absolute inset-0 opacity-20 flex justify-around">
               {[...Array(6)].map((_, i) => (
                 <div key={i} className="w-1 bg-green-500 h-full" />
               ))}
            </div>

            {/* Vision HUD */}
            <div className="absolute inset-0 flex items-center justify-center p-12">
               <div className="w-full h-full border-2 border-primary/30 rounded-[3rem] relative flex flex-col items-center justify-center">
                  <div className="absolute top-1/2 left-0 right-0 h-0.5 bg-primary/20" />
                  <div className="absolute left-1/2 top-0 bottom-0 w-0.5 bg-primary/40 shadow-[0_0_15px_rgba(16,185,129,0.5)]" />

                  {/* Robot Representation */}
                  <motion.div
                    animate={{ y: [200, -200] }}
                    transition={{ duration: 5, repeat: Infinity, ease: "linear" }}
                    className="relative z-10"
                  >
                    <div className="w-32 h-48 bg-slate-800 border-2 border-white/10 rounded-2xl flex items-center justify-center">
                       <div className="w-16 h-16 rounded-full border-2 border-primary animate-ping" />
                    </div>
                  </motion.div>

                  {/* Weed detection in step 4 */}
                  {step === 4 && (
                    <motion.div
                      initial={{ scale: 0, opacity: 0 }}
                      animate={{ scale: 1, opacity: 1 }}
                      className="absolute top-1/3 left-1/3 w-20 h-20 border-2 border-red-500 rounded-lg flex items-center justify-center"
                    >
                      <Trash2 className="text-red-500" />
                      <div className="absolute -top-6 left-0 text-[10px] bg-red-500 text-white px-2 py-0.5 font-bold rounded">TARGET: WEED</div>
                    </motion.div>
                  )}

                  <div className="absolute bottom-12 left-12 space-y-2">
                    <div className="flex items-center gap-2 text-primary font-black text-xs uppercase italic">
                      <Eye size={14} /> Vision Active
                    </div>
                    <div className="text-[10px] text-slate-500 font-mono">
                      OFFSET: +0.02m<br/>
                      SPEED: 1.2 km/h
                    </div>
                  </div>
               </div>
            </div>
          </div>
        )
      case 5: // Turn
        return (
          <div className="w-full h-full bg-slate-50 dark:bg-zinc-950 p-12 flex items-center justify-center">
            <svg viewBox="0 0 400 400" className="w-80 h-80">
               <path d="M 50,350 L 50,50 A 50,50 0 0 1 150,50 L 150,350" stroke="#10b981" strokeWidth="4" fill="none" strokeDasharray="10 5" opacity="0.3" />
               <motion.path
                 d="M 50,350 L 50,50 A 50,50 0 0 1 150,50 L 150,350"
                 stroke="#10b981"
                 strokeWidth="6"
                 fill="none"
                 initial={{ pathLength: 0 }}
                 animate={{ pathLength: 1 }}
                 transition={{ duration: 4, repeat: Infinity }}
               />
               <motion.g animate={{ rotate: [0, 180, 180, 0] }} transition={{ duration: 4, repeat: Infinity }}>
                 <rect x="-15" y="-20" width="30" height="40" rx="4" fill="#333" />
               </motion.g>
            </svg>
            <div className="absolute bottom-12 text-center">
               <p className="text-xl font-black tracking-tighter uppercase italic">Maneuvering...</p>
               <p className="text-xs text-slate-500">Entering Row 02</p>
            </div>
          </div>
        )
      case 6: // Complete
        return (
          <div className="w-full h-full flex items-center justify-center bg-slate-50 dark:bg-zinc-950 p-12">
            <div className="max-w-md w-full grid grid-cols-2 gap-4">
               {[
                 { label: "Rows Completed", value: "12" },
                 { label: "Area Covered", value: "1.2 Acres" },
                 { label: "Duration", value: "45 mins" },
                 { label: "Weeds Removed", value: "420" }
               ].map((stat, i) => (
                 <motion.div
                   key={i}
                   initial={{ scale: 0.9, opacity: 0 }}
                   animate={{ scale: 1, opacity: 1 }}
                   transition={{ delay: i * 0.1 }}
                   className="p-6 bg-white dark:bg-zinc-900 rounded-3xl border border-slate-200 dark:border-slate-800 text-center"
                 >
                   <p className="text-[10px] font-black uppercase text-slate-500 mb-2">{stat.label}</p>
                   <p className="text-3xl font-black text-primary">{stat.value}</p>
                 </motion.div>
               ))}
               <div className="col-span-2 p-8 bg-slate-900 rounded-[2.5rem] text-center mt-4">
                  <CheckCircle2 size={40} className="text-primary mx-auto mb-4" />
                  <h4 className="text-2xl font-black text-white italic tracking-tighter mb-2">MISSION COMPLETE</h4>
                  <p className="text-xs text-slate-400">Robot returning to charging station.</p>
               </div>
            </div>
          </div>
        )
      default:
        return null
    }
  } else {
    // Logistics Scenario
    switch (step) {
      case 0:
        return (
          <div className="w-full h-full flex items-center justify-center bg-slate-100 dark:bg-zinc-950">
             <div className="w-64 h-[500px] bg-slate-900 rounded-[3rem] border-8 border-slate-800 p-6 text-white flex flex-col">
                <h4 className="text-xl font-bold mb-8">Logistics Hub</h4>
                <div className="flex-1 space-y-6">
                   <div className="flex items-center justify-between p-4 bg-zinc-800 rounded-2xl">
                     <span className="text-sm font-bold text-primary">FIELD A</span>
                     <ArrowRight size={14} />
                     <span className="text-sm font-bold text-slate-500">STORAGE</span>
                   </div>
                   <div className="p-8 border-2 border-primary/20 border-dashed rounded-[2rem] flex flex-col items-center">
                      <MapIcon size={32} className="text-primary mb-2" />
                      <p className="text-[10px] text-center text-slate-500">Plan delivery route using existing farm tracks.</p>
                   </div>
                </div>
                <Button className="w-full py-6 rounded-2xl font-black mt-auto">DEPLOY ROBOT</Button>
             </div>
          </div>
        )
      case 1:
        return (
          <div className="w-full h-full relative">
            <RobotCanvas attachment="cargo" />
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
               <motion.div
                 animate={{ y: [50, 0], opacity: [0, 1] }}
                 className="bg-white dark:bg-zinc-900 p-4 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 flex items-center gap-4"
               >
                 <div className="w-12 h-12 bg-slate-100 dark:bg-zinc-800 rounded-xl flex items-center justify-center">
                    <Package className="text-primary" />
                 </div>
                 <div>
                    <p className="text-xs font-black uppercase tracking-widest">Load Status</p>
                    <p className="text-lg font-bold">40kg Crates</p>
                 </div>
               </motion.div>
            </div>
          </div>
        )
      case 2:
        return (
          <div className="w-full h-full bg-slate-50 dark:bg-zinc-950 p-12">
            <div className="w-full h-full border-2 border-slate-200 dark:border-slate-800 rounded-[3rem] relative bg-white dark:bg-zinc-900 overflow-hidden">
               {/* Map View */}
               <div className="absolute inset-0 opacity-10">
                  <div className="absolute top-1/4 left-1/4 w-1/2 h-1/2 border-4 border-slate-500 rounded-full" />
                  <div className="absolute top-0 bottom-0 left-1/2 w-1 bg-slate-500" />
               </div>

               {/* Animated Route */}
               <svg className="w-full h-full">
                  <path d="M 100,100 Q 200,100 200,200 T 300,300" stroke="#10b981" strokeWidth="4" fill="none" opacity="0.2" />
                  <motion.circle
                    r="8" fill="#10b981"
                    animate={{ cx: [100, 300], cy: [100, 300] }}
                    transition={{ duration: 5, repeat: Infinity }}
                  />
               </svg>

               <div className="absolute top-8 left-8">
                  <div className="flex items-center gap-3 p-4 bg-slate-900 text-white rounded-2xl">
                     <Navigation size={18} className="text-primary" />
                     <span className="text-xs font-bold uppercase tracking-widest">EN ROUTE TO STORAGE</span>
                  </div>
               </div>
            </div>
          </div>
        )
      case 3:
        return (
          <div className="w-full h-full flex items-center justify-center bg-slate-50 dark:bg-zinc-950">
            <div className="text-center">
               <div className="w-24 h-24 bg-primary/10 rounded-[2rem] flex items-center justify-center mx-auto mb-6">
                  <CheckCircle2 size={40} className="text-primary" />
               </div>
               <h4 className="text-4xl font-black italic tracking-tighter mb-4">DELIVERY SUCCESSFUL</h4>
               <p className="text-slate-500 max-w-xs mx-auto">Material safely transported from field to storage area.</p>
               <div className="mt-8 flex justify-center gap-4">
                  <div className="p-4 bg-white dark:bg-zinc-900 rounded-2xl border border-slate-200 dark:border-slate-800 w-32">
                     <p className="text-[10px] font-black text-slate-500 uppercase">Weight</p>
                     <p className="text-xl font-bold">40kg</p>
                  </div>
                  <div className="p-4 bg-white dark:bg-zinc-900 rounded-2xl border border-slate-200 dark:border-slate-800 w-32">
                     <p className="text-[10px] font-black text-slate-500 uppercase">Time</p>
                     <p className="text-xl font-bold">12m</p>
                  </div>
               </div>
            </div>
          </div>
        )
      default:
        return null
    }
  }
}
