<template>
  <div class="weather-background" :class="[`weather-${weatherType}`, `mode-${lightMode}`]">
    <!-- 背景渐变 -->
    <div class="gradient-layer" :style="gradientStyle"></div>
    
    <!-- 粒子效果Canvas -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>
    
    <!-- 天气图标 -->
    <div class="weather-icon">
      <span class="icon">{{ weatherIcon }}</span>
      <span class="weather-name">{{ weatherName }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { playAmbient, stopAmbient } from '@/utils/soundService'

interface Props {
  weatherType?: 'storm' | 'rain' | 'cloudy' | 'sunny' | 'clear'
  lightMode?: 'day' | 'night'
}

const props = withDefaults(defineProps<Props>(), {
  weatherType: 'sunny',
  lightMode: 'day'
})

// 天气配置
const weatherConfigs = {
  storm: {
    name: '暴风雨',
    icon: '⛈️',
    bgStart: '#1a1a2e',
    bgEnd: '#16213e',
    particleColor: '#4a5568',
    particleCount: 80,
    particleSpeed: 3,
    soundType: 'stormy'
  },
  rain: {
    name: '细雨',
    icon: '🌧️',
    bgStart: '#4a5568',
    bgEnd: '#718096',
    particleColor: '#a0aec0',
    particleCount: 60,
    particleSpeed: 2,
    soundType: 'rainy'
  },
  cloudy: {
    name: '多云',
    icon: '☁️',
    bgStart: '#cbd5e0',
    bgEnd: '#e2e8f0',
    particleColor: '#ffffff',
    particleCount: 30,
    particleSpeed: 0.5,
    soundType: 'cloudy'
  },
  sunny: {
    name: '晴天',
    icon: '☀️',
    bgStart: '#fbd38d',
    bgEnd: '#fbb6ce',
    particleColor: '#fed7aa',
    particleCount: 20,
    particleSpeed: 0.3,
    soundType: 'sunny'
  },
  clear: {
    name: '晴空',
    icon: '✨',
    bgStart: '#63b3ed',
    bgEnd: '#4299e1',
    particleColor: '#ffffff',
    particleCount: 25,
    particleSpeed: 0.4,
    soundType: 'sunny'
  }
}

const currentConfig = computed(() => weatherConfigs[props.weatherType])
const weatherIcon = computed(() => currentConfig.value.icon)
const weatherName = computed(() => currentConfig.value.name)

// 渐变样式（根据日夜模式调整）
const gradientStyle = computed(() => {
  const config = currentConfig.value
  let startColor = config.bgStart
  let endColor = config.bgEnd
  
  // 夜晚模式调暗
  if (props.lightMode === 'night') {
    startColor = darkenColor(config.bgStart, 0.4)
    endColor = darkenColor(config.bgEnd, 0.4)
  }
  
  return {
    background: `linear-gradient(180deg, ${startColor} 0%, ${endColor} 100%)`
  }
})

// 粒子系统
const particleCanvas = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D | null = null
let particles: any[] = []
let splashes: Splash[] = []
let lightnings: Lightning[] = []
let lightningTimer = 0
let animationFrameId: number

// 基础粒子类
class Particle {
  x: number
  y: number
  vx: number
  vy: number
  size: number
  opacity: number

  constructor(canvasWidth: number, canvasHeight: number, speed: number) {
    this.x = Math.random() * canvasWidth
    this.y = Math.random() * canvasHeight
    this.vx = (Math.random() - 0.5) * speed
    this.vy = Math.random() * speed + 1
    this.size = Math.random() * 3 + 1
    this.opacity = Math.random() * 0.5 + 0.3
  }

  update(canvasWidth: number, canvasHeight: number) {
    this.x += this.vx
    this.y += this.vy

    if (this.y > canvasHeight) {
      this.y = -10
      this.x = Math.random() * canvasWidth
    }
    if (this.x < 0 || this.x > canvasWidth) {
      this.x = Math.random() * canvasWidth
    }
  }

  draw(ctx: CanvasRenderingContext2D, color: string) {
    ctx.fillStyle = `${color}${Math.floor(this.opacity * 255).toString(16).padStart(2, '0')}`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

// 雨滴类（增强）
class RainDrop {
  x: number
  y: number
  length: number
  speed: number
  opacity: number
  angle: number

  constructor(canvasWidth: number, canvasHeight: number, speed: number) {
    this.x = Math.random() * canvasWidth
    this.y = Math.random() * canvasHeight - canvasHeight
    this.length = Math.random() * 15 + 10
    this.speed = Math.random() * speed + speed
    this.opacity = Math.random() * 0.3 + 0.5
    this.angle = Math.PI / 6 // 30度斜角
  }

  update(canvasWidth: number, canvasHeight: number) {
    this.x += Math.cos(this.angle) * this.speed
    this.y += this.speed

    // 雨滴到达底部时返回溅水位置
    if (this.y > canvasHeight) {
      return { x: this.x, y: canvasHeight }
    }
    return null
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.strokeStyle = `rgba(174, 194, 224, ${this.opacity})`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(
      this.x - Math.cos(this.angle) * this.length,
      this.y - this.length
    )
    ctx.stroke()
  }

  reset(canvasWidth: number) {
    this.x = Math.random() * canvasWidth
    this.y = -10
  }
}

// 溅水效果类
class Splash {
  x: number
  y: number
  particles: { x: number; y: number; vx: number; vy: number; life: number }[]

  constructor(x: number, y: number) {
    this.x = x
    this.y = y
    this.particles = []

    // 创建3-5个溅水粒子
    for (let i = 0; i < Math.random() * 2 + 3; i++) {
      this.particles.push({
        x: 0,
        y: 0,
        vx: (Math.random() - 0.5) * 3,
        vy: -(Math.random() * 3 + 2),
        life: 1
      })
    }
  }

  update() {
    this.particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy
      p.vy += 0.2 // 重力
      p.life -= 0.05
    })

    this.particles = this.particles.filter(p => p.life > 0)
    return this.particles.length > 0
  }

  draw(ctx: CanvasRenderingContext2D) {
    this.particles.forEach(p => {
      ctx.fillStyle = `rgba(174, 194, 224, ${p.life * 0.6})`
      ctx.beginPath()
      ctx.arc(this.x + p.x, this.y + p.y, 2, 0, Math.PI * 2)
      ctx.fill()
    })
  }
}

// 云朵类
class Cloud {
  x: number
  y: number
  width: number
  height: number
  speed: number
  opacity: number

  constructor(canvasWidth: number, canvasHeight: number) {
    this.x = Math.random() * canvasWidth
    this.y = Math.random() * (canvasHeight * 0.3)
    this.width = Math.random() * 80 + 60
    this.height = this.width * 0.6
    this.speed = Math.random() * 0.3 + 0.1
    this.opacity = Math.random() * 0.3 + 0.2
  }

  update(canvasWidth: number) {
    this.x += this.speed
    if (this.x > canvasWidth + this.width) {
      this.x = -this.width
    }
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = `rgba(255, 255, 255, ${this.opacity})`

    // 绘制多个圆形组成云朵
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.width * 0.3, 0, Math.PI * 2)
    ctx.arc(this.x + this.width * 0.3, this.y - this.height * 0.2, this.width * 0.35, 0, Math.PI * 2)
    ctx.arc(this.x + this.width * 0.6, this.y, this.width * 0.3, 0, Math.PI * 2)
    ctx.fill()
  }
}

