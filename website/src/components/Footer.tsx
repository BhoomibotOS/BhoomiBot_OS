"use client"

import Link from "next/link"

export function Footer() {
  const links = [
    { name: "Home", href: "#home" },
    { name: "Robot", href: "#robot" },
    { name: "Agriculture", href: "#agriculture" },
    { name: "Applications", href: "#applications" },
    { name: "Technology", href: "#technology" },
    { name: "About", href: "#about" },
    { name: "Contact", href: "#contact" },
  ]

  const legal = [
    { name: "Privacy Policy", href: "#" },
    { name: "Terms of Service", href: "#" },
  ]

  return (
    <footer className="bg-slate-900 text-white py-16 border-t border-white/10">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
          <div className="space-y-6">
            <Link href="/" className="flex items-center space-x-2">
              <span className="text-2xl font-black tracking-tighter text-white">
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
                Pune, Maharashtra, India
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
