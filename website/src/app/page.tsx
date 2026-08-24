import { Hero } from "@/components/sections/Hero"
import { MeetBhoomiBot } from "@/components/sections/MeetBhoomiBot"
import { Showcase } from "@/components/sections/Showcase"
import { MissionPlanner } from "@/components/sections/MissionPlanner"
import { ActionDemo } from "@/components/sections/ActionDemo"
import { WhyBhoomiBot } from "@/components/sections/WhyBhoomiBot"
import { Agriculture } from "@/components/sections/Agriculture"
import { Attachments } from "@/components/sections/Attachments"
import { Logistics } from "@/components/sections/Logistics"
import { RemoteControl } from "@/components/sections/RemoteControl"
import { OperatingModes } from "@/components/sections/OperatingModes"
import { Technology } from "@/components/sections/Technology"
import { Specifications } from "@/components/sections/Specifications"
import { HowItWorks } from "@/components/sections/HowItWorks"
import { Safety } from "@/components/sections/Safety"
import { RealWorldApplications } from "@/components/sections/RealWorldApplications"
import { CaseStudies } from "@/components/sections/CaseStudies"
import { ImageGallery } from "@/components/sections/ImageGallery"
import { Comparison } from "@/components/sections/Comparison"
import { ROICalculator } from "@/components/sections/ROICalculator"
import { FAQ } from "@/components/sections/FAQ"
import { About } from "@/components/sections/About"
import { Contact } from "@/components/sections/Contact"

export const runtime = 'edge';

export default function Home() {
  return (
    <main className="flex min-h-screen flex-col">
      <Hero />
      <MeetBhoomiBot />
      <Showcase />
      <ActionDemo />
      <MissionPlanner />
      <WhyBhoomiBot />
      <Agriculture />
      <Attachments />
      <Logistics />
      <RemoteControl />
      <OperatingModes />
      <Technology />
      <Specifications />
      <HowItWorks />
      <Safety />
      <RealWorldApplications />
      <CaseStudies />
      <ImageGallery />
      <Comparison />
      <ROICalculator />
      <FAQ />
      <About />
      <Contact />
    </main>
  )
}
