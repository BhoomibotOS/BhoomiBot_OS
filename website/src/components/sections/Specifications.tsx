"use client"

import { motion } from "framer-motion"

const specifications = [
  { category: "Drive System", value: "4WD Differential Drive" },
  { category: "Motor", value: "High-torque Electric Brushless" },
  { category: "Battery", value: "LiFePO4 Support" },
  { category: "Payload", value: "To be finalized" },
  { category: "Dimensions", value: "To be finalized" },
  { category: "Ground Clearance", value: "To be finalized" },
  { category: "Operating Speed", value: "To be finalized" },
  { category: "Communication", value: "Wi-Fi / 4G Support" },
  { category: "Sensors", value: "IMU, Ultrasonic, GPS" },
  { category: "Camera", value: "1080p HD Wide-angle" },
  { category: "Controller", value: "BhoomiBot OS (ESP32 Based)" },
  { category: "Attachments", value: "Modular Quick-Release" },
]

export function Specifications() {
  return (
    <section className="py-24 bg-slate-50 dark:bg-zinc-950/50">
      <div className="container mx-auto px-4">
        <div className="max-w-4xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Technical Specifications</h2>
            <p className="text-slate-600 dark:text-slate-400">
              The core parameters of the BhoomiBot modular robotic platform.
            </p>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="bg-white dark:bg-zinc-900 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 shadow-xl overflow-hidden"
          >
            <div className="grid grid-cols-1 md:grid-cols-2">
              {specifications.map((spec, i) => (
                <div
                  key={spec.category}
                  className={`p-8 flex flex-col justify-center border-b border-slate-100 dark:border-slate-800 ${i % 2 === 0 ? "md:border-r" : ""}`}
                >
                  <span className="text-xs font-black uppercase tracking-widest text-primary mb-2">{spec.category}</span>
                  <span className="text-xl font-bold text-slate-800 dark:text-slate-200">{spec.value}</span>
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
      </div>
    </section>
  )
}
