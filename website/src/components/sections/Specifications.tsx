"use client"

import { motion } from "framer-motion"

const specifications = [
  { category: "Drive System", value: "Dual-Motor Differential Drive" },
  { category: "Motor", value: "High-torque Industrial Electric" },
  { category: "Battery", value: "48V 50Ah LiFePO4" },
  { category: "Chassis", value: "Reinforced All-Steel Frame" },
  { category: "Control Path", value: "Dual Path (Direct / Edge Relay)" },
  { category: "Ground Clearance", value: "Adjustable (Task Dependent)" },
  { category: "Operating Speed", value: "Up to 5 m/s (Software Capped)" },
  { category: "Communication", value: "Wi-Fi / 4G (CloudBridge v13)" },
  { category: "Sensors", value: "High-Precision GPS, IMU" },
  { category: "Camera", value: "FHD 1080p (Dual Path Switchable)" },
  { category: "OS Architecture", value: "BhoomiBot OS (VCU + Android)" },
  { category: "Attachments", value: "Standard Modular Quick-Release" },
]

export function Specifications() {
  return (
    <section className="py-16 sm:py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="w-full max-w-[1800px] mx-auto px-4 sm:px-8 lg:px-12 xl:px-16">
        <div className="text-center mb-12 sm:mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-4 sm:mb-6">Technical Specifications</h2>
          <p className="text-slate-600 dark:text-slate-400 text-base sm:text-lg">
            The core parameters of the BhoomiBot modular robotic platform.
          </p>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="bg-white dark:bg-zinc-900 rounded-3xl sm:rounded-[2.5rem] border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden"
        >
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 divide-y sm:divide-y-0 border-slate-100 dark:border-slate-800">
            {specifications.map((spec, i) => (
              <div
                key={spec.category}
                className="p-5 sm:p-8 flex flex-col justify-center border-b border-r border-slate-100 dark:border-slate-800"
              >
                <span className="text-[10px] sm:text-xs font-black uppercase tracking-widest text-primary mb-1 sm:mb-2">{spec.category}</span>
                <span className="text-base sm:text-xl font-bold text-slate-800 dark:text-slate-200">{spec.value}</span>
              </div>
            ))}
          </div>

            <div className="bg-slate-900 p-8 text-center">
              <p className="text-slate-400 text-sm italic">
                All specifications are based on standard platform configurations and are subject to minor variations.
              </p>
            </div>
          </motion.div>
        </div>
    </section>
  )
}
