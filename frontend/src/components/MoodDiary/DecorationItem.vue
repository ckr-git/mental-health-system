<template>
  <div
    class="decoration-item"
    :class="{
      'editable': editable,
      'interacting': isInteracting,
      'can-interact': canInteract && !editable,
      'cannot-interact': !canInteract && !editable,
      [`effect-${currentEffect}`]: currentEffect
    }"
    :style="decorationStyle"
    @mousedown="handleMouseDown"
    @click="handleClick"
  >
    <!-- 粒子效果 -->
    <ParticleEffect
      v-if="particleType"
      :type="particleType"
      :trigger="particleTrigger"
      :x="50"
      :y="50"
    />

    <!-- 装饰图标 -->
    <div class="decoration-icon" :style="iconStyle">
      {{ decoration.decorationIcon || '🎨' }}
    </div>

    <!-- 编辑模式下的操作按钮 -->
    <div v-if="editable" class="edit-controls">
      <el-button
        class="remove-btn"
        type="danger"
        size="small"
        circle
        @click.stop="$emit('remove', decoration)"
      >
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <!-- 装饰名称提示 -->
    <div v-if="!editable" class="decoration-tooltip">
      {{ decoration.decorationName }}
    </div>

    <!-- 互动次数徽章 -->
    <div v-if="decoration.interactionCount > 0" class="interaction-badge">
      {{ decoration.interactionCount }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import ParticleEffect from './ParticleEffect.vue'
import { rafThrottle } from '@/utils/performance'

interface Props {
  decoration: any
  editable?: boolean
  selectedTool?: string
  interactionType?: string
}

const props = withDefaults(defineProps<Props>(), {
  editable: false,
  selectedTool: 'hand',
  interactionType: 'move'
})

const emit = defineEmits<{
  (e: 'update-position', decoration: any, position: { x: number; y: number }): void
  (e: 'interact', decoration: any): void
  (e: 'remove', decoration: any): void
}>()

const isDragging = ref(false)
const isInteracting = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const initialX = ref(0)
const initialY = ref(0)

// 粒子效果状态
const particleType = ref<'water' | 'fire' | 'star' | 'heart' | 'note' | 'light' | 'sparkle' | null>(null)
const particleTrigger = ref(0)
const currentEffect = ref('')

// 交互类型到粒子类型的映射
const interactionToParticle: Record<string, 'water' | 'fire' | 'star' | 'heart' | 'note' | 'light' | 'sparkle'> = {
  water: 'water',
  light: 'fire',
  view: 'sparkle',
  play_music: 'note',
  toggle: 'light',
  blessing: 'star',
  pet: 'heart'
}

// 判断当前工具是否可以与该装饰交互
const canInteract = computed(() => {
  // 编辑模式下只能用手套移动
  if (props.editable) {
    return props.selectedTool === 'hand'
  }

  // 非编辑模式，检查工具类型是否匹配装饰的交互类型
  const decorationInteractionType = props.decoration.interactionType
  return decorationInteractionType && props.interactionType === decorationInteractionType
})

// 装饰样式
const decorationStyle = computed(() => {
  let cursor = 'default'

  if (props.editable && props.selectedTool === 'hand') {
    cursor = 'move'
  } else if (canInteract.value) {
    cursor = 'pointer'
  } else if (!props.editable) {
    cursor = 'not-allowed'
  }

  return {
    left: `${props.decoration.positionX || 50}%`,
    top: `${props.decoration.positionY || 50}%`,
    zIndex: props.decoration.positionZ || 0,
    transform: `translate(-50%, -50%) scale(${props.decoration.scale || 1}) rotate(${props.decoration.rotation || 0}deg)`,
    cursor
  }
})

// 图标样式
const iconStyle = computed(() => {
  const sizeMap: Record<string, string> = {
    small: '32px',
    medium: '48px',
    large: '64px'
  }

  // 从config中获取尺寸，或使用默认值
  const size = sizeMap['medium']

  return {
    fontSize: size
  }
})

// 鼠标按下
const handleMouseDown = (e: MouseEvent) => {
  if (!props.editable) return

  isDragging.value = true
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  initialX.value = props.decoration.positionX || 50
  initialY.value = props.decoration.positionY || 50

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)

  e.preventDefault()
  e.stopPropagation()
}

// 鼠标移动 (RAF-throttled for smooth dragging performance)
const handleMouseMoveThrottled = rafThrottle((e: MouseEvent) => {
  if (!isDragging.value) return

  const container = document.querySelector('.room-canvas') as HTMLElement
  if (!container) return

  const containerRect = container.getBoundingClientRect()
  const deltaX = ((e.clientX - dragStartX.value) / containerRect.width) * 100
  const deltaY = ((e.clientY - dragStartY.value) / containerRect.height) * 100

  let newX = initialX.value + deltaX
  let newY = initialY.value + deltaY

  // 限制在画布范围内
  newX = Math.max(0, Math.min(100, newX))
  newY = Math.max(0, Math.min(100, newY))

  emit('update-position', props.decoration, {
    x: Math.round(newX),
    y: Math.round(newY)
  })
})

const handleMouseMove = (e: MouseEvent) => {
  handleMouseMoveThrottled(e)
}

// 鼠标释放
const handleMouseUp = () => {
  if (isDragging.value) {
    isDragging.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }
}

