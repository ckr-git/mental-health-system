<template>
  <div class="light-switch" @click="toggle">
    <!-- 灯绳 -->
    <div class="rope" :class="{ pulling: isPulling }">
      <div class="rope-line"></div>
      <div class="rope-handle"></div>
    </div>
    
    <!-- 灯泡 -->
    <div class="bulb" :class="{ on: lightMode === 'day' }">
      <div class="bulb-glow"></div>
      <span class="bulb-icon">{{ lightMode === 'day' ? '💡' : '🌙' }}</span>
    </div>
    
    <!-- 提示文字 -->
    <div class="switch-hint" v-if="showHint">
      {{ lightMode === 'day' ? '点击关灯' : '点击开灯' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { playSound } from '@/utils/soundService'

interface Props {
  lightMode?: 'day' | 'night'
  showHint?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  lightMode: 'day',
  showHint: true
})

const emit = defineEmits<{
  toggle: []
}>()

const isPulling = ref(false)

const toggle = () => {
  playSound('switch')
  isPulling.value = true

  // 播放拉绳动画
  setTimeout(() => {
    isPulling.value = false
    emit('toggle')
  }, 300)
}
</script>

<style scoped>
.light-switch {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  cursor: pointer;
  user-select: none;
}

.rope {
  position: relative;
  width: 4px;
  height: 60px;
  margin: 0 auto;
  transition: height 0.3s ease;
}

.rope.pulling {
  height: 80px;
}

.rope-line {
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, #8b7355 0%, #654321 100%);
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.rope-handle {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  background: #654321;
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.bulb {
  position: relative;
  width: 60px;
  height: 60px;
  margin: -10px auto 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.bulb.on {
  background: rgba(255, 235, 59, 0.3);
  box-shadow: 
    0 0 20px rgba(255, 235, 59, 0.5),
    0 4px 12px rgba(0, 0, 0, 0.1);
}

.bulb-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 235, 59, 0.6) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.bulb.on .bulb-glow {
  opacity: 1;
  animation: pulse 2s ease-in-out infinite;
}

.bulb-icon {
  font-size: 32px;
  position: relative;
  z-index: 1;
  transition: transform 0.3s ease;
}

.light-switch:hover .bulb-icon {
  transform: scale(1.1);
}

.light-switch:active .bulb-icon {
  transform: scale(0.95);
}

.switch-hint {
  margin-top: 12px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  animation: fadeInOut 3s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

@keyframes fadeInOut {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}
</style>
