"use client"

import * as React from "react"
import { motion } from "framer-motion"
import Image from "next/image"

const images = [
  {
    src: "/robots/gallery-1.jpg",
    alt: "BhoomiBot in the field",
    title: "Field Operation"
  },
  {
    src: "/robots/gallery-2.jpg",
    alt: "BhoomiBot with weeding attachment",
    title: "Precision Weeding"
  },
  {
    src: "/robots/gallery-3.jpg",
    alt: "BhoomiBot logistics configuration",
    title: "Farm Logistics"
  },
  {
    src: "/robots/gallery-4.jpg",
    alt: "BhoomiBot close-up sensor stack",
    title: "AI Sensor Suite"
  },
  {
    src: "/robots/gallery-5.jpg",
    alt: "BhoomiBot night operation",
    title: "24/7 Autonomy"
  },
  {
    src: "/robots/gallery-6.jpg",
    alt: "BhoomiBot team testing",
    title: "Engineering Excellence"
  }
]

export function ImageGallery() {
  return (
    <section id="gallery" className="py-24 bg-white dark:bg-black">
      <div className="container mx-auto px-4">
        <div className="flex flex-col items-center text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Showcase
          </div>
          <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">Product Gallery</h2>
          <p className="text-slate-600 dark:text-slate-400 max-w-2xl text-lg">
            A closer look at BhoomiBot's design and real-world field applications.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {images.map((image, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="group relative aspect-square rounded-[2rem] overflow-hidden shadow-xl"
            >
              <Image
                src={image.src}
                alt={image.alt}
                fill
                className="object-cover transition-transform duration-500 group-hover:scale-110"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end p-8">
                <div>
                  <h3 className="text-white text-xl font-bold">{image.title}</h3>
                  <p className="text-primary text-sm font-medium">{image.alt}</p>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  )
}