// 点击交互
const handleClick = (e: MouseEvent) => {
  // 如果正在拖动，不触发交互
  if (isDragging.value) return

  // 编辑模式下不触发装饰交互
  if (props.editable) return

  // 检查是否可以交互
  if (!canInteract.value) {
    console.log('Cannot interact: tool type mismatch', {
      selectedTool: props.selectedTool,
      interactionType: props.interactionType,
      decorationInteractionType: props.decoration.interactionType
    })
    return
  }

  // 触发互动动画
  isInteracting.value = true

  // 触发粒子效果
  const decorationInteractionType = props.decoration.interactionType
  if (decorationInteractionType && interactionToParticle[decorationInteractionType]) {
    particleType.value = interactionToParticle[decorationInteractionType]
    particleTrigger.value++
    currentEffect.value = decorationInteractionType
  }

  setTimeout(() => {
    isInteracting.value = false
    currentEffect.value = ''
  }, 600)

  emit('interact', props.decoration)
  e.stopPropagation()
}
</script>

<style scoped>
.decoration-item {
  position: absolute;
  transition: transform 0.3s ease, filter 0.3s ease;
  user-select: none;
  will-change: transform, filter;
  transform: translateZ(0); /* GPU acceleration */
}

.decoration-item.editable:hover {
  filter: drop-shadow(0 0 8px rgba(64, 158, 255, 0.6));
}

/* 可交互状态 - 高亮提示 */
.decoration-item.can-interact:hover {
  filter: drop-shadow(0 0 12px rgba(102, 234, 114, 0.8));
  transform: translate(-50%, -50%) scale(1.1);
}

/* 不可交互状态 - 灰暗 */
.decoration-item.cannot-interact {
  opacity: 0.5;
  filter: grayscale(30%);
}

.decoration-item.cannot-interact:hover {
  opacity: 0.6;
}

.decoration-item:not(.editable):hover .decoration-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(-10px);
}

.decoration-item.interacting {
  animation: interact-bounce 0.6s ease;
}

/* 各种交互效果的专属动画 */
.decoration-item.effect-water .decoration-icon {
  animation: water-grow 0.8s ease;
  filter: drop-shadow(0 0 12px rgba(64, 158, 255, 0.8));
}

.decoration-item.effect-light .decoration-icon {
  animation: fire-flicker 0.8s ease;
  filter: drop-shadow(0 0 16px rgba(255, 136, 0, 1));
}

.decoration-item.effect-view .decoration-icon {
  animation: view-zoom 0.6s ease;
}

.decoration-item.effect-play_music .decoration-icon {
  animation: music-spin 1s ease;
  filter: drop-shadow(0 0 10px rgba(147, 112, 219, 0.8));
}

.decoration-item.effect-toggle .decoration-icon {
  animation: light-pulse 0.6s ease;
  filter: drop-shadow(0 0 20px rgba(255, 255, 0, 1));
}

.decoration-item.effect-blessing .decoration-icon {
  animation: blessing-shine 0.8s ease;
  filter: drop-shadow(0 0 15px rgba(255, 215, 0, 1));
}

.decoration-item.effect-pet .decoration-icon {
  animation: pet-jump 0.6s ease;
  filter: drop-shadow(0 0 12px rgba(255, 105, 180, 0.9));
}

/* 浇水动画 - 植物生长 */
@keyframes water-grow {
  0%, 100% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.15) translateY(-5px);
  }
  50% {
    transform: scale(1.1) rotate(-5deg);
  }
  70% {
    transform: scale(1.1) rotate(5deg);
  }
}

/* 点火动画 - 火焰闪烁 */
@keyframes fire-flicker {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  25% {
    transform: scale(1.2);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.15);
    opacity: 1;
  }
  75% {
    transform: scale(1.25);
    opacity: 0.8;
  }
}

/* 查看动画 - 放大 */
@keyframes view-zoom {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.5) rotate(10deg);
  }
}

/* 音乐动画 - 旋转 */
@keyframes music-spin {
  0% {
    transform: scale(1) rotate(0deg);
  }
  100% {
    transform: scale(1) rotate(360deg);
  }
}

/* 灯光动画 - 脉冲 */
@keyframes light-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.7;
  }
}

/* 祝福动画 - 闪耀 */
@keyframes blessing-shine {
  0%, 100% {
    transform: scale(1) rotate(0deg);
  }
  25% {
    transform: scale(1.2) rotate(10deg);
  }
  50% {
    transform: scale(1.3) rotate(-10deg);
  }
  75% {
    transform: scale(1.2) rotate(5deg);
  }
}

/* 抚摸动画 - 跳跃 */
@keyframes pet-jump {
  0%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-15px);
  }
  50% {
    transform: translateY(-20px) rotate(-10deg);
  }
  70% {
    transform: translateY(-10px) rotate(10deg);
  }
}

@keyframes interact-bounce {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
  }
  25% {
    transform: translate(-50%, -50%) scale(1.2) rotate(5deg);
  }
  50% {
    transform: translate(-50%, -50%) scale(0.95) rotate(-5deg);
  }
  75% {
    transform: translate(-50%, -50%) scale(1.1) rotate(3deg);
  }
}

/* 装饰图标 */
.decoration-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  transition: filter 0.3s ease;
}

.decoration-item:hover .decoration-icon {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.3));
}

/* 编辑控制按钮 */
.edit-controls {
  position: absolute;
  top: -12px;
  right: -12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.decoration-item.editable:hover .edit-controls {
  opacity: 1;
}

.remove-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 装饰名称提示 */
.decoration-tooltip {
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(0);
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  font-size: 12px;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s ease;
  pointer-events: none;
  z-index: 1000;
}

.decoration-tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: rgba(0, 0, 0, 0.8);
}

/* 互动次数徽章 */
.interaction-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
</style>
