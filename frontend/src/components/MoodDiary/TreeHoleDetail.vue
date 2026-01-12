<template>
  <el-dialog
    v-model="dialogVisible"
    title="💭 倾诉详情"
    width="650px"
    @close="handleClose"
    class="tree-hole-detail-dialog"
  >
    <div v-if="detail" class="detail-content" v-loading="loading">
      <!-- 倾诉对象 -->
      <div class="detail-header">
        <div class="speak-to">
          <span class="label">倾诉对象:</span>
          <span class="value">
            {{ getSpeakToIcon(detail.speakToType) }} {{ detail.speakToName }}
          </span>
        </div>
        <div class="status">
          <el-tag
            v-if="detail.isExpired"
            type="info"
            effect="plain"
          >
            已消失
          </el-tag>
          <el-tag
            v-else
            type="success"
          >
            {{ getExpireTypeText(detail.expireType) }}
          </el-tag>
        </div>
      </div>

      <!-- 倾诉内容 -->
      <div class="detail-body">
        <div class="content-text">{{ detail.content }}</div>

        <!-- 情绪标签 -->
        <div v-if="detail.emotionTags" class="emotion-tags">
          <el-tag
            v-for="(tag, index) in parseEmotionTags(detail.emotionTags)"
            :key="index"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 元数据 -->
      <div class="detail-footer">
        <div class="meta-item">
          <span class="meta-label">创建时间:</span>
          <span class="meta-value">{{ formatDateTime(detail.createTime) }}</span>
        </div>
        <div v-if="detail.lastViewTime" class="meta-item">
          <span class="meta-label">最后查看:</span>
          <span class="meta-value">{{ formatDateTime(detail.lastViewTime) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">查看次数:</span>
          <span class="meta-value">{{ detail.viewCount }} 次</span>
        </div>
        <div v-if="detail.expireTime && !detail.isExpired" class="meta-item">
          <span class="meta-label">消失时间:</span>
          <span class="meta-value expire-time">
            {{ formatDateTime(detail.expireTime) }}
          </span>
        </div>
        <div v-if="detail.viewCondition" class="meta-item">
          <span class="meta-label">查看条件:</span>
          <span class="meta-value">{{ getConditionText(detail.viewCondition) }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// Props
interface Props {
  modelValue: boolean
  id?: number
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 状态
const loading = ref(false)
const detail = ref<any>(null)

// 监听ID变化,加载详情
watch(() => props.id, async (newId) => {
  if (newId && props.modelValue) {
    await loadDetail(newId)
  }
}, { immediate: true })

// 加载详情
const loadDetail = async (id: number) => {
  try {
    loading.value = true
    const res = await request.get(`/patient/tree-hole/view/${id}`)

    if (res.code === 200) {
      detail.value = res.data
    } else {
      // 优雅地显示错误提示,不关闭对话框
      ElMessage.warning({
        message: res.message || '暂时无法查看这条树洞',
        duration: 3000
      })
      handleClose()
    }
  } catch (error: any) {
    // 友好的错误提示
    const errorMsg = error.response?.data?.message || error.message || '加载失败'

    // 根据错误消息显示不同的图标和提示
    if (errorMsg.includes('心情') || errorMsg.includes('条件')) {
      ElMessage({
        message: `🔒 ${errorMsg}`,
        type: 'warning',
        duration: 4000,
        showClose: true
      })
    } else {
      ElMessage.warning({
        message: errorMsg,
        duration: 3000
      })
    }

    handleClose()
  } finally {
    loading.value = false
  }
}

// 获取倾诉对象图标
const getSpeakToIcon = (type: string) => {
  const icons: Record<string, string> = {
    self: '🧘',
    person: '👤',
    role: '🎭',
    thing: '🎈',
    custom: '✨'
  }
  return icons[type] || '💬'
}

// 获取消失类型文本
const getExpireTypeText = (type: string) => {
  const texts: Record<string, string> = {
    '5sec': '⚡ 5秒后消失',
    '1hour': '⏱️ 1小时后消失',
    tonight: '🌙 今晚12点消失',
    tomorrow: '🌅 明天凌晨消失',
    forever: '💎 永久保存',
    conditional: '🔐 条件触发'
  }
  return texts[type] || type
}

// 获取条件文本
const getConditionText = (condition: string) => {
  const texts: Record<string, string> = {
    mood_low: '心情低落时(<3分)',
    mood_high: '心情高涨时(>8分)',
    after_30days: '30天后'
  }
  return texts[condition] || condition
}

// 解析情绪标签
const parseEmotionTags = (tags: string) => {
  try {
    return JSON.parse(tags)
  } catch {
    return []
  }
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  try {
    const date = new Date(time)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
  } catch {
    return time
  }
}

// 关闭对话框
const handleClose = () => {
  detail.value = null
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.tree-hole-detail-dialog {
  .detail-content {
    min-height: 200px;

    // 头部
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      margin-bottom: 20px;

      .speak-to {
        color: white;
        font-size: 16px;
        font-weight: 500;

        .label {
          opacity: 0.9;
          margin-right: 8px;
        }

        .value {
          font-weight: 600;
        }
      }
    }

    // 内容
    .detail-body {
      padding: 20px;
      background: #f5f7fa;
      border-radius: 12px;
      margin-bottom: 20px;

      .content-text {
        font-size: 15px;
        line-height: 1.8;
        color: #303133;
        white-space: pre-wrap;
        word-break: break-word;
        margin-bottom: 16px;
      }

      .emotion-tags {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
    }

    // 底部元数据
    .detail-footer {
      padding: 16px;
      background: white;
      border: 1px solid #ebeef5;
      border-radius: 8px;

      .meta-item {
        display: flex;
        justify-content: space-between;
        padding: 8px 0;
        font-size: 14px;

        &:not(:last-child) {
          border-bottom: 1px dashed #ebeef5;
        }

        .meta-label {
          color: #909399;
          font-weight: 500;
        }

        .meta-value {
          color: #606266;

          &.expire-time {
            color: #f56c6c;
            font-weight: 500;
          }
        }
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: center;
  }
}
</style>
