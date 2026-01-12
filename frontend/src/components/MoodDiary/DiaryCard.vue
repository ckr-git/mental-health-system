<template>
  <div class="diary-card" @click="handleClick">
    <!-- 状态动画canvas -->
    <canvas
      ref="animationCanvas"
      class="animation-canvas"
      v-if="showAnimation"
    ></canvas>

    <!-- 状态徽章 -->
    <div class="status-badge" :class="`status-${diary.status}`" v-if="diary.status !== 'ongoing'">
      {{ statusText }}
    </div>
    
    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="mood-info">
        <span class="mood-emoji">{{ diary.moodEmoji || '😊' }}</span>
        <span class="mood-score">{{ diary.moodScore }}/10</span>
      </div>
      <div class="time-info">
        {{ formatTime(diary.createTime) }}
      </div>
    </div>
    
    <!-- 卡片内容 -->
    <div class="card-content">
      <h3 class="diary-title">{{ diary.title || '未命名日记' }}</h3>
      <p class="diary-excerpt">{{ excerpt }}</p>
    </div>
    
    <!-- 卡片底部 -->
    <div class="card-footer">
      <!-- 天气标签 -->
      <div class="weather-tag" v-if="diary.weatherType">
        {{ weatherIcon }} {{ weatherName }}
      </div>
      
      <!-- 统计信息 -->
      <div class="stats">
        <span class="stat-item" v-if="diary.viewCount">
          👁️ {{ diary.viewCount }}
        </span>
        <span class="stat-item" v-if="diary.commentCount">
          💬 {{ diary.commentCount }}
        </span>
      </div>
    </div>
    
    <!-- 悬浮交互提示 -->
    <div class="hover-hint">点击查看详情</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { playSound } from '@/utils/soundService'

interface Diary {
  id: number
  moodScore: number
  moodEmoji?: string
  title?: string
  content?: string
  weatherType?: string
  status?: string
  createTime: string
  viewCount?: number
  commentCount?: number
}

interface Props {
  diary: Diary
  maxExcerptLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxExcerptLength: 60
})

const emit = defineEmits<{
  click: [diary: Diary]
}>()

// 天气配置
const weatherConfigs: Record<string, { name: string; icon: string }> = {
  storm: { name: '暴风雨', icon: '⛈️' },
  rain: { name: '细雨', icon: '🌧️' },
  cloudy: { name: '多云', icon: '☁️' },
  sunny: { name: '晴天', icon: '☀️' },
  clear: { name: '晴空', icon: '✨' }
}

// 状态配置
const statusConfigs: Record<string, string> = {
  better: '已好转',
  overcome: '已度过',
  proud: '战胜了'
}

const weatherIcon = computed(() => 
  props.diary.weatherType ? weatherConfigs[props.diary.weatherType]?.icon : ''
)

const weatherName = computed(() => 
  props.diary.weatherType ? weatherConfigs[props.diary.weatherType]?.name : ''
)

const statusText = computed(() => 
  props.diary.status ? statusConfigs[props.diary.status] : ''
)

const excerpt = computed(() => {
  const content = props.diary.content || ''
  if (content.length <= props.maxExcerptLength) {
    return content
  }
  return content.substring(0, props.maxExcerptLength) + '...'
})

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const handleClick = () => {
  playSound('card-flip')
  emit('click', props.diary)
}

// 动画相关
const animationCanvas = ref<HTMLCanvasElement>()
const showAnimation = ref(false)
let animationFrameId: number
let ctx: CanvasRenderingContext2D | null = null

// 粒子基类
class AnimationParticle {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  maxLife: number
  size: number
  color: string

