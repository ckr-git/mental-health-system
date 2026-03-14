<template>
  <div class="cloud-container">
    <div
      v-for="cloud in clouds"
      :key="cloud.id"
      class="cloud"
      :class="{
        'cloud-dispersing': cloud.isDispersing,
        'cloud-clickable': !cloud.isDispersing
      }"
      :style="getCloudStyle(cloud)"
      @click="handleCloudClick(cloud.id)"
    >
      <div class="cloud-part cloud-part-1"></div>
      <div class="cloud-part cloud-part-2"></div>
      <div class="cloud-part cloud-part-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Cloud {
  id: number
  x: number
  y: number
  size: number
  speed: number
  isDispersing: boolean
  clickCount: number
}

const clouds = ref<Cloud[]>([])
let cloudId = 0

// 初始化云朵
const initClouds = (count: number = 5) => {
  const newClouds: Cloud[] = []

  for (let i = 0; i < count; i++) {
    newClouds.push({
      id: cloudId++,
      x: Math.random() * 80 + 10, // 10-90%
      y: Math.random() * 30 + 5, // 5-35%
      size: 0.6 + Math.random() * 0.6, // 0.6-1.2
      speed: 30 + Math.random() * 40, // 30-70秒
      isDispersing: false,
      clickCount: 0
    })
  }

  clouds.value = newClouds
}

// 计算云朵样式
const getCloudStyle = (cloud: Cloud) => {
  return {
    left: `${cloud.x}%`,
    top: `${cloud.y}%`,
    '--size': cloud.size,
    '--speed': `${cloud.speed}s`
  }
}

// 点击云朵
const handleCloudClick = (cloudId: number) => {
  const cloud = clouds.value.find(c => c.id === cloudId)
  if (!cloud || cloud.isDispersing) return

  cloud.clickCount++

  // 连续点击3次后散开
  if (cloud.clickCount >= 3) {
    cloud.isDispersing = true

    // 2秒后移除并重新生成
    setTimeout(() => {
      const index = clouds.value.findIndex(c => c.id === cloudId)
      if (index !== -1) {
        clouds.value[index] = {
          id: cloudId++,
          x: Math.random() * 80 + 10,
          y: Math.random() * 30 + 5,
          size: 0.6 + Math.random() * 0.6,
          speed: 30 + Math.random() * 40,
          isDispersing: false,
          clickCount: 0
        }
      }
    }, 2000)
  }

  // 5秒后重置点击计数
  setTimeout(() => {
    if (cloud && !cloud.isDispersing) {
      cloud.clickCount = 0
    }
  }, 5000)
}

onMounted(() => {
  initClouds()
})

// 暴露方法
defineExpose({
  initClouds
})
</script>

<style scoped>
.cloud-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 100;
}

.cloud {
  position: absolute;
  pointer-events: auto;
  cursor: pointer;
  transform: translate(-50%, -50%) scale(var(--size));
  animation: cloud-float var(--speed) linear infinite;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.cloud-clickable:hover {
  transform: translate(-50%, -50%) scale(calc(var(--size) * 1.1));
}

.cloud-part {
  position: absolute;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  filter: blur(3px);
  box-shadow:
    0 0 20px rgba(255, 255, 255, 0.8),
    0 0 40px rgba(255, 255, 255, 0.6),
    inset 0 0 20px rgba(200, 220, 255, 0.3);
}

.cloud-part-1 {
  width: 80px;
  height: 50px;
  left: 0;
  top: 10px;
}

.cloud-part-2 {
  width: 60px;
  height: 40px;
  left: 50px;
  top: 0;
}

.cloud-part-3 {
  width: 70px;
  height: 45px;
  left: 90px;
  top: 8px;
}

/* 云朵漂浮动画 */
@keyframes cloud-float {
  0% {
    transform: translate(-50%, -50%) translateX(0) scale(var(--size));
  }
  100% {
    transform: translate(-50%, -50%) translateX(100vw) scale(var(--size));
  }
}

/* 云朵散开动画 */
.cloud-dispersing {
  animation: cloud-disperse 2s ease-out forwards !important;
}

.cloud-dispersing .cloud-part-1 {
  animation: part-disperse-1 2s ease-out forwards;
}

.cloud-dispersing .cloud-part-2 {
  animation: part-disperse-2 2s ease-out forwards;
}

.cloud-dispersing .cloud-part-3 {
  animation: part-disperse-3 2s ease-out forwards;
}

@keyframes cloud-disperse {
  0% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) scale(calc(var(--size) * 2));
  }
}

@keyframes part-disperse-1 {
  0% {
    transform: translate(0, 0) scale(1);
  }
  100% {
    transform: translate(-30px, -20px) scale(0.5);
    opacity: 0;
  }
}

@keyframes part-disperse-2 {
  0% {
    transform: translate(0, 0) scale(1);
  }
  100% {
    transform: translate(0, -40px) scale(0.3);
    opacity: 0;
  }
}

@keyframes part-disperse-3 {
  0% {
    transform: translate(0, 0) scale(1);
  }
  100% {
    transform: translate(30px, -20px) scale(0.5);
    opacity: 0;
  }
}
</style>
