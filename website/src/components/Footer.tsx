"use client"

import Link from "next/link"
import Image from "next/image"

export function Footer() {
  const links = [
    { name: "Home", href: "/#home" },
    { name: "Live Console", href: "/live" },
    { name: "Services", href: "/#services" },
    { name: "About", href: "/#about" },
    { name: "Contact", href: "/#contact" },
  ]

  const legal = [
    { name: "Privacy Policy", href: "#" },
    { name: "Terms of Service", href: "#" },
  ]

  return (
    <footer className="bg-slate-900 text-white py-16 border-t border-white/10">
      <div className="w-full px-4 sm:px-6 md:px-8 lg:px-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
          <div className="space-y-6">
            <Link href="/" className="flex items-center space-x-3">
              <Image
                src="/robots/web_icon2.png"
                alt="BhoomiBot Logo"
                width={52}
                height={52}
                className="w-10 h-10 sm:w-12 sm:h-12 md:w-14 md:h-14 object-contain shrink-0"
              />
              <span className="text-2xl sm:text-3xl md:text-4xl font-black tracking-tighter text-white">
                BhoomiBot <span className="text-primary">AI Labs</span>
              </span>
            </Link>
            <p className="text-slate-400 leading-relaxed">
              Building practical robotic systems that reduce repetitive human effort and make automation accessible for real-world environments.
            </p>
          </div>

          <div>
            <h4 className="font-bold mb-6 text-slate-200 uppercase tracking-widest text-xs">Navigation</h4>
            <ul className="space-y-4">
              {links.map((link) => (
                <li key={link.name}>
                  <Link href={link.href} className="text-slate-400 hover:text-primary transition-colors">
                    {link.name}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-bold mb-6 text-slate-200 uppercase tracking-widest text-xs">Legal</h4>
            <ul className="space-y-4">
              {legal.map((link) => (
                <li key={link.name}>
                  <Link href={link.href} className="text-slate-400 hover:text-primary transition-colors">
                    {link.name}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-bold mb-6 text-slate-200 uppercase tracking-widest text-xs">Contact Us</h4>
            <div className="space-y-4 text-slate-400">
              <p>
                <span className="block font-bold text-slate-300">Email</span>
                info@bhoomibot.com
              </p>
              <p>
                <span className="block font-bold text-slate-300">Location</span>
                Electronic city phase 1, Bengaluru, India ,560100
              </p>
            </div>
          </div>
        </div>

        <div className="mt-16 pt-8 border-t border-white/5 flex flex-col md:flex-row justify-between items-center gap-4 text-xs text-slate-500">
          <p>© {new Date().getFullYear()} BhoomiBot AI Labs. All rights reserved.</p>
          <p className="italic">Cultivating Intelligence in the Real World.</p>
        </div>
      </div>
    </footer>
  )
}