// 闪电类
class Lightning {
  segments: { x: number; y: number }[]
  opacity: number
  life: number

  constructor(canvasWidth: number, canvasHeight: number) {
    this.segments = []
    this.opacity = 1
    this.life = 1

    // 生成闪电路径
    let x = Math.random() * canvasWidth
    let y = 0
    this.segments.push({ x, y })

    while (y < canvasHeight * 0.7) {
      x += (Math.random() - 0.5) * 50
      y += Math.random() * 50 + 30
      this.segments.push({ x, y })
    }
  }

  update() {
    this.life -= 0.1
    this.opacity = this.life
    return this.life > 0
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.strokeStyle = `rgba(255, 255, 200, ${this.opacity})`
    ctx.lineWidth = 3
    ctx.shadowBlur = 15
    ctx.shadowColor = 'rgba(255, 255, 200, 0.8)'

    ctx.beginPath()
    ctx.moveTo(this.segments[0].x, this.segments[0].y)
    for (let i = 1; i < this.segments.length; i++) {
      ctx.lineTo(this.segments[i].x, this.segments[i].y)
    }
    ctx.stroke()

    ctx.shadowBlur = 0
  }
}

// 阳光光线类
class SunRay {
  x: number
  y: number
  length: number
  angle: number
  opacity: number
  speed: number

  constructor(canvasWidth: number, canvasHeight: number) {
    this.x = canvasWidth / 2
    this.y = canvasHeight * 0.2
    this.length = Math.random() * 100 + 80
    this.angle = Math.random() * Math.PI
    this.opacity = Math.random() * 0.15 + 0.05
    this.speed = (Math.random() - 0.5) * 0.01
  }

  update() {
    this.angle += this.speed
    this.opacity = Math.abs(Math.sin(this.angle * 2)) * 0.15 + 0.05
  }

  draw(ctx: CanvasRenderingContext2D) {
    const gradient = ctx.createLinearGradient(
      this.x, this.y,
      this.x + Math.cos(this.angle) * this.length,
      this.y + Math.sin(this.angle) * this.length
    )
    gradient.addColorStop(0, `rgba(255, 248, 220, ${this.opacity})`)
    gradient.addColorStop(1, `rgba(255, 248, 220, 0)`)

    ctx.strokeStyle = gradient
    ctx.lineWidth = 20
    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(
      this.x + Math.cos(this.angle) * this.length,
      this.y + Math.sin(this.angle) * this.length
    )
    ctx.stroke()
  }
}

