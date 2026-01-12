<template>
  <div class="interaction-toolbar">
    <div class="toolbar-title">工具箱</div>
    <div class="toolbar-items">
      <div
        v-for="tool in tools"
        :key="tool.type"
        class="tool-item"
        :class="{ 'active': selectedTool === tool.type }"
        :title="tool.name"
        @click="selectTool(tool.type)"
      >
        <div class="tool-icon">{{ tool.icon }}</div>
        <div class="tool-name">{{ tool.name }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Tool {
  type: string
  name: string
  icon: string
  interactionType: string
}

const emit = defineEmits<{
  (e: 'tool-selected', toolType: string): void
}>()

const selectedTool = ref<string>('hand')

// 工具列表 - 映射到后端的interactionType
const tools: Tool[] = [
  { type: 'hand', name: '手套', icon: '🖐️', interactionType: 'move' },
  { type: 'watering_can', name: '浇水', icon: '💧', interactionType: 'water' },
  { type: 'lighter', name: '点火', icon: '🔥', interactionType: 'light' },
  { type: 'magnifier', name: '查看', icon: '🔍', interactionType: 'view' },
  { type: 'music_note', name: '音乐', icon: '🎵', interactionType: 'play_music' },
  { type: 'switch', name: '开关', icon: '💡', interactionType: 'toggle' },
  { type: 'wand', name: '祝福', icon: '✨', interactionType: 'blessing' },
  { type: 'heart', name: '抚摸', icon: '💗', interactionType: 'pet' }
]

const selectTool = (toolType: string) => {
  selectedTool.value = toolType
  emit('tool-selected', toolType)
}

// 获取工具对应的交互类型
const getInteractionType = (toolType: string): string => {
  const tool = tools.find(t => t.type === toolType)
  return tool?.interactionType || ''
}

// 暴露方法给父组件
defineExpose({
  selectedTool,
  getInteractionType
})
</script>

<style scoped>
.interaction-toolbar {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
  z-index: 200;
  min-width: 120px;
}

.toolbar-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  text-align: center;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
}

.toolbar-items {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  border-radius: 12px;
  background: #f5f7fa;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.tool-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.tool-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.tool-item.active .tool-icon,
.tool-item.active .tool-name {
  filter: brightness(1.2);
}

.tool-icon {
  font-size: 28px;
  margin-bottom: 4px;
  transition: transform 0.3s ease;
}

.tool-item:hover .tool-icon {
  transform: scale(1.2) rotate(10deg);
}

.tool-item.active .tool-icon {
  transform: scale(1.15);
  animation: tool-pulse 1.5s ease-in-out infinite;
}

.tool-name {
  font-size: 11px;
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}

.tool-item.active .tool-name {
  color: white;
  font-weight: 600;
}

@keyframes tool-pulse {
  0%, 100% {
    transform: scale(1.15);
  }
  50% {
    transform: scale(1.25);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .interaction-toolbar {
    top: 10px;
    right: 10px;
    padding: 12px;
    min-width: 100px;
  }

  .toolbar-items {
    gap: 6px;
  }

  .tool-item {
    padding: 8px 6px;
  }

  .tool-icon {
    font-size: 24px;
  }

  .tool-name {
    font-size: 10px;
  }
}
</style>
