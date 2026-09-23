import { Navbar } from "@/components/Navbar"
import { Footer } from "@/components/Footer"
import { RemoteControl } from "@/components/sections/RemoteControl"

export default function LivePage() {
  return (
    <main className="flex min-h-screen flex-col pt-20">
      <Navbar />
      <div className="flex-1">
        <RemoteControl />
      </div>
      <Footer />
    </main>
  )
}
