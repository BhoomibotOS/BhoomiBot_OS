"use client"

import * as React from "react"
import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
  Legend
} from "recharts"
import { TrendingUp, Calculator, ArrowRight, Download } from "lucide-react"

export function ROICalculator() {
  const [area, setArea] = React.useState(10)
  const [laborRate, setLaborRate] = React.useState(450)
  const [workers, setWorkers] = React.useState(3)

  // Calculations
  const daysPerYear = 150 // Active farming days
  const annualLaborCost = area * laborRate * workers * (daysPerYear / 10) // Scaled estimate
  const annualMaintenance = 15000
  const annualEnergy = 8000
  const annualRoboticCost = annualMaintenance + annualEnergy + (annualLaborCost * 0.15) // 85% labor reduction
  const potentialSavings = annualLaborCost - annualRoboticCost
  const paybackMonths = Math.round((420000 / potentialSavings) * 12)

  const chartData = [
    { name: "Year 1", Manual: annualLaborCost, Robotic: 420000 + annualRoboticCost },
    { name: "Year 2", Manual: annualLaborCost * 2, Robotic: 420000 + (annualRoboticCost * 2) },
    { name: "Year 3", Manual: annualLaborCost * 3, Robotic: 420000 + (annualRoboticCost * 3) },
  ]

  return (
    <section id="roi" className="py-24 bg-slate-50 dark:bg-zinc-950 overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Economic Impact
          </div>
          <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">ROI Calculator</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl text-lg">
            Calculate your potential savings and see how quickly BhoomiBot pays for itself through increased efficiency and reduced labor costs.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-stretch">
          {/* Inputs Column */}
          <Card className="lg:col-span-4 p-8 bg-white dark:bg-zinc-900 border-slate-200 dark:border-slate-800 rounded-[2.5rem] shadow-xl">
            <div className="flex items-center gap-3 mb-8">
               <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
                  <Calculator size={20} />
               </div>
               <h3 className="text-xl font-bold">Parameters</h3>
            </div>

            <div className="space-y-8">
              <div className="space-y-4">
                <div className="flex justify-between">
                  <Label className="font-bold text-xs uppercase tracking-widest text-slate-500">Farm Area</Label>
                  <span className="text-sm font-black text-primary">{area} Acres</span>
                </div>
                <input
                  type="range" min="1" max="100" value={area}
                  onChange={(e) => setArea(Number(e.target.value))}
                  className="w-full h-1.5 bg-slate-100 dark:bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-primary"
                />
              </div>

              <div className="space-y-4">
                <div className="flex justify-between">
                  <Label className="font-bold text-xs uppercase tracking-widest text-slate-500">Daily Labor Rate</Label>
                  <span className="text-sm font-black text-primary">₹{laborRate}</span>
                </div>
                <input
                  type="range" min="200" max="1000" step="50" value={laborRate}
                  onChange={(e) => setLaborRate(Number(e.target.value))}
                  className="w-full h-1.5 bg-slate-100 dark:bg-zinc-800 rounded-lg appearance-none cursor-pointer accent-primary"
                />
              </div>

              <div className="space-y-4">
                 <Label className="font-bold text-xs uppercase tracking-widest text-slate-500">Labor Required (Manual)</Label>
                 <div className="flex items-center justify-between p-4 bg-slate-50 dark:bg-zinc-950 rounded-2xl border border-slate-100 dark:border-slate-800">
                    <button onClick={() => setWorkers(Math.max(1, workers - 1))} className="w-10 h-10 rounded-xl bg-white dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 flex items-center justify-center font-bold hover:bg-primary hover:text-black transition-colors">-</button>
                    <div className="text-center">
                       <span className="text-2xl font-black">{workers}</span>
                       <p className="text-[10px] text-slate-500 font-bold uppercase tracking-tighter">Workers</p>
                    </div>
                    <button onClick={() => setWorkers(workers + 1)} className="w-10 h-10 rounded-xl bg-white dark:bg-zinc-900 border border-slate-200 dark:border-slate-800 flex items-center justify-center font-bold hover:bg-primary hover:text-black transition-colors">+</button>
                 </div>
              </div>
            </div>

            <div className="mt-12 p-6 rounded-3xl bg-primary/5 border border-primary/10">
               <div className="flex items-center gap-2 text-primary mb-2">
                  <TrendingUp size={16} />
                  <span className="text-xs font-bold uppercase tracking-widest">Efficiency Insight</span>
               </div>
               <p className="text-xs text-slate-500 leading-relaxed italic">
                 With {area} acres, BhoomiBot can reduce your manual labor dependency by up to 85%, allowing you to reallocate resources to higher-value tasks.
               </p>
            </div>
          </Card>

          {/* Results Column */}
          <div className="lg:col-span-8 flex flex-col gap-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                { label: "Manual Cost/Yr", value: `₹${Math.round(annualLaborCost/1000)}K`, sub: "Full Labor" },
                { label: "BhoomiBot Cost/Yr", value: `₹${Math.round(annualRoboticCost/1000)}K`, sub: "Energy + Maint", highlight: true },
                { label: "Payback Period", value: `${paybackMonths} Months`, sub: "Est. Recovery" }
              ].map((stat, i) => (
                <Card key={i} className={`p-8 rounded-[2rem] border-slate-200 dark:border-slate-800 ${stat.highlight ? 'bg-primary text-black' : 'bg-white dark:bg-zinc-900'}`}>
                   <p className={`text-[10px] font-black uppercase tracking-widest mb-1 ${stat.highlight ? 'text-black/60' : 'text-slate-500'}`}>{stat.label}</p>
                   <p className="text-3xl font-black italic tracking-tighter">{stat.value}</p>
                   <p className={`text-[10px] font-bold mt-2 ${stat.highlight ? 'text-black/40' : 'text-slate-400'}`}>{stat.sub}</p>
                </Card>
              ))}
            </div>

            <Card className="flex-1 p-8 bg-slate-900 text-white border-none rounded-[3rem] shadow-2xl relative overflow-hidden">
               <div className="relative z-10 h-full flex flex-col">
                  <div className="flex justify-between items-start mb-8">
                     <div>
                        <h4 className="text-2xl font-black tracking-tighter italic">Cumulative Cost Comparison</h4>
                        <p className="text-xs text-slate-400">Manual Labor vs. BhoomiBot Ownership (3 Year Outlook)</p>
                     </div>
                     <Button variant="outline" size="sm" className="rounded-xl border-white/20 text-white hover:bg-white hover:text-black">
                        <Download size={16} className="mr-2" /> PDF Report
                     </Button>
                  </div>

                  <div className="flex-1 min-h-[300px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                        <XAxis
                          dataKey="name"
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: '#64748b', fontSize: 12, fontWeight: 'bold' }}
                        />
                        <YAxis hide />
                        <Tooltip
                          cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                          contentStyle={{ backgroundColor: '#0f172a', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '1rem' }}
                        />
                        <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px' }} />
                        <Bar dataKey="Manual" fill="#334155" radius={[10, 10, 0, 0]} name="Manual Labor Cost" />
                        <Bar dataKey="Robotic" fill="#10b981" radius={[10, 10, 0, 0]} name="BhoomiBot Total Cost" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>

                  <div className="mt-8 pt-8 border-t border-white/5 flex flex-col md:flex-row justify-between items-center gap-6">
                     <div className="flex items-center gap-6">
                        <div>
                           <p className="text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Total 3-Year Savings</p>
                           <p className="text-4xl font-black text-primary tracking-tighter italic">₹{Math.round((annualLaborCost * 3 - (420000 + annualRoboticCost * 3))/1000)}K</p>
                        </div>
                     </div>
                     <Button className="rounded-2xl px-8 py-6 font-black bg-primary text-black shadow-xl shadow-primary/20 hover:scale-[1.05] transition-transform">
                        Schedule a Demo <ArrowRight size={18} className="ml-2" />
                     </Button>
                  </div>
               </div>

               {/* Design decoration */}
               <div className="absolute top-0 right-0 w-96 h-96 bg-primary/5 blur-[120px] rounded-full -translate-y-1/2 translate-x-1/2" />
            </Card>
          </div>
        </div>
      </div>
    </section>
  )
}