  constructor(x: number, y: number, color: string) {
    this.x = x
    this.y = y
    this.vx = (Math.random() - 0.5) * 3
    this.vy = -Math.random() * 4 - 2
    this.life = 1
    this.maxLife = 1
    this.size = Math.random() * 4 + 2
    this.color = color
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    this.vy += 0.1 // 重力
    this.life -= 0.01
    return this.life > 0
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = `${this.color}${Math.floor(this.life * 255).toString(16).padStart(2, '0')}`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

// 云层粒子
class CloudParticle {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  size: number
  opacity: number

  constructor(x: number, y: number) {
    this.x = x
    this.y = y
    this.vx = Math.random() * 2 - 1
    this.vy = -Math.random() * 2 - 1
    this.life = 1
    this.size = Math.random() * 20 + 10
    this.opacity = 0.6
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    this.life -= 0.015
    this.opacity = this.life * 0.6
    return this.life > 0
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = `rgba(200, 220, 255, ${this.opacity})`
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fill()
  }
}

// 彩虹桥
class Rainbow {
  centerX: number
  centerY: number
  radius: number
  progress: number
  colors: string[] = [
    '#FF0000', '#FF7F00', '#FFFF00',
    '#00FF00', '#0000FF', '#4B0082', '#9400D3'
  ]

  constructor(width: number, height: number) {
    this.centerX = width / 2
    this.centerY = height
    this.radius = Math.min(width, height) * 0.8
    this.progress = 0
  }

  update() {
    this.progress += 0.02
    return this.progress < 1
  }

  draw(ctx: CanvasRenderingContext2D) {
    const startAngle = Math.PI
    const endAngle = Math.PI + Math.PI * this.progress

    this.colors.forEach((color, index) => {
      const thickness = 8
      const currentRadius = this.radius - index * (thickness + 2)

      ctx.strokeStyle = `${color}${Math.floor(Math.min(this.progress * 2, 1) * 255).toString(16).padStart(2, '0')}`
      ctx.lineWidth = thickness
      ctx.beginPath()
      ctx.arc(this.centerX, this.centerY, currentRadius, startAngle, endAngle)
      ctx.stroke()
    })
  }
}

// 烟花粒子
class Firework {
  x: number
  y: number
  particles: AnimationParticle[] = []
  exploded: boolean = false
  life: number = 1

  constructor(x: number, y: number) {
    this.x = x
    this.y = y
  }

  explode() {
    if (this.exploded) return
    this.exploded = true

    const colors = ['#FFD700', '#FFA500', '#FF6347', '#FF69B4']
    const particleCount = 30

    for (let i = 0; i < particleCount; i++) {
      const angle = (Math.PI * 2 * i) / particleCount
      const velocity = Math.random() * 3 + 2
      const particle = new AnimationParticle(this.x, this.y, colors[Math.floor(Math.random() * colors.length)])
      particle.vx = Math.cos(angle) * velocity
      particle.vy = Math.sin(angle) * velocity
      this.particles.push(particle)
    }
  }

  update() {
    if (!this.exploded) {
      this.explode()
    }

    this.particles = this.particles.filter(p => p.update())
    this.life -= 0.008
    return this.particles.length > 0 && this.life > 0
  }

  draw(ctx: CanvasRenderingContext2D) {
    this.particles.forEach(p => p.draw(ctx))
  }
}

// 动画对象
let animationObjects: any[] = []

// 初始化动画
const initAnimation = (status: string) => {
  if (!animationCanvas.value) return

  const canvas = animationCanvas.value
  canvas.width = canvas.offsetWidth
  canvas.height = canvas.offsetHeight
  ctx = canvas.getContext('2d')

  animationObjects = []

  if (status === 'better') {
    // 云层散开效果
    for (let i = 0; i < 15; i++) {
      setTimeout(() => {
        animationObjects.push(new CloudParticle(
          canvas.width / 2 + (Math.random() - 0.5) * 100,
          canvas.height / 2 + (Math.random() - 0.5) * 50
        ))
      }, i * 100)
    }
  } else if (status === 'overcome') {
    // 彩虹桥效果
    animationObjects.push(new Rainbow(canvas.width, canvas.height))
  } else if (status === 'proud') {
    // 烟花效果
    const positions = [
      { x: canvas.width * 0.3, y: canvas.height * 0.3 },
      { x: canvas.width * 0.7, y: canvas.height * 0.3 },
      { x: canvas.width * 0.5, y: canvas.height * 0.5 }
    ]

    positions.forEach((pos, index) => {
      setTimeout(() => {
        animationObjects.push(new Firework(pos.x, pos.y))
      }, index * 400)
    })
  }

  showAnimation.value = true
  animate()
}

// 动画循环
const animate = () => {
  if (!ctx || !animationCanvas.value) return

  ctx.clearRect(0, 0, animationCanvas.value.width, animationCanvas.value.height)

  animationObjects = animationObjects.filter(obj => {
    const alive = obj.update()
    if (alive) {
      obj.draw(ctx!)
    }
    return alive
  })

  if (animationObjects.length > 0) {
    animationFrameId = requestAnimationFrame(animate)
  } else {
    showAnimation.value = false
  }
}

// 监听状态变化
watch(() => props.diary.status, (newStatus, oldStatus) => {
  if (newStatus && newStatus !== 'ongoing' && newStatus !== oldStatus) {
    initAnimation(newStatus)
  }
})

onMounted(() => {
  // 如果初始状态已经变化，显示动画
  if (props.diary.status && props.diary.status !== 'ongoing') {
    // 首次加载不显示动画，避免过多动画
    // initAnimation(props.diary.status)
  }
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})
</script>

<style scoped>
.diary-card {
  position: relative;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.diary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.diary-card:hover .hover-hint {
  opacity: 1;
  transform: translateY(0);
}

/* 状态徽章 */
.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  color: white;
  z-index: 1;
}

.status-better {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.status-overcome {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.status-proud {
  background: linear-gradient(135deg, #ffd89b 0%, #19547b 100%);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mood-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mood-emoji {
  font-size: 32px;
  line-height: 1;
}

.mood-score {
  font-size: 20px;
  font-weight: 600;
  color: #2d3748;
}

.time-info {
  font-size: 12px;
  color: #718096;
}

/* 卡片内容 */
.card-content {
  margin: 16px 0;
}

.diary-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.diary-excerpt {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  margin: 0;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.weather-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  font-size: 12px;
  color: #4a5568;
}

.stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  font-size: 12px;
  color: #718096;
}

/* 悬浮提示 */
.hover-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  font-size: 12px;
  opacity: 0;
  transform: translateY(100%);
  transition: all 0.3s ease;
}

/* 动画画布 */
.animation-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}
</style>
