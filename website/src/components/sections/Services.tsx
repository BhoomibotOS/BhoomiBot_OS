"use client"

import { motion } from "framer-motion"
import { Wrench, Cpu, Compass, ArrowRight, CheckCircle2, Shield, Layers, Radio, Bot } from "lucide-react"
import { Button } from "@/components/ui/button"
import Link from "next/link"

const serviceCategories = [
  {
    icon: Wrench,
    badge: "Hardware & CAD",
    title: "Mechanical Design & CAD Engineering",
    description: "End-to-end mechanical engineering for heavy-duty field robots, custom machinery, and precision automation tools.",
    highlights: [
      "3D CAD Modeling (SolidWorks / Fusion 360) & Sheet Metal Design",
      "Chassis & Heavy Structural Frame Engineering (Agricultural & Heavy Equipment)",
      "Robotic Hand / Gripper Design & End-of-Arm Tooling (EOAT)",
      "Weatherproof & Rugged Enclosures (IP65/IP67 rated for field ops)"
    ]
  },
  {
    icon: Cpu,
    badge: "Firmware & Electronics",
    title: "Embedded Systems & Firmware Development",
    description: "Industrial-grade control systems, firmware architecture, and robust communication protocols for real-time operation.",
    highlights: [
      "VCU (Vehicle Control Unit) & Motor Controller Programming (STM32 & High-End Controllers)",
      "CAN Bus, RS485, SPI & UART Industrial Communication Networks",
      "Low-Latency Telemetry & Cloud Bridge Integration",
      "Real-Time Sensor Fusion & Electronic Braking / Watchdog Systems"
    ]
  },
  {
    icon: Compass,
    badge: "Turnkey Robotics",
    title: "Mechatronics & Full-Stack Prototyping",
    description: "Transforming raw concepts into fully functional, tested, and field-ready mechatronic prototypes.",
    highlights: [
      "Rapid Hardware Prototyping (Concept to Working Field Model)",
      "Differential & Ackermann Drive Kinematics Implementation",
      "Custom Actuator Assembly & Power Electronics Integration",
      "On-Site Field Testing & System Calibration"
    ]
  }
]

export function Services() {
  return (
    <section id="services" className="py-24 bg-slate-950 text-white relative overflow-hidden">
      {/* Background Lighting & Grid Accent */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(34,197,94,0.08)_0,transparent_50%)] pointer-events-none" />
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1f293708_1px,transparent_1px),linear-gradient(to_bottom,#1f293708_1px,transparent_1px)] bg-[size:3rem_3rem] pointer-events-none" />

      <div className="container mx-auto px-4 relative z-10">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-black tracking-widest uppercase mb-4">
            <Bot size={14} />
            <span>B2B Engineering & R&D Services</span>
          </div>
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">
            Mechatronics & Embedded Solutions
          </h2>
          <p className="text-slate-400 max-w-3xl text-lg leading-relaxed">
            Leverage BhoomiBot's battle-tested R&D capabilities. We help OEMs, agri-tech startups, and industrial companies design, build, and deploy custom mechatronic systems.
          </p>
        </div>

        {/* Services Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {serviceCategories.map((service, index) => (
            <motion.div
              key={service.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.15, duration: 0.5 }}
              className="group relative bg-zinc-900/90 rounded-[2.5rem] p-8 border border-white/10 hover:border-primary/50 transition-all duration-300 flex flex-col justify-between shadow-2xl hover:shadow-primary/10"
            >
              <div>
                <div className="flex items-center justify-between mb-6">
                  <div className="w-14 h-14 rounded-2xl bg-primary/10 border border-primary/20 flex items-center justify-center text-primary group-hover:scale-110 transition-transform duration-300">
                    <service.icon size={28} />
                  </div>
                  <span className="text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-full bg-white/5 border border-white/10 text-slate-300">
                    {service.badge}
                  </span>
                </div>

                <h3 className="text-2xl font-black tracking-tight mb-3 group-hover:text-primary transition-colors">
                  {service.title}
                </h3>
                <p className="text-slate-400 text-sm leading-relaxed mb-6">
                  {service.description}
                </p>

                <div className="space-y-3 pt-4 border-t border-white/5 mb-8">
                  {service.highlights.map((item, i) => (
                    <div key={i} className="flex items-start gap-3">
                      <div className="w-5 h-5 rounded-full bg-primary/20 text-primary flex items-center justify-center shrink-0 mt-0.5">
                        <CheckCircle2 size={12} />
                      </div>
                      <span className="text-xs text-slate-300 leading-snug font-medium">
                        {item}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              <Button
                variant="outline"
                className="w-full justify-between font-bold text-xs uppercase tracking-widest rounded-xl border-white/10 hover:border-primary hover:bg-primary hover:text-black transition-all"
                asChild
              >
                <Link href="#contact">
                  <span>Consult Engineering Team</span>
                  <ArrowRight size={14} />
                </Link>
              </Button>
            </motion.div>
          ))}
        </div>

        {/* Bottom Banner */}
        <div className="mt-16 p-10 rounded-[2.5rem] bg-gradient-to-r from-zinc-900 via-zinc-950 to-zinc-900 border border-white/10 relative overflow-hidden flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="max-w-2xl">
            <h3 className="text-2xl md:text-3xl font-black tracking-tight mb-2">
              Need Custom Hardware or Prototyping?
            </h3>
            <p className="text-slate-400 text-sm leading-relaxed">
              From SolidWorks CAD design to custom STM32 VCU controllers and CAN Bus networks, we bring your industrial hardware ideas to life.
            </p>
          </div>
          <Button size="lg" className="font-bold shrink-0 px-8 py-6 rounded-2xl text-sm uppercase tracking-widest shadow-xl" asChild>
            <Link href="#contact">
              Request R&D Proposal
            </Link>
          </Button>
        </div>
      </div>
    </section>
  )
}
