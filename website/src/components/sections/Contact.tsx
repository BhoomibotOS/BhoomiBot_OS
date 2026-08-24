"use client"

import * as React from "react"
import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Send, Linkedin, Mail, Phone, MapPin } from "lucide-react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"

const contactSchema = z.object({
  name: z.string().min(2, "Name too short"),
  email: z.string().email("Invalid email"),
  company: z.string().min(1, "Required"),
  application: z.string().min(1, "Please select an application"),
  message: z.string().min(10, "Message too short")
})

type ContactFormValues = z.infer<typeof contactSchema>

export function Contact() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset
  } = useForm<ContactFormValues>({
    resolver: zodResolver(contactSchema),
    defaultValues: {
      name: "",
      email: "",
      company: "",
      application: "Agriculture",
      message: ""
    }
  })

  const onSubmit = async (data: ContactFormValues) => {
    // Simulate API call
    console.log("Form Data:", data)
    await new Promise(resolve => setTimeout(resolve, 1000))
    alert("Message sent successfully!")
    reset()
  }

  return (
    <section id="contact" className="py-24 bg-white dark:bg-black overflow-hidden">
      <div className="container mx-auto px-4">
        <div className="max-w-6xl mx-auto bg-slate-900 rounded-[3rem] overflow-hidden shadow-2xl relative">
          <div className="grid grid-cols-1 lg:grid-cols-2">
            <div className="p-12 lg:p-20 text-white flex flex-col justify-between">
              <div>
                <h2 className="text-4xl md:text-6xl font-black tracking-tighter mb-6">Ready to Explore BhoomiBot?</h2>
                <p className="text-slate-400 text-lg mb-10 leading-relaxed">
                  Schedule a demonstration or speak with our engineering team to see how BhoomiBot can transform your operations.
                </p>

                <div className="space-y-8">
                  <div className="flex items-center gap-4 group">
                    <div className="w-12 h-12 rounded-2xl bg-primary/20 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                      <Mail size={20} />
                    </div>
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">Email Us</p>
                      <span className="font-bold text-lg">info@bhoomibot.com</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 group">
                    <div className="w-12 h-12 rounded-2xl bg-primary/20 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                      <Linkedin size={20} />
                    </div>
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">LinkedIn</p>
                      <a href="https://linkedin.com/company/bhoomibot" target="_blank" rel="noopener noreferrer" className="font-bold text-lg hover:text-primary transition-colors">
                        BhoomiBot AI Labs
                      </a>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 group">
                    <div className="w-12 h-12 rounded-2xl bg-primary/20 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-black transition-all">
                      <MapPin size={20} />
                    </div>
                    <div>
                      <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">Location</p>
                      <span className="font-bold text-lg">Ecity phase 1, Bengaluru, Karnataka, 560100</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-12 p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-sm">
                <h4 className="font-bold text-primary mb-2 italic">"Cultivating Intelligence"</h4>
                <p className="text-xs text-slate-400">Join our journey towards autonomous agricultural excellence.</p>
              </div>
            </div>

            <div className="bg-white p-12 lg:p-20">
              <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Label htmlFor="name" className="text-slate-900 font-bold uppercase text-[10px] tracking-widest">Full Name</Label>
                    <Input
                      id="name"
                      {...register("name")}
                      placeholder="Rahul Sharma"
                      className={`rounded-xl border-slate-200 focus:border-primary ${errors.name ? 'border-red-500' : ''}`}
                    />
                    {errors.name && <p className="text-[10px] text-red-500 font-bold">{errors.name.message}</p>}
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="company" className="text-slate-900 font-bold uppercase text-[10px] tracking-widest">Company / Farm</Label>
                    <Input
                      id="company"
                      {...register("company")}
                      placeholder="Green Fields Agri"
                      className={`rounded-xl border-slate-200 focus:border-primary ${errors.company ? 'border-red-500' : ''}`}
                    />
                    {errors.company && <p className="text-[10px] text-red-500 font-bold">{errors.company.message}</p>}
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email" className="text-slate-900 font-bold uppercase text-[10px] tracking-widest">Email Address</Label>
                  <Input
                    id="email"
                    type="email"
                    {...register("email")}
                    placeholder="rahul@example.com"
                    className={`rounded-xl border-slate-200 focus:border-primary ${errors.email ? 'border-red-500' : ''}`}
                  />
                  {errors.email && <p className="text-[10px] text-red-500 font-bold">{errors.email.message}</p>}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="application" className="text-slate-900 font-bold uppercase text-[10px] tracking-widest">Primary Application</Label>
                  <select
                    id="application"
                    {...register("application")}
                    className="w-full h-10 px-4 py-2 rounded-xl border border-slate-200 focus:border-primary outline-none transition-colors bg-white appearance-none text-sm"
                  >
                    <option value="Agriculture">Agriculture</option>
                    <option value="Weeding">Weeding</option>
                    <option value="Spraying">Spraying</option>
                    <option value="Material Handling">Material Handling</option>
                    <option value="Autonomous Operation">Autonomous Operation</option>
                    <option value="Other">Other</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="message" className="text-slate-900 font-bold uppercase text-[10px] tracking-widest">Message</Label>
                  <textarea
                    id="message"
                    rows={4}
                    {...register("message")}
                    placeholder="Tell us about your requirements..."
                    className={`w-full px-4 py-3 rounded-xl border border-slate-200 focus:border-primary outline-none transition-colors resize-none text-sm ${errors.message ? 'border-red-500' : ''}`}
                  />
                  {errors.message && <p className="text-[10px] text-red-500 font-bold">{errors.message.message}</p>}
                </div>

                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full py-8 rounded-2xl text-lg font-black uppercase tracking-widest shadow-xl shadow-primary/20 hover:scale-[1.02] transition-transform"
                >
                  {isSubmitting ? "Sending..." : "Request a Demo"}
                </Button>
              </form>
            </div>
          </div>

          {/* Background decoration */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-primary/10 blur-[100px] -z-10 rounded-full" />
        </div>
      </div>
    </section>
  )
}
