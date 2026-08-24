"use client"

import React from "react"
import { motion } from "framer-motion"
import { Shield, Target, Package, Zap, ChevronRight, Info } from "lucide-react"

export type AttachmentType = "none" | "plough" | "sprayer" | "cargo"

interface Attachment {
  id: AttachmentType
  name: string
  description: string
  icon: any
  price: string
  compatibility: string
}

const attachments: Attachment[] = [
  {
    id: "none",
    name: "Standard Chassis",
    description: "Base modular platform with sensor tower and battery core.",
    icon: Shield,
    price: "Included",
    compatibility: "All Environments"
  },
  {
    id: "plough",
    name: "Weed Removal Tool",
    description: "Precision mechanical weeding with adjustable row spacing.",
    icon: Target,
    price: "+ ₹45,000",
    compatibility: "Vegetable Crops"
  },
  {
    id: "sprayer",
    name: "Smart Sprayer",
    description: "Liquid tank with wide boom and targeted nozzle control.",
    icon: Zap,
    price: "+ ₹62,000",
    compatibility: "Orchards & Fields"
  },
  {
    id: "cargo",
    name: "Logistics Platform",
    description: "Heavy-duty flatbed for crate transport and material handling.",
    icon: Package,
    price: "+ ₹28,000",
    compatibility: "Farm Logistics"
  }
]

interface AttachmentSelectorProps {
  activeAttachment: AttachmentType
  onSelect: (id: AttachmentType) => void
}

export function AttachmentSelector({ activeAttachment, onSelect }: AttachmentSelectorProps) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between mb-2">
         <h4 className="text-sm font-black uppercase tracking-widest text-slate-500">Configure Platform</h4>
         <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-[10px] font-bold">
            <Info size={12} />
            Modular System
         </div>
      </div>

      <div className="space-y-3">
        {attachments.map((item) => (
          <motion.button
            key={item.id}
            whileHover={{ x: 4 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => onSelect(item.id)}
            className={`w-full text-left p-4 rounded-2xl border transition-all flex items-center gap-4 ${
              activeAttachment === item.id
                ? "bg-primary border-primary shadow-lg shadow-primary/20 text-black"
                : "bg-white dark:bg-zinc-900 border-slate-200 dark:border-slate-800 hover:border-primary/50"
            }`}
          >
            <div className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 ${
               activeAttachment === item.id ? "bg-black/10" : "bg-slate-100 dark:bg-zinc-800 text-primary"
            }`}>
              <item.icon size={24} />
            </div>

            <div className="flex-1 min-w-0">
               <div className="flex items-center justify-between mb-0.5">
                  <span className="font-bold text-sm truncate">{item.name}</span>
                  <span className={`text-[10px] font-black ${
                     activeAttachment === item.id ? "text-black/60" : "text-slate-400"
                  }`}>{item.price}</span>
               </div>
               <p className={`text-xs truncate ${
                  activeAttachment === item.id ? "text-black/70" : "text-slate-500"
               }`}>
                  {item.description}
               </p>
            </div>

            <ChevronRight size={16} className={activeAttachment === item.id ? "text-black" : "text-slate-300"} />
          </motion.button>
        ))}
      </div>

      <div className="mt-6 p-6 rounded-3xl bg-slate-900 text-white relative overflow-hidden group">
         <div className="relative z-10">
            <p className="text-[10px] font-black uppercase tracking-widest text-primary mb-2">Total System Value</p>
            <div className="flex items-baseline gap-2">
               <h5 className="text-3xl font-black italic tracking-tighter">₹4,20,000*</h5>
               <span className="text-[10px] text-slate-400 italic">*Estimated Base Price</span>
            </div>
            <button className="w-full mt-4 py-3 rounded-xl bg-primary text-black font-black text-xs uppercase tracking-widest hover:scale-[1.02] transition-transform">
               Request Custom Quote
            </button>
         </div>
         <div className="absolute top-0 right-0 w-32 h-32 bg-primary/20 blur-3xl rounded-full -translate-y-1/2 translate-x-1/2 group-hover:bg-primary/30 transition-colors" />
      </div>
    </div>
  )
}
