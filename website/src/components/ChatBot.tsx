"use client"

import * as React from "react"
import { motion, AnimatePresence } from "framer-motion"
import { MessageSquare, X, Send, Bot, User, Sparkles } from "lucide-react"
import { Button } from "@/components/ui/button"

interface Message {
  role: 'bot' | 'user'
  text: string
}

export function ChatBot() {
  const [isOpen, setIsOpen] = React.useState(false)
  const [messages, setMessages] = React.useState<Message[]>([
    { role: 'bot', text: 'Hello! I am BhoomiAI. How can I help you with your robotic field operations today?' }
  ])
  const [input, setInput] = React.useState('')
  const scrollRef = React.useRef<HTMLDivElement>(null)

  const handleSend = () => {
    if (!input.trim()) return

    const newMessages = [...messages, { role: 'user', text: input } as Message]
    setMessages(newMessages)
    setInput('')

    // Simulate AI Response
    setTimeout(() => {
      let response = "That's a great question about BhoomiBot! I'm currently in training, but I can tell you that our modular platform supports weeding, spraying, and logistics."

      if (input.toLowerCase().includes('battery')) {
        response = "BhoomiBot features a high-density Li-ion pack providing 8-12 hours of operation depending on the attachment and terrain."
      } else if (input.toLowerCase().includes('price') || input.toLowerCase().includes('cost')) {
        response = "Our base platform starts at approximately ₹4.2 Lakhs. We recommend using the ROI Calculator on our site for a detailed economic analysis."
      }

      setMessages(prev => [...prev, { role: 'bot', text: response }])
    }, 1000)
  }

  React.useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages])

  return (
    <div className="fixed bottom-6 left-6 z-[200]">
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 20, transformOrigin: 'bottom left' }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            className="mb-4 w-[350px] md:w-[400px] h-[500px] bg-white dark:bg-zinc-900 rounded-[2.5rem] border border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col overflow-hidden"
          >
            {/* Header */}
            <div className="p-6 bg-slate-900 text-white flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center text-primary">
                  <Bot size={20} />
                </div>
                <div>
                  <h4 className="text-sm font-bold">BhoomiAI</h4>
                  <div className="flex items-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
                    <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">Always Active</span>
                  </div>
                </div>
              </div>
              <button onClick={() => setIsOpen(false)} className="text-slate-400 hover:text-white transition-colors">
                <X size={20} />
              </button>
            </div>

            {/* Messages */}
            <div ref={scrollRef} className="flex-1 overflow-y-auto p-6 space-y-4 bg-slate-50 dark:bg-zinc-950/50">
              {messages.map((msg, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: msg.role === 'bot' ? -10 : 10 }}
                  animate={{ opacity: 1, x: 0 }}
                  className={`flex ${msg.role === 'bot' ? 'justify-start' : 'justify-end'}`}
                >
                  <div className={`max-w-[80%] p-4 rounded-2xl text-sm ${
                    msg.role === 'bot'
                      ? 'bg-white dark:bg-zinc-800 text-slate-800 dark:text-slate-200 shadow-sm border border-slate-100 dark:border-slate-700'
                      : 'bg-primary text-black font-medium shadow-lg shadow-primary/10'
                  }`}>
                    {msg.text}
                  </div>
                </motion.div>
              ))}
            </div>

            {/* Quick Actions */}
            <div className="px-6 py-2 flex gap-2 overflow-x-auto bg-slate-50 dark:bg-zinc-950/50 no-scrollbar">
               {['Battery Info', 'Price Quote', 'Mission Planning'].map((text) => (
                 <button
                  key={text}
                  onClick={() => setInput(text)}
                  className="whitespace-nowrap px-3 py-1 rounded-full border border-slate-200 dark:border-slate-800 bg-white dark:bg-zinc-900 text-[10px] font-bold text-slate-500 hover:border-primary hover:text-primary transition-all"
                 >
                   {text}
                 </button>
               ))}
            </div>

            {/* Input */}
            <div className="p-6 bg-white dark:bg-zinc-900 border-t border-slate-100 dark:border-slate-800">
              <form
                className="relative"
                onSubmit={(e) => { e.preventDefault(); handleSend(); }}
              >
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder="Ask about BhoomiBot..."
                  className="w-full pl-4 pr-12 py-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-zinc-950 outline-none focus:border-primary transition-colors text-sm"
                />
                <button
                  type="submit"
                  className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-8 rounded-lg bg-primary text-black flex items-center justify-center hover:scale-105 transition-transform"
                >
                  <Send size={14} />
                </button>
              </form>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <motion.button
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        onClick={() => setIsOpen(!isOpen)}
        className="w-16 h-16 rounded-full bg-slate-900 text-white shadow-2xl flex items-center justify-center relative group overflow-hidden border-2 border-primary/20"
      >
        <div className="absolute inset-0 bg-primary opacity-0 group-hover:opacity-10 transition-opacity" />
        {isOpen ? <X size={24} /> : (
          <>
            <MessageSquare size={24} className="group-hover:scale-110 transition-transform text-primary" />
            <div className="absolute top-0 right-0 w-4 h-4 bg-primary border-4 border-slate-900 rounded-full" />
          </>
        )}
      </motion.button>
    </div>
  )
}
