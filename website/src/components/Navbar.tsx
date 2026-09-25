"use client"

import * as React from "react"
import Link from "next/link"
import Image from "next/image"
import { Menu, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const navLinks = [
  { name: "Home", href: "/" },
  { name: "Live Console", href: "/live" },
  { name: "Services", href: "/#services" },
  { name: "About", href: "/#about" },
]

export function Navbar() {
  const [isOpen, setIsOpen] = React.useState(false)
  const [scrolled, setScrolled] = React.useState(false)

  React.useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  return (
    <nav
      className={cn(
        "fixed top-0 w-full z-50 transition-all duration-300 border-b",
        scrolled
          ? "bg-background/90 backdrop-blur-md border-border py-2.5 shadow-sm"
          : "bg-transparent border-transparent py-4"
      )}
    >
      <div className="w-full px-4 sm:px-6 md:px-8 lg:px-12 flex items-center justify-between">
        <Link href="/" className="flex items-center space-x-2 sm:space-x-3">
          <Image
            src="/robots/web_icon2.png"
            alt="BhoomiBot Logo"
            width={48}
            height={48}
            className="w-9 h-9 sm:w-11 sm:h-11 md:w-12 md:h-12 object-contain shrink-0"
          />
          <span className="text-xl sm:text-2xl md:text-3xl font-black tracking-tighter text-foreground">
            BhoomiBot <span className="text-primary">AI Labs</span>
          </span>
        </Link>

        {/* Desktop Nav */}
        <div className="hidden md:flex items-center space-x-8">
          {navLinks.map((link) => (
            <Link
              key={link.name}
              href={link.href}
              className="text-lg font-bold text-foreground/90 hover:text-primary transition-colors"
            >
              {link.name}
            </Link>
          ))}
          <Button size="default" className="text-sm font-bold px-5 py-2">
            Request a Demo
          </Button>
        </div>

        {/* Mobile Toggle */}
        <button
          className="md:hidden text-foreground p-1.5"
          onClick={() => setIsOpen(!isOpen)}
          aria-label="Toggle Menu"
        >
          {isOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Mobile Nav */}
      {isOpen && (
        <div className="md:hidden bg-background border-b border-border px-6 py-6 space-y-5 flex flex-col items-center animate-in slide-in-from-top duration-300">
          {navLinks.map((link) => (
            <Link
              key={link.name}
              href={link.href}
              className="text-xl font-bold text-foreground hover:text-primary transition-colors"
              onClick={() => setIsOpen(false)}
            >
              {link.name}
            </Link>
          ))}
          <Button className="w-full text-base font-bold py-2.5">
            Request a Demo
          </Button>
        </div>
      )}
    </nav>
  )
}
