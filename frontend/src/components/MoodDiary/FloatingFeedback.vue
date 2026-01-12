<template>
  <div class="floating-feedback-container">
    <transition-group name="feedback" tag="div">
      <div
        v-for="feedback in feedbacks"
        :key="feedback.id"
        class="feedback-item"
        :class="`feedback-${feedback.type}`"
        :style="getFeedbackStyle(feedback)"
      >
        <div class="feedback-icon">{{ feedback.icon }}</div>
        <div class="feedback-text">{{ feedback.text }}</div>
        <div v-if="feedback.count > 1" class="feedback-combo">
          x{{ feedback.count }}
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Feedback {
  id: number
  type: string
  icon: string
  text: string
  count: number
  x: number
  y: number
}

const feedbacks = ref<Feedback[]>([])
let feedbackId = 0

// 不同交互类型的反馈配置
const feedbackConfig: Record<string, { icon: string; texts: string[] }> = {
  water: {
    icon: '💧',
    texts: ['浇水成功!', '植物更茁壮了!', '喝饱啦~', '好舒服~']
  },
  light: {
    icon: '🔥',
    texts: ['点燃!', '温暖的光!', '燃起来了!', '好温暖~']
  },
  view: {
    icon: '🔍',
    texts: ['仔细看看', '发现细节', '观察中...', '真有趣!']
  },
  play_music: {
    icon: '🎵',
    texts: ['悦耳的旋律', '音乐响起~', '好听!', '陶醉了~']
  },
  toggle: {
    icon: '💡',
    texts: ['灯光切换', '照亮了!', '好亮!', '温馨的光~']
  },
  blessing: {
    icon: '✨',
    texts: ['收到祝福!', '好运加成!', '闪闪发光~', '感受到爱~']
  },
  pet: {
    icon: '💗',
    texts: ['好开心~', '被宠爱了!', '喜欢这样!', '再摸摸~']
  }
}

// 显示反馈
const showFeedback = (type: string, x: number, y: number, count: number = 1) => {
  const config = feedbackConfig[type] || { icon: '✨', texts: ['互动成功!'] }
  const randomText = config.texts[Math.floor(Math.random() * config.texts.length)]

  const feedback: Feedback = {
    id: feedbackId++,
    type,
    icon: config.icon,
    text: randomText,
    count,
    x,
    y
  }

  feedbacks.value.push(feedback)

  // 自动移除
  setTimeout(() => {
    const index = feedbacks.value.findIndex(f => f.id === feedback.id)
    if (index !== -1) {
      feedbacks.value.splice(index, 1)
    }
  }, 2000)
}

// 计算反馈样式
const getFeedbackStyle = (feedback: Feedback) => {
  // 添加一些随机偏移，避免重叠
  const offsetX = (Math.random() - 0.5) * 20
  const offsetY = (Math.random() - 0.5) * 20

  return {
    left: `${feedback.x + offsetX}%`,
    top: `${feedback.y + offsetY}%`
  }
}

// 暴露方法给父组件
defineExpose({
  showFeedback
})
</script>

<style scoped>
.floating-feedback-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 500;
}

.feedback-item {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-size: 14px;
  font-weight: 600;
  transform: translate(-50%, -50%);
  animation: float-up 2s ease-out forwards;
  backdrop-filter: blur(10px);
  will-change: transform, opacity;
  backface-visibility: hidden;
}

/* 不同类型的颜色主题 */
.feedback-water {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.95) 0%, rgba(100, 200, 255, 0.95) 100%);
  color: white;
}

.feedback-light {
  background: linear-gradient(135deg, rgba(255, 136, 0, 0.95) 0%, rgba(255, 180, 50, 0.95) 100%);
  color: white;
}

.feedback-view {
  background: linear-gradient(135deg, rgba(147, 112, 219, 0.95) 0%, rgba(180, 150, 230, 0.95) 100%);
  color: white;
}

.feedback-play_music {
  background: linear-gradient(135deg, rgba(147, 112, 219, 0.95) 0%, rgba(200, 150, 255, 0.95) 100%);
  color: white;
}

.feedback-toggle {
  background: linear-gradient(135deg, rgba(255, 215, 0, 0.95) 0%, rgba(255, 235, 100, 0.95) 100%);
  color: #333;
}

.feedback-blessing {
  background: linear-gradient(135deg, rgba(255, 215, 0, 0.95) 0%, rgba(255, 180, 50, 0.95) 100%);
  color: white;
}

.feedback-pet {
  background: linear-gradient(135deg, rgba(255, 105, 180, 0.95) 0%, rgba(255, 150, 200, 0.95) 100%);
  color: white;
}

.feedback-icon {
  font-size: 18px;
  line-height: 1;
}

.feedback-text {
  white-space: nowrap;
  line-height: 1;
}

.feedback-combo {
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

/* 浮动向上动画 */
@keyframes float-up {
  0% {
    opacity: 0;
    transform: translate(-50%, -50%) translateY(0) scale(0.5);
  }
  20% {
    opacity: 1;
    transform: translate(-50%, -50%) translateY(-10px) scale(1.1);
  }
  40% {
    transform: translate(-50%, -50%) translateY(-20px) scale(1);
  }
  100% {
    opacity: 0;
    transform: translate(-50%, -50%) translateY(-60px) scale(0.8);
  }
}

/* 过渡动画 */
.feedback-enter-active {
  transition: none;
}

.feedback-leave-active {
  transition: none;
}
</style>
