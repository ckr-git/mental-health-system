<template>
  <div class="firework-container">
    <canvas ref="canvasRef" class="firework-canvas"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { throttle } from '@/utils/performance'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  alpha: number
  color: string
  gravity: number
}

const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let particles: Particle[] = []
let animationId: number | null = null
let isActive = false
let lastFrameTime = 0
const targetFPS = 60
const frameInterval = 1000 / targetFPS

// Auto-adjust particle count based on device performance
let particleMultiplier = 1
const performanceMode = (() => {
  // Detect device performance based on hardware concurrency and memory
  const cores = navigator.hardwareConcurrency || 4
  if (cores >= 8) return 'high'
  if (cores >= 4) return 'medium'
  return 'low'
})()

if (performanceMode === 'low') {
  particleMultiplier = 0.5 // 50% particles on low-end devices
} else if (performanceMode === 'medium') {
  particleMultiplier = 0.75 // 75% particles on mid-range devices
}

// 初始化Canvas
const initCanvas = () => {
  if (!canvasRef.value) return

  const canvas = canvasRef.value
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  ctx = canvas.getContext('2d')
}

// 创建烟花爆炸
const createFirework = (x: number, y: number, color?: string) => {
  const baseParticleCount = 100
  const particleCount = Math.floor(baseParticleCount * particleMultiplier)
  const colors = color ? [color] : [
    '#ff1744', '#ff9100', '#ffd600', '#76ff03',
    '#00e5ff', '#2979ff', '#d500f9', '#ff4081'
  ]

  for (let i = 0; i < particleCount; i++) {
    const angle = (Math.PI * 2 * i) / particleCount
    const speed = 3 + Math.random() * 5

    particles.push({
      x,
      y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      alpha: 1,
      color: colors[Math.floor(Math.random() * colors.length)],
      gravity: 0.08
    })
  }
}

// 动画循环 (optimized with frame limiting)
const animate = (currentTime: number = 0) => {
  if (!ctx || !canvasRef.value) return

  // Frame rate limiting for better performance
  const elapsed = currentTime - lastFrameTime
  if (elapsed < frameInterval) {
    if (isActive) {
      animationId = requestAnimationFrame(animate)
    }
    return
  }
  lastFrameTime = currentTime - (elapsed % frameInterval)

  ctx.fillStyle = 'rgba(0, 0, 0, 0.15)'
  ctx.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height)

  // 更新和绘制粒子 (batch operations for better performance)
  let i = particles.length
  while (i--) {
    const particle = particles[i]

    particle.x += particle.vx
    particle.y += particle.vy
    particle.vy += particle.gravity
    particle.alpha -= 0.008

    if (particle.alpha <= 0) {
      particles.splice(i, 1)
      continue
    }

    ctx.globalAlpha = particle.alpha
    ctx.fillStyle = particle.color
    ctx.shadowBlur = 15
    ctx.shadowColor = particle.color
    ctx.beginPath()
    ctx.arc(particle.x, particle.y, 4, 0, Math.PI * 2)
    ctx.fill()
  }

  if (isActive && particles.length > 0) {
    animationId = requestAnimationFrame(animate)
  } else if (isActive) {
    // 如果还在激活状态但粒子已清空，继续监听
    animationId = requestAnimationFrame(animate)
  }
}

// 开始烟花表演
const startFireworks = (duration: number = 5000) => {
  if (!canvasRef.value) return

  isActive = true
  const interval = 400 // 每400ms一个烟花

  animate()

  const fireworkInterval = setInterval(() => {
    const x = Math.random() * canvasRef.value!.width
    const y = Math.random() * canvasRef.value!.height * 0.6 + 50 // 上60%区域
    createFirework(x, y)
  }, interval)

  // 持续时间后停止
  setTimeout(() => {
    clearInterval(fireworkInterval)
    isActive = false
  }, duration)
}

// 停止烟花
const stopFireworks = () => {
  isActive = false
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  particles = []

  // 清空画布
  if (ctx && canvasRef.value) {
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  }
}

// 监听窗口大小变化 (throttled for performance)
const handleResize = throttle(() => {
  if (canvasRef.value) {
    canvasRef.value.width = window.innerWidth
    canvasRef.value.height = window.innerHeight
  }
}, 200) // Throttle to once per 200ms

onMounted(() => {
  initCanvas()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  stopFireworks()
  window.removeEventListener('resize', handleResize)
})

// 暴露方法
defineExpose({
  startFireworks,
  stopFireworks,
  createFirework
})
</script>

<style scoped>
.firework-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 999;
}

.firework-canvas {
  width: 100%;
  height: 100%;
}
</style>
