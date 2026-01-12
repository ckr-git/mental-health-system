<template>
  <div class="tree-hole-list">
    <!-- 空状态 -->
    <el-empty
      v-if="treeHoles.length === 0 && !loading"
      description="还没有任何倾诉记录"
      :image-size="140"
    >
      <el-button type="primary" @click="emit('create')">
        🌳 开始倾诉
      </el-button>
    </el-empty>

    <!-- 树洞列表 -->
    <div v-else class="tree-holes-container" v-loading="loading">
      <div class="tree-holes-header">
        <h3 class="header-title">🌳 我的心情树洞</h3>
        <p class="header-subtitle">共 {{ treeHoles.length }} 条倾诉记录</p>
      </div>

      <!-- 按倾诉对象分组 -->
      <div class="tree-holes-groups">
        <div
          v-for="(group, key) in groupedTreeHoles"
          :key="key"
          class="group-section"
        >
          <div class="group-header">
            <span class="group-icon">{{ getGroupIcon(key) }}</span>
            <span class="group-title">{{ getGroupTitle(key) }}</span>
            <span class="group-count">({{ group.length }})</span>
          </div>

          <!-- 记录卡片 -->
          <div class="tree-hole-cards">
            <div
              v-for="hole in group"
              :key="hole.id"
              class="tree-hole-card"
              :class="{
                'is-expired': hole.isExpired,
                'can-view': hole.canView || !hole.isExpired
              }"
              @click="handleCardClick(hole)"
            >
              <!-- 卡片状态标签 -->
              <div class="card-status">
                <el-tag
                  v-if="hole.isExpired"
                  type="info"
                  size="small"
                  effect="plain"
                >
                  已消失
                </el-tag>
                <el-tag
                  v-else
                  type="success"
                  size="small"
                >
                  {{ getExpireText(hole.expireType, hole.expireTime) }}
                </el-tag>
              </div>

              <!-- 倾诉内容预览 -->
              <div class="card-content">
                <div class="content-preview">
                  {{ getPreview(hole.content) }}
                </div>

                <!-- 情绪标签 -->
                <div v-if="hole.emotionTags" class="emotion-tags">
                  <el-tag
                    v-for="(tag, index) in parseEmotionTags(hole.emotionTags)"
                    :key="index"
                    size="small"
                    effect="light"
                    :color="getEmotionColor(tag)"
                  >
                    {{ getEmotionEmoji(tag) }} {{ tag }}
                  </el-tag>
                </div>
              </div>

              <!-- 卡片底部信息 -->
              <div class="card-footer">
                <span class="footer-time">
                  {{ formatTime(hole.createTime) }}
                </span>
                <span v-if="hole.viewCount > 0" class="footer-views">
                  👁️ {{ hole.viewCount }}
                </span>
              </div>

              <!-- 删除按钮 -->
              <div class="card-actions">
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click.stop="handleDelete(hole.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// Props
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
  lastViewTime?: string
}

