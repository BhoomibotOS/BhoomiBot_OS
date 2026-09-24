import { Navbar } from "@/components/Navbar"
import { Hero } from "@/components/sections/Hero"
import { MeetBhoomiBot } from "@/components/sections/MeetBhoomiBot"
import { Technology } from "@/components/sections/Technology"
import { Specifications } from "@/components/sections/Specifications"
import { Agriculture } from "@/components/sections/Agriculture"
import { Logistics } from "@/components/sections/Logistics"
import { OperatingModes } from "@/components/sections/OperatingModes"
import { Safety } from "@/components/sections/Safety"
import { FAQ } from "@/components/sections/FAQ"
import { Contact } from "@/components/sections/Contact"
import { Footer } from "@/components/Footer"

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col">
      <Navbar />

      {/* 1. Introduction */}
      <div id="home">
        <Hero />
      </div>

      {/* 2. Hardware Spotlight */}
      <div id="robot" className="bg-slate-50 dark:bg-zinc-950 py-10">
        <MeetBhoomiBot />
        <Specifications />
      </div>

      {/* 3. Operational Logic */}
      <div id="technology">
        <Technology />
        <OperatingModes />
      </div>

      {/* 4. Use Cases */}
      <div id="agriculture">
        <Agriculture />
        <Logistics />
      </div>

      {/* 5. Support & Reliability */}
      <div id="about">
        <Safety />
        <FAQ />
      </div>

      <div id="contact">
        <Contact />
      </div>

      <Footer />
    </main>
  )
}
