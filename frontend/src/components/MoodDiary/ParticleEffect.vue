<template>
  <div class="particle-container">
    <transition-group name="particle" tag="div">
      <div
        v-for="particle in particles"
        :key="particle.id"
        class="particle"
        :class="`particle-${type}`"
        :style="getParticleStyle(particle)"
      >
        {{ particle.emoji }}
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  type: 'water' | 'fire' | 'star' | 'heart' | 'note' | 'light' | 'sparkle'
  trigger: number // 触发计数器
  x?: number // 起始X位置（百分比）
  y?: number // 起始Y位置（百分比）
}

interface Particle {
  id: number
  emoji: string
  x: number
  y: number
  vx: number
  vy: number
  life: number
  scale: number
  rotation: number
}

const props = withDefaults(defineProps<Props>(), {
  x: 50,
  y: 50
})

const particles = ref<Particle[]>([])
let particleId = 0

// 不同类型的粒子配置
const particleConfig = {
  water: {
    emoji: '💧',
    count: 8,
    speed: { min: 2, max: 5 },
    life: 1500,
    direction: 'down'
  },
  fire: {
    emoji: '🔥',
    count: 10, // 减少数量以提高性能
    speed: { min: 1, max: 3 },
    life: 2000,
    direction: 'up'
  },
  star: {
    emoji: '✨',
    count: 12, // 减少数量以提高性能
    speed: { min: 2, max: 4 },
    life: 1800,
    direction: 'radial'
  },
  heart: {
    emoji: '💖',
    count: 10,
    speed: { min: 1.5, max: 3 },
    life: 2000,
    direction: 'float'
  },
  note: {
    emoji: '🎵',
    count: 10, // 减少数量以提高性能
    speed: { min: 1, max: 2.5 },
    life: 2500,
    direction: 'curve'
  },
  light: {
    emoji: '💡',
    count: 8,
    speed: { min: 1, max: 2 },
    life: 1500,
    direction: 'pulse'
  },
  sparkle: {
    emoji: '⭐',
    count: 15, // 减少数量以提高性能
    speed: { min: 2, max: 5 },
    life: 1500,
    direction: 'burst'
  }
}

// 创建粒子
const createParticles = () => {
  const config = particleConfig[props.type]
  const newParticles: Particle[] = []

  for (let i = 0; i < config.count; i++) {
    const angle = (Math.PI * 2 * i) / config.count
    const speed = config.speed.min + Math.random() * (config.speed.max - config.speed.min)

    let vx = 0
    let vy = 0

    switch (config.direction) {
      case 'down':
        vx = (Math.random() - 0.5) * 2
        vy = speed
        break
      case 'up':
        vx = (Math.random() - 0.5) * 2
        vy = -speed
        break
      case 'radial':
        vx = Math.cos(angle) * speed
        vy = Math.sin(angle) * speed
        break
      case 'float':
        vx = (Math.random() - 0.5) * speed
        vy = -speed * 0.5
        break
      case 'curve':
        vx = Math.cos(angle) * speed * 0.5
        vy = -speed
        break
      case 'burst':
        vx = Math.cos(angle) * speed
        vy = Math.sin(angle) * speed
        break
      case 'pulse':
        vx = 0
        vy = 0
        break
    }

    newParticles.push({
      id: particleId++,
      emoji: config.emoji,
      x: props.x,
      y: props.y,
      vx,
      vy,
      life: config.life,
      scale: 0.8 + Math.random() * 0.4,
      rotation: Math.random() * 360
    })
  }

  particles.value = newParticles

  // 自动清理
  setTimeout(() => {
    particles.value = []
  }, config.life)
}

// 计算粒子样式
const getParticleStyle = (particle: Particle) => {
  return {
    left: `${particle.x}%`,
    top: `${particle.y}%`,
    '--vx': particle.vx,
    '--vy': particle.vy,
    '--life': `${particle.life}ms`,
    '--scale': particle.scale,
    '--rotation': `${particle.rotation}deg`
  }
}

// 监听触发器
watch(() => props.trigger, () => {
  if (props.trigger > 0) {
    createParticles()
  }
})
</script>

<style scoped>
.particle-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  font-size: 20px;
  pointer-events: none;
  transform: translate(-50%, -50%) scale(var(--scale)) rotate(var(--rotation));
  animation-duration: var(--life);
  animation-timing-function: ease-out;
  animation-fill-mode: forwards;
  will-change: transform, opacity;
  backface-visibility: hidden; /* Smooth rendering */
}

/* 水滴效果 */
.particle-water {
  animation-name: particle-fall;
  filter: drop-shadow(0 0 3px rgba(64, 158, 255, 0.6));
}

@keyframes particle-fall {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) translateY(0) scale(var(--scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateY(calc(var(--vy) * 50px)) translateX(calc(var(--vx) * 30px)) scale(0.3);
  }
}

/* 火焰效果 */
.particle-fire {
  animation-name: particle-rise;
  filter: drop-shadow(0 0 5px rgba(255, 136, 0, 0.8));
}

@keyframes particle-rise {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) translateY(0) scale(var(--scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateY(calc(var(--vy) * 40px)) translateX(calc(var(--vx) * 20px)) scale(0.2);
  }
}

/* 星星放射效果 */
.particle-star {
  animation-name: particle-radial;
  filter: drop-shadow(0 0 4px rgba(255, 215, 0, 0.8));
}

@keyframes particle-radial {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 30px)) translateY(calc(var(--vy) * 30px)) scale(var(--scale)) rotate(180deg);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 50px)) translateY(calc(var(--vy) * 50px)) scale(0.3) rotate(360deg);
  }
}

/* 爱心漂浮效果 */
.particle-heart {
  animation-name: particle-float;
  filter: drop-shadow(0 0 4px rgba(255, 105, 180, 0.7));
}

@keyframes particle-float {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0.5);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 20px)) translateY(calc(var(--vy) * 40px)) scale(var(--scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 30px)) translateY(calc(var(--vy) * 80px)) scale(0.2);
  }
}

/* 音符曲线效果 */
.particle-note {
  animation-name: particle-curve;
  filter: drop-shadow(0 0 3px rgba(147, 112, 219, 0.7));
}

@keyframes particle-curve {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(var(--scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateX(calc(sin(var(--vy) * 3.14) * 40px)) translateY(calc(var(--vy) * -60px)) scale(0.3) rotate(720deg);
  }
}

/* 光芒脉冲效果 */
.particle-light {
  animation-name: particle-pulse;
  filter: drop-shadow(0 0 8px rgba(255, 255, 0, 0.9));
}

@keyframes particle-pulse {
  0%, 100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.5);
  }
  50% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(var(--scale));
  }
}

/* 闪光爆发效果 */
.particle-sparkle {
  animation-name: particle-burst;
  filter: drop-shadow(0 0 5px rgba(255, 223, 0, 0.9));
}

@keyframes particle-burst {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0);
  }
  30% {
    opacity: 1;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 20px)) translateY(calc(var(--vy) * 20px)) scale(var(--scale));
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateX(calc(var(--vx) * 60px)) translateY(calc(var(--vy) * 60px)) scale(0) rotate(720deg);
  }
}

/* 粒子进出场动画 */
.particle-enter-active {
  transition: none;
}

.particle-leave-active {
  transition: none;
}
</style>
