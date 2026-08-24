import * as React from "react"
import { motion } from "framer-motion"
import Image from "next/image"
import Link from "next/link"
import { Calendar, User, ArrowRight, Tag } from "lucide-react"

export const runtime = 'edge';

const posts = [
  {
    title: "BhoomiBot: The Future of Autonomous Farming in India",
    slug: "future-of-autonomous-farming",
    excerpt: "How BhoomiBot is solving the labor shortage and precision gap in Indian agriculture through modular robotics.",
    date: "Aug 20, 2026",
    author: "SW Architect",
    category: "Agri-Tech",
    image: "/robots/blog-1.jpg"
  },
  {
    title: "Understanding 'Teach Once' Navigation Technology",
    slug: "understanding-teach-once-tech",
    excerpt: "A deep dive into the engineering behind our repeatable robotic workflows and row-following vision systems.",
    date: "Aug 15, 2026",
    author: "Systems Expert",
    category: "Engineering",
    image: "/robots/blog-2.jpg"
  },
  {
    title: "Optimizing Logistics with Autonomous Field Rovers",
    slug: "optimizing-farm-logistics",
    excerpt: "How modular cargo attachments are reducing operational costs by 40% for large-scale farm holdings.",
    date: "Aug 10, 2026",
    author: "Technical Expert",
    category: "Logistics",
    image: "/robots/blog-3.jpg"
  }
]

export default function BlogPage() {
  return (
    <main className="pt-32 pb-24 min-h-screen bg-slate-50 dark:bg-black">
      <div className="container mx-auto px-4">
        <div className="max-w-4xl mx-auto text-center mb-16">
          <div className="inline-block px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wider uppercase mb-4">
            Insights & Updates
          </div>
          <h1 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">BhoomiBot Labs Blog</h1>
          <p className="text-slate-600 dark:text-slate-400 text-lg">
            Engineering updates, field study results, and our vision for the future of autonomous robotics.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {posts.map((post) => (
            <div key={post.slug} className="group bg-white dark:bg-zinc-900 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 overflow-hidden shadow-xl hover:shadow-2xl transition-all hover:scale-[1.02]">
              <div className="relative h-64">
                <Image
                  src={post.image}
                  alt={post.title}
                  fill
                  className="object-cover group-hover:scale-105 transition-transform duration-500"
                />
                <div className="absolute top-4 left-4">
                   <span className="px-3 py-1 rounded-full bg-primary text-black text-[10px] font-black uppercase tracking-widest">
                     {post.category}
                   </span>
                </div>
              </div>

              <div className="p-8">
                <div className="flex items-center gap-4 text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">
                  <div className="flex items-center gap-1.5"><Calendar size={12} /> {post.date}</div>
                  <div className="flex items-center gap-1.5"><User size={12} /> {post.author}</div>
                </div>

                <h3 className="text-2xl font-black tracking-tighter mb-4 group-hover:text-primary transition-colors leading-tight">
                  <Link href={`/blog/${post.slug}`}>{post.title}</Link>
                </h3>

                <p className="text-slate-500 dark:text-slate-400 text-sm mb-6 leading-relaxed">
                  {post.excerpt}
                </p>

                <Link
                  href={`/blog/${post.slug}`}
                  className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary hover:gap-3 transition-all"
                >
                  Read More <ArrowRight size={14} />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </main>
  )
}
