"use client"

import { motion } from "framer-motion"
import { Check, X } from "lucide-react"

const comparisonData = [
  { feature: "Manual Effort", manual: "High (Physical labor)", bhoomi: "Minimal (Assisted/Auto)" },
  { feature: "Repetitive Work", manual: "Tiring & Inconsistent", bhoomi: "Consistent & Precision-led" },
  { feature: "Operator Involvement", manual: "Continuous presence", bhoomi: "Supervisory / Remote" },
  { feature: "Tool Flexibility", manual: "Limited to single use", bhoomi: "Modular / Multi-purpose" },
  { feature: "Remote Monitoring", manual: "Not possible", bhoomi: "Real-time via App" },
  { feature: "Operational Data", manual: "None / Manual logs", bhoomi: "Automatic digital logs" },
]

export function Comparison() {
  return (
    <section className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Modernizing Field Operations</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
            A direct comparison between traditional manual methods and BhoomiBot-assisted operations.
          </p>
        </div>

        <div className="max-w-4xl mx-auto overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 dark:border-slate-800">
                <th className="py-6 px-4 font-black uppercase text-xs tracking-widest text-slate-400">Feature</th>
                <th className="py-6 px-4 font-black uppercase text-xs tracking-widest text-slate-400 text-center">Traditional Manual</th>
                <th className="py-6 px-4 font-black uppercase text-xs tracking-widest text-primary text-center">BhoomiBot Assisted</th>
              </tr>
            </thead>
            <tbody>
              {comparisonData.map((row, i) => (
                <tr key={i} className="border-b border-slate-50 dark:border-slate-900 hover:bg-slate-50/50 dark:hover:bg-zinc-900/50 transition-colors">
                  <td className="py-6 px-4 font-bold text-slate-700 dark:text-slate-300">{row.feature}</td>
                  <td className="py-6 px-4 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-400 text-sm">
                      <X size={14} className="text-red-500" />
                      {row.manual}
                    </div>
                  </td>
                  <td className="py-6 px-4 text-center">
                    <div className="inline-flex items-center gap-2 text-slate-900 dark:text-white font-bold text-sm">
                      <Check size={14} className="text-primary" />
                      {row.bhoomi}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
