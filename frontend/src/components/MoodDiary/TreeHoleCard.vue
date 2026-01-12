<template>
  <div class="tree-hole-card" @click="handleClick">
    <!-- 状态徽章 -->
    <div class="status-badge" v-if="isExpired">
      已消失
    </div>

    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="speak-to-info">
        <span class="speak-icon">{{ getSpeakIcon }}</span>
        <span class="speak-text">对{{ treeHole.speakToName }}说</span>
      </div>
      <div class="time-info">
        {{ formatTime(treeHole.createTime) }}
      </div>
    </div>

    <!-- 卡片内容 -->
    <div class="card-content">
      <p class="content-excerpt">{{ excerpt }}</p>
    </div>

    <!-- 情绪标签 -->
    <div class="emotion-tags" v-if="emotionTags.length > 0">
      <el-tag
        v-for="(tag, index) in emotionTags"
        :key="index"
        size="small"
        effect="light"
        :color="getEmotionColor(tag)"
      >
        {{ getEmotionEmoji(tag) }} {{ tag }}
      </el-tag>
    </div>

    <!-- 卡片底部 -->
    <div class="card-footer">
      <!-- 过期时间 -->
      <div class="expire-tag">
        {{ getExpireText }}
      </div>

      <!-- 统计信息 -->
      <div class="stats">
        <span class="stat-item" v-if="treeHole.viewCount > 0">
          👁️ {{ treeHole.viewCount }}
        </span>
      </div>
    </div>

    <!-- 删除按钮 -->
    <div class="card-actions" @click.stop>
      <el-button
        type="danger"
        size="small"
        text
        @click="handleDelete"
      >
        删除
      </el-button>
    </div>

    <!-- 悬浮交互提示 -->
    <div class="hover-hint">点击查看详情</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'

interface TreeHole {
  id: number
  speakToType: string
  speakToName: string
  content: string
  emotionTags?: string
  expireType: string
  expireTime?: string
  isExpired: number
  canView: number
  viewCount: number
  createTime: string
}

interface Props {
  treeHole: TreeHole
  maxExcerptLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxExcerptLength: 100
})

const emit = defineEmits<{
  click: [id: number]
  delete: [id: number]
}>()

// 倾诉对象图标
const speakToIcons: Record<string, string> = {
  self: '🪞',
  person: '👤',
  role: '🎭',
  thing: '🎈',
  custom: '✨'
}

const getSpeakIcon = computed(() => {
  return speakToIcons[props.treeHole.speakToType] || '💬'
})

const isExpired = computed(() => props.treeHole.isExpired === 1)

const emotionTags = computed(() => {
  if (!props.treeHole.emotionTags) return []
  try {
    return JSON.parse(props.treeHole.emotionTags)
  } catch {
    return []
  }
})

const excerpt = computed(() => {
  const content = props.treeHole.content || ''
  if (content.length <= props.maxExcerptLength) {
    return content
  }
  return content.substring(0, props.maxExcerptLength) + '...'
})

// 获取情绪表情
const getEmotionEmoji = (emotion: string) => {
  const emojiMap: Record<string, string> = {
    '快乐': '😊',
    '平静': '😌',
    '悲伤': '😢',
    '愤怒': '😠',
    '焦虑': '😰',
    '压力': '😖',
    '失落': '😞',
    '兴奋': '🤩',
    '恐惧': '😨',
    '感恩': '🥰'
  }
  return emojiMap[emotion] || '💭'
}

// 获取情绪颜色
const getEmotionColor = (emotion: string) => {
  const colorMap: Record<string, string> = {
    '快乐': '#67c23a',
    '平静': '#409eff',
    '悲伤': '#909399',
    '愤怒': '#f56c6c',
    '焦虑': '#e6a23c',
    '压力': '#f56c6c',
    '失落': '#909399',
    '兴奋': '#e6a23c',
    '恐惧': '#909399',
    '感恩': '#67c23a'
  }
  return colorMap[emotion] || '#909399'
}

// 获取消失时间文本
const getExpireText = computed(() => {
  const expireType = props.treeHole.expireType
  const texts: Record<string, string> = {
    '5sec': '⚡ 5秒后消失',
    '1hour': '⏱️ 1小时后消失',
    'tonight': '🌙 今晚12点消失',
    'tomorrow': '🌅 明天凌晨消失',
    'forever': '💎 永久保存',
    'conditional': '🔐 条件触发'
  }

  if (props.treeHole.expireTime && expireType !== 'forever' && expireType !== 'conditional') {
    const remaining = new Date(props.treeHole.expireTime).getTime() - Date.now()
    if (remaining > 0) {
      const hours = Math.floor(remaining / (1000 * 60 * 60))
      const minutes = Math.floor((remaining % (1000 * 60 * 60)) / (1000 * 60))
      if (hours > 0) {
        return `${hours}小时${minutes}分后消失`
      }
      return `${minutes}分钟后消失`
    }
  }

  return texts[expireType] || '未知'
})

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const handleClick = () => {
  if (!isExpired.value || props.treeHole.canView === 1) {
    emit('click', props.treeHole.id)
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条倾诉记录吗?',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    emit('delete', props.treeHole.id)
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.tree-hole-card {
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

.tree-hole-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.tree-hole-card:hover .hover-hint {
  opacity: 1;
  transform: translateY(0);
}

.tree-hole-card:hover .card-actions {
  opacity: 1;
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
  background: #909399;
  z-index: 1;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.speak-to-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.speak-icon {
  font-size: 24px;
  line-height: 1;
}

.speak-text {
  font-size: 14px;
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

.content-excerpt {
  font-size: 14px;
  color: #4a5568;
  line-height: 1.6;
  margin: 0;
  word-break: break-all;
}

/* 情绪标签 */
.emotion-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 12px 0;
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

.expire-tag {
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

/* 删除按钮 */
.card-actions {
  position: absolute;
  bottom: 16px;
  right: 16px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 2;
}

/* 悬浮提示 */
.hover-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: linear-gradient(135deg, #4a7c4e 0%, #8b7355 100%);
  color: white;
  text-align: center;
  font-size: 12px;
  opacity: 0;
  transform: translateY(100%);
  transition: all 0.3s ease;
}
</style>
