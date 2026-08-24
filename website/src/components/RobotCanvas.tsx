"use client"

import React, { Suspense, useRef, useState } from "react"
import { Canvas, useFrame } from "@react-three/fiber"
import {
  OrbitControls,
  Stage,
  PerspectiveCamera,
  Environment,
  Html,
  Float,
  ContactShadows,
  KeyboardControls,
  KeyboardControlsEntry,
  useKeyboardControls
} from "@react-three/drei"
import * as THREE from "three"
import { motion, AnimatePresence } from "framer-motion"
import { Info, Maximize, Zap, Shield, Cpu } from "lucide-react"

enum Controls {
  forward = "forward",
  back = "back",
  left = "left",
  right = "right",
}

const map: KeyboardControlsEntry<Controls>[] = [
  { name: Controls.forward, keys: ["ArrowUp", "w", "W"] },
  { name: Controls.back, keys: ["ArrowDown", "s", "S"] },
  { name: Controls.left, keys: ["ArrowLeft", "a", "A"] },
  { name: Controls.right, keys: ["ArrowRight", "d", "D"] },
]

function Hotspot({ position, title, description, icon: Icon }: any) {
  const [active, setActive] = useState(false)

  return (
    <Html position={position} center distanceFactor={8}>
      <div className="relative group">
        <motion.button
          whileHover={{ scale: 1.2 }}
          onClick={() => setActive(!active)}
          className={`w-6 h-6 rounded-full flex items-center justify-center border-2 transition-colors ${
            active ? "bg-primary border-primary text-black" : "bg-black/50 border-white text-white"
          }`}
        >
          <Icon size={12} />
        </motion.button>

        <AnimatePresence>
          {active && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.8, y: 10 }}
              className="absolute bottom-8 left-1/2 -translate-x-1/2 w-48 bg-slate-900 text-white p-3 rounded-xl border border-white/10 shadow-2xl z-50 pointer-events-none"
            >
              <p className="text-[10px] font-black uppercase text-primary mb-1 tracking-widest">{title}</p>
              <p className="text-[11px] leading-relaxed text-slate-300">{description}</p>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </Html>
  )
}

function RobotModel({ attachment = "none" }: { attachment?: string }) {
  const group = useRef<THREE.Group>(null)
  const [, get] = useKeyboardControls<Controls>()

  useFrame((state, delta) => {
    if (!group.current) return

    const { forward, back, left, right } = get()

    // Simple movement logic
    if (forward) group.current.position.z -= 2 * delta
    if (back) group.current.position.z += 2 * delta
    if (left) group.current.rotation.y += 2 * delta
    if (right) group.current.rotation.y -= 2 * delta
  })

  return (
    <group ref={group} dispose={null}>
      <Float speed={2} rotationIntensity={0.5} floatIntensity={0.5}>
        {/* Main Body */}
        <mesh castShadow receiveShadow position={[0, 0.25, 0]}>
          <boxGeometry args={[1, 0.4, 1.5]} />
          <meshStandardMaterial
            color="#1a1a1a"
            metalness={0.9}
            roughness={0.1}
            envMapIntensity={1}
          />
        </mesh>

        {/* Chassis Accents */}
        <mesh position={[0, 0.46, 0]}>
          <boxGeometry args={[0.95, 0.05, 1.45]} />
          <meshStandardMaterial color="#666" metalness={1} roughness={0.2} />
        </mesh>

        {/* Wheels with PBR */}
        {[[-0.6, 0.1, 0.55], [0.6, 0.1, 0.55], [-0.6, 0.1, -0.55], [0.6, 0.1, -0.55]].map((pos, i) => (
          <group key={i} position={pos as [number, number, number]}>
            <mesh rotation={[0, 0, Math.PI / 2]}>
              <cylinderGeometry args={[0.3, 0.3, 0.25, 32]} />
              <meshStandardMaterial color="#080808" roughness={0.8} metalness={0.2} />
            </mesh>
            {/* Hubcap */}
            <mesh rotation={[0, 0, Math.PI / 2]} position={[pos[0] > 0 ? 0.05 : -0.05, 0, 0]}>
               <cylinderGeometry args={[0.15, 0.15, 0.1, 16]} />
               <meshStandardMaterial color={i < 2 ? "#10b981" : "#333"} metalness={0.8} roughness={0.2} />
            </mesh>
          </group>
        ))}

        {/* Sensor Tower */}
        <group position={[0, 0.6, 0.5]}>
          <mesh castShadow>
            <cylinderGeometry args={[0.08, 0.12, 0.4, 16]} />
            <meshStandardMaterial color="#222" metalness={0.8} />
          </mesh>
          <mesh position={[0, 0.25, 0]}>
            <sphereGeometry args={[0.1, 32, 32]} />
            <meshStandardMaterial color="#10b981" emissive="#10b981" emissiveIntensity={2} />
          </mesh>
          <Hotspot
            position={[0, 0.4, 0]}
            title="AI Vision System"
            description="Dual 4K cameras with depth sensing for autonomous obstacle avoidance and crop identification."
            icon={Cpu}
          />
        </group>

        {/* Battery Pack Hotspot */}
        <Hotspot
          position={[0, 0.3, -0.4]}
          title="Battery Core"
          description="High-density 10kWh Li-ion pack providing 8-12 hours of continuous operation."
          icon={Zap}
        />

        {/* Safety System Hotspot */}
        <Hotspot
          position={[0, 0.5, 0]}
          title="Security Shell"
          description="Industrial-grade IP67 rated chassis with emergency physical stop access."
          icon={Shield}
        />

        {/* Attachments with Animations */}
        <AnimatePresence mode="wait">
          {attachment === "plough" && (
            <motion.group
              key="plough"
              initial={{ scale: 0, y: -0.5 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0, y: -0.5 }}
              position={[0, 0.1, -0.9]}
            >
              <mesh rotation={[0.5, 0, 0]}>
                <boxGeometry args={[0.9, 0.1, 0.5]} />
                <meshStandardMaterial color="#444" metalness={1} roughness={0.3} />
              </mesh>
              {[[-0.3, -0.1, 0], [0, -0.1, 0.1], [0.3, -0.1, 0]].map((p, i) => (
                 <mesh key={i} position={p as [number, number, number]} rotation={[0.2, 0, 0]}>
                   <boxGeometry args={[0.05, 0.4, 0.05]} />
                   <meshStandardMaterial color="#222" />
                 </mesh>
              ))}
            </motion.group>
          )}

          {attachment === "sprayer" && (
            <motion.group
              key="sprayer"
              initial={{ scale: 0, z: 0 }}
              animate={{ scale: 1, z: -0.3 }}
              exit={{ scale: 0, z: 0 }}
              position={[0, 0.6, -0.3]}
            >
              <mesh>
                <cylinderGeometry args={[0.45, 0.45, 0.6, 32]} />
                <meshStandardMaterial color="#ffffff" roughness={0.2} metalness={0.1} />
              </mesh>
              <mesh position={[0, 0, 0]} rotation={[0, 0, Math.PI / 2]}>
                 <cylinderGeometry args={[0.04, 0.04, 1.8, 16]} />
                 <meshStandardMaterial color="#ddd" metalness={0.8} />
              </mesh>
            </motion.group>
          )}

          {attachment === "cargo" && (
            <motion.group
              key="cargo"
              initial={{ scale: 0, y: 1 }}
              animate={{ scale: 1, y: 0.55 }}
              exit={{ scale: 0, y: 1 }}
              position={[0, 0.55, -0.2]}
            >
              <mesh castShadow>
                <boxGeometry args={[1.1, 0.15, 1.3]} />
                <meshStandardMaterial color="#222" metalness={0.5} roughness={0.5} />
              </mesh>
              <mesh position={[0, 0.15, 0]}>
                 <boxGeometry args={[1.05, 0.1, 1.25]} />
                 <meshStandardMaterial color="#111" />
              </mesh>
            </motion.group>
          )}
        </AnimatePresence>
      </Float>
    </group>
  )
}

