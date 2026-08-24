"use client"

import { motion } from "framer-motion"

const apps = [
  { title: "Agriculture", img: "https://images.unsplash.com/photo-1523348837708-15d4a09cfac2?q=80&w=800" },
  { title: "Material Handling", img: "https://images.unsplash.com/photo-1580674285054-bed31e145f59?q=80&w=800" },
  { title: "Farm Logistics", img: "https://images.unsplash.com/photo-1605000797499-95a51c5269ae?q=80&w=800" },
  { title: "Warehouse Operations", img: "https://images.unsplash.com/photo-1553413077-190dd305871c?q=80&w=800" },
  { title: "Field Monitoring", img: "https://images.unsplash.com/photo-1594498653385-d5172b532c00?q=80&w=800" },
  { title: "Autonomous Patrol", img: "https://images.unsplash.com/photo-1560644269-58a1b60d5947?q=80&w=800" }
]

export function RealWorldApplications() {
  return (
    <section id="applications-grid" className="py-24 bg-white dark:bg-black">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Real-World Applications</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl mx-auto">
            BhoomiBot is built to perform in diverse environments, from open fields to structured warehouses.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {apps.map((app, index) => (
            <motion.div
              key={app.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="group relative h-80 rounded-[2.5rem] overflow-hidden shadow-lg"
            >
              <img src={app.img} alt={app.title} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
              <div className="absolute bottom-8 left-8">
                <h3 className="text-2xl font-black text-white italic tracking-tighter">{app.title}</h3>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
