import React, { Suspense } from "react"
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import "./globals.css"
import { Navbar } from "@/components/Navbar"
import { Footer } from "@/components/Footer"
import { ChatBot } from "@/components/ChatBot"
import { Analytics } from "@/components/Analytics"
import { cn } from "@/lib/utils"

const inter = Inter({ subsets: ["latin"] })

export const metadata: Metadata = {
  title: "BhoomiBot AI Labs | Smarter Robotics for Agriculture & Logistics",
  description: "BhoomiBot is a modular robotic platform designed for agriculture, material handling and autonomous field operations in India.",
  keywords: "Agricultural robot India, Farming robot, Autonomous agricultural robot, Electric agricultural robot, Farm robotics, BhoomiBot",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className="scroll-smooth" suppressHydrationWarning>
      <body className={cn(inter.className, "min-h-screen bg-background text-foreground antialiased")}>
        <Suspense fallback={null}>
          <Analytics />
        </Suspense>
        <Navbar />
        {children}
        <ChatBot />
        <Footer />
      </body>
    </html>
  )
}