// 初始化画布
const initCanvas = () => {
  if (!particleCanvas.value) return

  const canvas = particleCanvas.value
  const parent = canvas.parentElement
  if (!parent) return

  canvas.width = parent.clientWidth
  canvas.height = parent.clientHeight
  ctx = canvas.getContext('2d')

  // 根据天气类型创建粒子
  const config = currentConfig.value
  const weatherType = props.weatherType
  particles = []
  splashes = []

  if (weatherType === 'storm') {
    // 暴雨：密集雨滴
    for (let i = 0; i < 100; i++) {
      particles.push(new RainDrop(canvas.width, canvas.height, config.particleSpeed))
    }
  } else if (weatherType === 'rain') {
    // 细雨：普通雨滴
    for (let i = 0; i < 60; i++) {
      particles.push(new RainDrop(canvas.width, canvas.height, config.particleSpeed))
    }
  } else if (weatherType === 'cloudy') {
    // 多云：云朵飘动
    for (let i = 0; i < 5; i++) {
      particles.push(new Cloud(canvas.width, canvas.height))
    }
  } else if (weatherType === 'sunny' || weatherType === 'clear') {
    // 晴天：阳光光线
    for (let i = 0; i < 8; i++) {
      particles.push(new SunRay(canvas.width, canvas.height))
    }
  }

  animate()
}

// 动画循环（增强版）
const animate = () => {
  if (!ctx || !particleCanvas.value) return

  const canvas = particleCanvas.value
  const weatherType = props.weatherType

  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 根据天气类型渲染不同效果
  if (weatherType === 'storm' || weatherType === 'rain') {
    // 渲染雨滴
    particles.forEach((drop: RainDrop) => {
      const splashPos = drop.update(canvas.width, canvas.height)
      if (splashPos) {
        // 创建溅水效果（每5个雨滴创建一次，避免过多）
        if (Math.random() < 0.2) {
          splashes.push(new Splash(splashPos.x, splashPos.y))
        }
        drop.reset(canvas.width)
      }
      drop.draw(ctx!)
    })

    // 渲染溅水
    splashes = splashes.filter(splash => {
      const alive = splash.update()
      if (alive) {
        splash.draw(ctx!)
      }
      return alive
    })

    // 暴雨天气：随机闪电
    if (weatherType === 'storm') {
      lightningTimer++
      if (lightningTimer > 120 && Math.random() < 0.02) {
        lightnings.push(new Lightning(canvas.width, canvas.height))
        lightningTimer = 0
      }

      // 渲染并移除消失的闪电
      lightnings = lightnings.filter(lightning => {
        const alive = lightning.update()
        if (alive) {
          lightning.draw(ctx!)
        }
        return alive
      })
    }
  } else if (weatherType === 'cloudy') {
    // 渲染云朵
    particles.forEach((cloud: Cloud) => {
      cloud.update(canvas.width)
      cloud.draw(ctx!)
    })
  } else if (weatherType === 'sunny' || weatherType === 'clear') {
    // 渲染阳光
    particles.forEach((ray: SunRay) => {
      ray.update()
      ray.draw(ctx!)
    })
  }

  animationFrameId = requestAnimationFrame(animate)
}

// 调整画布大小
const handleResize = () => {
  if (particleCanvas.value) {
    const parent = particleCanvas.value.parentElement
    if (parent) {
      particleCanvas.value.width = parent.clientWidth
      particleCanvas.value.height = parent.clientHeight
    }
  }
}

// 颜色加深函数
const darkenColor = (color: string, amount: number): string => {
  const hex = color.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  
  const newR = Math.floor(r * (1 - amount))
  const newG = Math.floor(g * (1 - amount))
  const newB = Math.floor(b * (1 - amount))
  
  return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`
}

// 监听天气变化
watch(() => props.weatherType, () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  initCanvas()

  // 播放对应天气的环境音
  const soundType = currentConfig.value.soundType
  if (soundType) {
    playAmbient(soundType)
  }
}, { immediate: true })

onMounted(() => {
  initCanvas()
  window.addEventListener('resize', handleResize)

  // 播放初始环境音
  const soundType = currentConfig.value.soundType
  if (soundType) {
    playAmbient(soundType)
  }
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  window.removeEventListener('resize', handleResize)

  // 停止环境音
  stopAmbient()
})
</script>

<style scoped>
.weather-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  transition: all 0.8s ease;
  z-index: 0;
}

.gradient-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: background 0.8s ease;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.weather-icon {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  color: white;
  font-size: 14px;
  transition: all 0.3s ease;
}

.weather-icon .icon {
  font-size: 24px;
}

/* 夜晚模式 */
.mode-night .weather-icon {
  background: rgba(0, 0, 0, 0.3);
}

/* 天气类型特定样式 */
.weather-storm .weather-icon {
  animation: shake 3s infinite;
}

.weather-sunny .weather-icon .icon {
  animation: rotate 20s linear infinite;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-2px); }
  20%, 40%, 60%, 80% { transform: translateX(2px); }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
