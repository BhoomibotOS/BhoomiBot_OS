import * as React from "react"
import Image from "next/image"
import Link from "next/link"
import { ArrowLeft, Calendar, User, Share2 } from "lucide-react"

export const runtime = 'edge';

export default function BlogPost({ params }: { params: { slug: string } }) {
  return (
    <main className="pt-32 pb-24 min-h-screen bg-white dark:bg-black">
      <div className="container mx-auto px-4">
        <div className="max-w-3xl mx-auto">
          <Link
            href="/blog"
            className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-widest text-slate-500 hover:text-primary transition-colors mb-12"
          >
            <ArrowLeft size={14} /> Back to Blog
          </Link>

          <div className="mb-12">
            <h1 className="text-4xl md:text-6xl font-black tracking-tighter mb-8 leading-tight">
              BhoomiBot: The Future of Autonomous Farming in India
            </h1>

            <div className="flex flex-wrap items-center gap-6 py-6 border-y border-slate-100 dark:border-slate-800">
               <div className="flex items-center gap-3">
                 <div className="w-10 h-10 rounded-full bg-slate-200" />
                 <div>
                   <p className="text-xs font-black uppercase tracking-widest">SW Architect</p>
                   <p className="text-[10px] text-slate-500 font-bold">Systems Expert</p>
                 </div>
               </div>
               <div className="flex items-center gap-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                 <Calendar size={12} /> Aug 20, 2026
               </div>
               <div className="ml-auto">
                 <button className="p-2 rounded-full border border-slate-200 hover:bg-slate-50 transition-colors">
                    <Share2 size={16} />
                 </button>
               </div>
            </div>
          </div>

          <div className="relative h-[400px] md:h-[600px] rounded-[3rem] overflow-hidden mb-12 shadow-2xl">
             <Image
               src="/robots/blog-1.jpg"
               alt="Blog Header"
               fill
               className="object-cover"
             />
          </div>

          <article className="prose prose-lg dark:prose-invert max-w-none text-slate-600 dark:text-slate-400 leading-relaxed">
            <p className="text-xl font-bold text-slate-900 dark:text-white mb-8">
              Agricultural automation is no longer a luxury but a necessity in the Indian context. As labor availability declines, robots like BhoomiBot are stepping in to bridge the gap.
            </p>
            <p>
              Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
            </p>
            <h2 className="text-2xl font-black text-slate-900 dark:text-white mt-12 mb-6 tracking-tight">The Modular Revolution</h2>
            <p>
              Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
            </p>
            <ul className="space-y-4 my-8">
              <li className="flex gap-4">
                <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0 mt-2" />
                <span>Autonomous weeding with precision vision.</span>
              </li>
              <li className="flex gap-4">
                <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0 mt-2" />
                <span>Targeted spraying reducing chemical waste by 60%.</span>
              </li>
              <li className="flex gap-4">
                <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0 mt-2" />
                <span>Heavy duty logistics handling up to 100kg cargo.</span>
              </li>
            </ul>
          </article>
        </div>
      </div>
    </main>
  )
}