export default function RobotCanvas({ attachment = "none" }: { attachment?: string }) {
  const [isFullscreen, setIsFullscreen] = useState(false)

  return (
    <div className={`relative transition-all duration-500 ${isFullscreen ? "fixed inset-0 z-[100] bg-black" : "w-full h-[400px] md:h-[600px] cursor-grab active:cursor-grabbing"}`}>
      <KeyboardControls map={map}>
        <Canvas shadows gl={{ antialias: true, preserveDrawingBuffer: true }}>
          <PerspectiveCamera makeDefault position={[4, 3, 6]} fov={45} />
          <Suspense fallback={null}>
            <Environment preset="city" />
            <Stage
              intensity={0.8}
              environment="city"
              adjustCamera={false}
              contactShadow={{ blur: 2, opacity: 0.5 }}
            >
              <RobotModel attachment={attachment} />
            </Stage>
            <OrbitControls
              makeDefault
              enableZoom={!isFullscreen}
              minPolarAngle={Math.PI / 4}
              maxPolarAngle={Math.PI / 2}
              autoRotate={!isFullscreen}
              autoRotateSpeed={0.5}
            />
            <ContactShadows
              position={[0, -0.01, 0]}
              opacity={0.4}
              scale={10}
              blur={2}
              far={1}
            />
          </Suspense>
        </Canvas>
      </KeyboardControls>

      {/* Overlay UI Controls */}
      <div className="absolute bottom-6 right-6 flex flex-col gap-3">
        <Button
          size="icon"
          variant="outline"
          className="rounded-full bg-white/10 backdrop-blur-md border-white/20 text-white hover:bg-primary hover:text-black transition-all"
          onClick={() => setIsFullscreen(!isFullscreen)}
        >
          <Maximize size={18} />
        </Button>
        <Button
          size="icon"
          variant="outline"
          className="rounded-full bg-white/10 backdrop-blur-md border-white/20 text-white hover:bg-primary hover:text-black transition-all"
          onClick={() => alert("AR Mode coming soon to WebXR compatible devices!")}
        >
          <span className="text-[10px] font-bold">AR</span>
        </Button>
      </div>

      <div className="absolute top-6 left-6 pointer-events-none">
        <div className="bg-black/50 backdrop-blur-md border border-white/10 px-4 py-2 rounded-full flex items-center gap-3">
          <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
          <span className="text-[10px] font-black uppercase tracking-widest text-white">Interactive 3D Preview</span>
        </div>
        <div className="mt-2 text-[8px] text-white/40 uppercase font-black tracking-widest ml-4">
          Use WASD or Arrows to drive
        </div>
      </div>
    </div>
  )
}

function Button({ children, size, variant, className, onClick }: any) {
  const sizeClasses = size === "icon" ? "w-10 h-10 p-0" : "px-4 py-2"
  const variantClasses = variant === "outline" ? "border" : "bg-primary text-black"

  return (
    <button
      onClick={onClick}
      className={`flex items-center justify-center rounded-lg font-medium transition-all ${sizeClasses} ${variantClasses} ${className}`}
    >
      {children}
    </button>
  )
}
