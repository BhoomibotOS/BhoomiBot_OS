"use client"

import { useEffect } from "react"
import { usePathname, useSearchParams } from "next/navigation"

export function Analytics() {
  const pathname = usePathname()
  const searchParams = useSearchParams()

  useEffect(() => {
    // This is where you would initialize GA4 or other analytics tools
    // Example: window.gtag('config', 'GA_MEASUREMENT_ID', { page_path: pathname })
    console.log(`[Analytics] Page View: ${pathname}${searchParams.toString() ? `?${searchParams.toString()}` : ''}`)
  }, [pathname, searchParams])

  return null
}

// Helper function for custom event tracking
export const trackEvent = (eventName: string, params?: Record<string, any>) => {
  if (typeof window !== 'undefined') {
    console.log(`[Analytics] Event: ${eventName}`, params)
    // window.gtag('event', eventName, params)
  }
}
