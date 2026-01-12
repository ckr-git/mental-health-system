<template>
  <div class="meteor-container">
    <transition-group name="meteor" tag="div">
      <div
        v-for="meteor in meteors"
        :key="meteor.id"
        class="meteor"
        :style="getMeteorStyle(meteor)"
      >
        <div class="meteor-tail"></div>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Meteor {
  id: number
  x: number
  y: number
  angle: number
  length: number
  speed: number
  delay: number
}

const meteors = ref<Meteor[]>([])
let meteorId = 0

// Auto-adjust meteor count based on device performance
const performanceMode = (() => {
  const cores = navigator.hardwareConcurrency || 4
  if (cores >= 8) return 'high'
  if (cores >= 4) return 'medium'
  return 'low'
})()

let meteorMultiplier = 1
if (performanceMode === 'low') {
  meteorMultiplier = 0.6 // 60% meteors on low-end devices
} else if (performanceMode === 'medium') {
  meteorMultiplier = 0.8 // 80% meteors on mid-range devices
}

// 创建流星雨 (optimized with performance adjustment)
const createMeteorShower = (count: number = 50) => {
  const adjustedCount = Math.floor(count * meteorMultiplier)
  const newMeteors: Meteor[] = []

  for (let i = 0; i < adjustedCount; i++) {
    newMeteors.push({
      id: meteorId++,
      x: Math.random() * 100,
      y: Math.random() * 40, // 从顶部40%开始
      angle: 40 + Math.random() * 20, // 40-60度角
      length: 80 + Math.random() * 120,
      speed: 0.8 + Math.random() * 1.5,
      delay: Math.random() * 2.5 // 0-2.5秒延迟
    })
  }

  meteors.value = newMeteors

  // 5秒后清理
  setTimeout(() => {
    meteors.value = []
  }, 5000)
}

// 计算流星样式
const getMeteorStyle = (meteor: Meteor) => {
  return {
    left: `${meteor.x}%`,
    top: `${meteor.y}%`,
    '--angle': `${meteor.angle}deg`,
    '--length': `${meteor.length}px`,
    '--speed': `${meteor.speed}s`,
    '--delay': `${meteor.delay}s`
  }
}

// 暴露方法
defineExpose({
  createMeteorShower
})
</script>

<style scoped>
.meteor-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: hidden;
  z-index: 1000;
}

.meteor {
  position: absolute;
  width: 5px;
  height: 5px;
  background: radial-gradient(circle, #ffffff 0%, #e0f2ff 50%, transparent 100%);
  border-radius: 50%;
  box-shadow:
    0 0 20px 4px rgba(255, 255, 255, 1),
    0 0 40px 8px rgba(200, 230, 255, 0.8),
    0 0 60px 12px rgba(150, 200, 255, 0.5);
  animation: meteor-fall var(--speed) linear var(--delay) forwards;
}

.meteor-tail {
  position: absolute;
  top: 0;
  right: 0;
  width: var(--length);
  height: 4px;
  background: linear-gradient(
    to left,
    rgba(255, 255, 255, 0) 0%,
    rgba(180, 220, 255, 0.3) 30%,
    rgba(200, 230, 255, 0.7) 60%,
    rgba(255, 255, 255, 1) 100%
  );
  box-shadow:
    0 0 15px 3px rgba(255, 255, 255, 0.6),
    0 0 30px 6px rgba(200, 230, 255, 0.4);
  transform-origin: right center;
  transform: rotate(var(--angle));
  filter: blur(1px);
}

@keyframes meteor-fall {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.5);
  }
  5% {
    opacity: 1;
    transform: translate(5px, 5px) scale(1);
  }
  70% {
    opacity: 1;
  }
  95% {
    opacity: 0.5;
  }
  100% {
    opacity: 0;
    transform: translate(400px, 400px) scale(0.8);
  }
}

/* 过渡动画 */
.meteor-enter-active {
  transition: none;
}

.meteor-leave-active {
  transition: none;
}
</style>