interface Props {
  treeHoles: TreeHole[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// Emits
const emit = defineEmits<{
  (e: 'create'): void
  (e: 'view', id: number): void
  (e: 'delete', id: number): void
  (e: 'refresh'): void
}>()

// 按倾诉对象分组
const groupedTreeHoles = computed(() => {
  const groups: Record<string, TreeHole[]> = {}

  props.treeHoles.forEach(hole => {
    const key = `${hole.speakToType}:${hole.speakToName}`
    if (!groups[key]) {
      groups[key] = []
    }
    groups[key].push(hole)
  })

  // 按创建时间排序每组内的记录
  Object.keys(groups).forEach(key => {
    groups[key].sort((a, b) =>
      new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
    )
  })

  return groups
})

// 获取分组图标
const getGroupIcon = (key: string) => {
  const [type] = key.split(':')
  const icons: Record<string, string> = {
    self: '🧘',
    person: '👤',
    role: '🎭',
    thing: '🎈',
    custom: '✨'
  }
  return icons[type] || '💬'
}

// 获取分组标题
const getGroupTitle = (key: string) => {
  const [, name] = key.split(':')
  return name
}

// 获取消失时间文本
const getExpireText = (expireType: string, expireTime?: string) => {
  const texts: Record<string, string> = {
    '5sec': '⚡ 5秒后消失',
    '1hour': '⏱️ 1小时后消失',
    tonight: '🌙 今晚12点消失',
    tomorrow: '🌅 明天凌晨消失',
    forever: '💎 永久保存',
    conditional: '🔐 条件触发'
  }

  if (expireTime && expireType !== 'forever' && expireType !== 'conditional') {
    const remaining = new Date(expireTime).getTime() - Date.now()
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
}

// 获取内容预览
const getPreview = (content: string) => {
  if (!content) return ''
  return content.length > 100 ? content.slice(0, 100) + '...' : content
}

// 解析情绪标签
const parseEmotionTags = (tags: string) => {
  try {
    return JSON.parse(tags)
  } catch {
    return []
  }
}

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

// 格式化时间
const formatTime = (time: string) => {
  try {
    const now = new Date()
    const target = new Date(time)
    const diff = now.getTime() - target.getTime()

    const seconds = Math.floor(diff / 1000)
    const minutes = Math.floor(seconds / 60)
    const hours = Math.floor(minutes / 60)
    const days = Math.floor(hours / 24)

    if (days > 30) {
      return `${Math.floor(days / 30)}个月前`
    } else if (days > 0) {
      return `${days}天前`
    } else if (hours > 0) {
      return `${hours}小时前`
    } else if (minutes > 0) {
      return `${minutes}分钟前`
    } else {
      return '刚刚'
    }
  } catch {
    return time
  }
}

// 点击卡片
const handleCardClick = (hole: TreeHole) => {
  if (hole.isExpired && !hole.canView) {
    ElMessage.warning('这条倾诉已经消失了')
    return
  }

  emit('view', hole.id)
}

// 删除记录
const handleDelete = async (id: number) => {
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

    emit('delete', id)
  } catch {
    // 用户取消
  }
}
</script>

<style scoped lang="scss">
.tree-hole-list {
  width: 100%;

  .tree-holes-container {
    .tree-holes-header {
      margin-bottom: 25px;
      text-align: center;

      .header-title {
        font-size: 24px;
        font-weight: 600;
        color: white;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        margin-bottom: 8px;
      }

      .header-subtitle {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.9);
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
      }
    }

    // 分组区域
    .tree-holes-groups {
      .group-section {
        margin-bottom: 30px;

        .group-header {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 16px;
          background: rgba(255, 255, 255, 0.95);
          backdrop-filter: blur(10px);
          border-radius: 12px;
          color: #303133;
          font-weight: 600;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

          .group-icon {
            font-size: 20px;
          }

          .group-title {
            font-size: 16px;
            flex: 1;
          }

          .group-count {
            font-size: 14px;
            opacity: 0.6;
          }
        }

        .tree-hole-cards {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
          gap: 16px;
          padding: 16px 0;
        }
      }
    }

    // 树洞卡片
    .tree-hole-card {
      position: relative;
      padding: 20px;
      background: white;
      border-radius: 16px;
      border: 2px solid #e4e7ed;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      animation: cardEnter 0.5s ease-out backwards;

      @keyframes cardEnter {
        from {
          opacity: 0;
          transform: translateY(20px) scale(0.95);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }

      &:hover {
        transform: translateY(-8px) scale(1.02);
        box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
        border-color: #409eff;
      }

      &.is-expired {
        opacity: 0.6;
        filter: grayscale(0.5);
        background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
      }

      &.can-view {
        border-color: #67c23a;
        background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
      }

      .card-status {
        position: absolute;
        top: 12px;
        right: 12px;
      }

      .card-content {
        margin-bottom: 16px;
        padding-right: 80px;

        .content-preview {
          font-size: 14px;
          line-height: 1.6;
          color: #606266;
          margin-bottom: 12px;
          word-break: break-all;
        }

        .emotion-tags {
          display: flex;
          gap: 6px;
          flex-wrap: wrap;

          .el-tag {
            font-size: 12px;
          }
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 12px;
        border-top: 1px solid #ebeef5;
        font-size: 12px;
        color: #909399;

        .footer-time {
          flex: 1;
        }

        .footer-views {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }

      .card-actions {
        position: absolute;
        bottom: 16px;
        right: 16px;
        opacity: 0;
        transition: opacity 0.2s;
      }

      &:hover .card-actions {
        opacity: 1;
      }
    }
  }
}
</style>
