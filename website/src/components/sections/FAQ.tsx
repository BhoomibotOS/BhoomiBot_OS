"use client"

import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion"

const faqs = [
  {
    question: "What is BhoomiBot?",
    answer: "BhoomiBot is a modular robotic platform designed for agriculture, logistics, and field operations. It features an electric drive system and supports various attachments for different tasks."
  },
  {
    question: "Is BhoomiBot battery powered?",
    answer: "Yes, BhoomiBot is fully electric, powered by high-capacity LiFePO4 batteries designed for long-duration field operations."
  },
  {
    question: "How is BhoomiBot controlled?",
    answer: "It can be controlled remotely via the BhoomiBot mobile application using a smartphone or tablet. It also supports autonomous mission modes."
  },
  {
    question: "What agricultural tasks can it perform?",
    answer: "With the appropriate attachments, BhoomiBot can perform weeding, spraying, ploughing, crop monitoring, and material transportation."
  },
  {
    question: "Can attachments be changed easily?",
    answer: "Yes, the platform features a quick-release modular mounting system that allows tools to be swapped in a few minutes."
  },
  {
    question: "Is BhoomiBot autonomous?",
    answer: "BhoomiBot supports autonomous navigation for predefined missions. The level of autonomy depends on the sensor package and software configuration installed."
  },
  {
    question: "What safety features are available?",
    answer: "BhoomiBot includes hardware-level emergency braking, communication watchdog timers, and sensor-based obstacle detection to ensure safe operation."
  }
]

export function FAQ() {
  return (
    <section className="py-24 bg-white dark:bg-black">
      <div className="container mx-auto px-4 max-w-3xl">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-5xl font-black tracking-tighter mb-6">Frequently Asked Questions</h2>
          <p className="text-slate-600 dark:text-slate-400">
            Everything you need to know about the BhoomiBot platform.
          </p>
        </div>

        <Accordion type="single" collapsible className="w-full">
          {faqs.map((faq, i) => (
            <AccordionItem key={i} value={`item-${i}`} className="border-slate-100 dark:border-slate-800">
              <AccordionTrigger className="text-left font-bold text-lg py-6 hover:no-underline hover:text-primary">
                {faq.question}
              </AccordionTrigger>
              <AccordionContent className="text-slate-500 dark:text-slate-400 text-base leading-relaxed pb-6">
                {faq.answer}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      </div>
    </section>
  )
}
