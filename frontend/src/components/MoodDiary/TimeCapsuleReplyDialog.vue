<template>
  <el-dialog
    v-model="dialogVisible"
    title="💬 回复这封信"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="reply-dialog"
  >
    <div class="reply-editor" v-if="letter">
      <!-- 原信件预览 -->
      <div class="original-letter">
        <div class="section-label">📖 原信内容</div>
        <div class="original-content">
          <div class="letter-header">
            <span class="letter-icon">{{ getLetterIcon(letter.letterType) }}</span>
            <span class="letter-title">{{ letter.title || '给未来的自己' }}</span>
          </div>
          <div class="letter-body">
            <div class="letter-greeting">{{ getGreeting(letter.letterType) }}</div>
            <div class="letter-text">{{ letter.content }}</div>
            <div class="letter-date">{{ formatDate(letter.createTime) }} 的我</div>
          </div>
        </div>
      </div>

      <!-- 时光跨度 -->
      <div class="time-span">
        <div class="span-icon">⏱️</div>
        <div class="span-text">
          距离写信已经过去了 <span class="highlight">{{ getDaysSpan(letter.createTime) }}</span> 天
        </div>
      </div>

      <!-- 回复内容 -->
      <div class="reply-section">
        <div class="section-label">✍️ 现在的你想说</div>
        <el-input
          v-model="replyContent"
          type="textarea"
          :rows="8"
          placeholder="回复过去的自己...&#10;&#10;例如：&#10;- 那时候的困难现在已经克服了&#10;- 感谢过去的自己没有放弃&#10;- 现在的我过得怎么样..."
          maxlength="1000"
          show-word-limit
          class="reply-textarea"
        />
      </div>

      <!-- 温馨提示 -->
      <div class="tips">
        <div class="tip-icon">💡</div>
        <div class="tip-text">
          这是你与过去的自己的对话。记录你的成长和感悟，留下时光的印记。
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitting"
        :disabled="!replyContent.trim()"
      >
        💌 发送回复
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// Props
const props = defineProps<{
  visible: boolean
  letter: any
}>()

// Emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 回复内容
const replyContent = ref('')
const submitting = ref(false)

// 监听letter变化，预填充已有回复内容
watch(() => props.letter, (newLetter) => {
  if (newLetter && newLetter.replyContent) {
    replyContent.value = newLetter.replyContent
  }
}, { immediate: true })

// 信件类型配置
const letterTypeMap: Record<string, any> = {
  praise: { name: '表扬信', icon: '🎉', greeting: '未来的我，你好！' },
  hope: { name: '期望信', icon: '💌', greeting: '致未来更好的我：' },
  thanks: { name: '感谢信', icon: '✉️', greeting: '未来度过难关的我：' },
  goal: { name: '目标信', icon: '🎯', greeting: '未来的我：' }
}

// 获取信件图标
const getLetterIcon = (type: string) => {
  return letterTypeMap[type]?.icon || '💌'
}

// 获取问候语
const getGreeting = (type: string) => {
  return letterTypeMap[type]?.greeting || '亲爱的未来的我：'
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 计算天数跨度
const getDaysSpan = (createTime: string) => {
  if (!createTime) return 0
  const created = new Date(createTime)
  const now = new Date()
  const diff = now.getTime() - created.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
}

// 提交回复
const handleSubmit = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    submitting.value = true
    const res = await request.post(`/patient/time-capsule/reply/${props.letter.id}`, {
      replyContent: replyContent.value
    })

    if (res.code === 200) {
      ElMessage.success('💌 回复已保存！')
      emit('success')
      handleClose()
    }
  } catch (error) {
    console.error('Failed to reply letter:', error)
    ElMessage.error('回复失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  // 重置表单
  setTimeout(() => {
    replyContent.value = ''
  }, 300)
}
</script>

<style scoped lang="scss">
.reply-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.reply-editor {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

/* 原信件预览 */
.original-letter {
  .original-content {
    background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
    border-left: 4px solid #409eff;
    border-radius: 12px;
    padding: 16px;
  }

  .letter-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ed;

    .letter-icon {
      font-size: 24px;
    }

    .letter-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .letter-body {
    .letter-greeting {
      font-size: 14px;
      font-weight: 600;
      color: #606266;
      margin-bottom: 8px;
    }

    .letter-text {
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
      white-space: pre-wrap;
      margin-bottom: 12px;
      max-height: 200px;
      overflow-y: auto;
    }

    .letter-date {
      text-align: right;
      font-size: 13px;
      color: #909399;
      font-style: italic;
    }
  }
}

/* 时光跨度 */
.time-span {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff7e6 0%, #fff3d6 100%);
  border-left: 4px solid #f59e0b;
  border-radius: 8px;

  .span-icon {
    font-size: 24px;
  }

  .span-text {
    font-size: 14px;
    color: #b45309;
    line-height: 1.6;

    .highlight {
      font-size: 18px;
      font-weight: 700;
      color: #d97706;
      margin: 0 4px;
    }
  }
}

/* 回复区域 */
.reply-section {
  .reply-textarea :deep(.el-textarea__inner) {
    font-size: 14px;
    line-height: 1.8;
    border-radius: 12px;
    padding: 16px;
    background: linear-gradient(135deg, #fffbf0 0%, #fff9e6 100%);
    border: 2px dashed #ffd700;

    &:focus {
      border-color: #409eff;
      background: white;
    }
  }
}

/* 温馨提示 */
.tips {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  border-left: 4px solid #1890ff;
  border-radius: 8px;

  .tip-icon {
    font-size: 20px;
    flex-shrink: 0;
  }

  .tip-text {
    font-size: 13px;
    color: #0050b3;
    line-height: 1.6;
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .reply-dialog {
    width: 95vw !important;
  }

  .time-span {
    flex-direction: column;
    text-align: center;
  }
}
</style>
