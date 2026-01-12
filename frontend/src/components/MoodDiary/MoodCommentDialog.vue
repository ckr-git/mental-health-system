<template>
  <el-dialog
    v-model="dialogVisible"
    title="给自己留言"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="comment-dialog">
      <!-- 选择互动类型 -->
      <div class="interaction-types">
        <div class="section-title">💭 选择心情</div>
        <div class="type-buttons">
          <div 
            v-for="type in interactionTypes" 
            :key="type.value"
            class="type-button"
            :class="{ active: form.commentType === type.value }"
            @click="selectType(type.value)"
          >
            <div class="type-icon">{{ type.icon }}</div>
            <div class="type-name">{{ type.label }}</div>
            <div class="type-desc">{{ type.desc }}</div>
          </div>
        </div>
      </div>
      
      <!-- 留言内容 -->
      <div class="comment-content">
        <div class="section-title">✍️ 留言内容</div>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          :placeholder="currentPlaceholder"
          maxlength="500"
          show-word-limit
        />
      </div>
      
      <!-- 温馨提示 -->
      <div class="tips">
        <div class="tip-item">
          <span class="tip-icon">💡</span>
          <span>留言会按时间线展示，帮你回顾当时的心情</span>
        </div>
        <div class="tip-item">
          <span class="tip-icon">🔒</span>
          <span>只有你自己可以看到这些留言</span>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleSubmit"
        :loading="submitting"
        :disabled="!form.commentType || !form.content.trim()"
      >
        发表留言
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { commentApi } from '@/api'

// Props
const props = defineProps<{
  visible: boolean
  diaryId: number
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

// 互动类型配置
const interactionTypes = [
  {
    value: 'agree',
    label: '赞同',
    icon: '👍',
    desc: '支持自己的想法',
    placeholder: '写下赞同的理由...\n例如：这个决定很对，要相信自己的判断！'
  },
  {
    value: 'disagree',
    label: '不赞同',
    icon: '🤔',
    desc: '换个角度思考',
    placeholder: '写下不同的看法...\n例如：也许可以从另一个角度看待这件事...'
  },
  {
    value: 'heartache',
    label: '心疼',
    icon: '💔',
    desc: '给自己一个拥抱',
    placeholder: '写下安慰的话...\n例如：已经很努力了，给自己一个拥抱吧'
  },
  {
    value: 'encourage',
    label: '鼓励',
    icon: '💪',
    desc: '为自己加油打气',
    placeholder: '写下鼓励的话...\n例如：你可以的！继续加油，明天会更好！'
  },
  {
    value: 'relief',
    label: '释然',
    icon: '🌈',
    desc: '放下过去向前看',
    placeholder: '写下释怀的想法...\n例如：过去的就让它过去吧，前方有更美好的风景'
  }
]

// 表单数据
const form = ref({
  commentType: '',
  content: ''
})

const submitting = ref(false)

// 当前占位符
const currentPlaceholder = computed(() => {
  const type = interactionTypes.find(t => t.value === form.value.commentType)
  return type?.placeholder || '写下你想对自己说的话...'
})

// 选择互动类型
const selectType = (type: string) => {
  form.value.commentType = type
  // 自动聚焦到输入框
  setTimeout(() => {
    const textarea = document.querySelector('.comment-content textarea') as HTMLTextAreaElement
    textarea?.focus()
  }, 100)
}

// 提交留言
const handleSubmit = async () => {
  if (!form.value.commentType) {
    ElMessage.warning('请选择心情类型')
    return
  }
  
  if (!form.value.content.trim()) {
    ElMessage.warning('请填写留言内容')
    return
  }
  
  try {
    submitting.value = true
    
    const res = await commentApi.add({
      diaryId: props.diaryId,
      commentType: form.value.commentType,
      content: form.value.content
    })
    
    if (res.code === 200) {
      ElMessage.success('留言发表成功！')
      emit('success')
      handleClose()
    }
  } catch (error) {
    console.error('Failed to submit comment:', error)
    ElMessage.error('发表留言失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  // 重置表单
  setTimeout(() => {
    form.value = {
      commentType: '',
      content: ''
    }
  }, 300)
}

// 监听对话框打开，重置表单
watch(() => props.visible, (val) => {
  if (val) {
    form.value = {
      commentType: '',
      content: ''
    }
  }
})
</script>

<style scoped>
.comment-dialog {
  padding: 0 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

/* 互动类型选择 */
.interaction-types {
  margin-bottom: 32px;
}

.type-buttons {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.type-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.type-button:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.type-button.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.type-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.type-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.type-desc {
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.4;
}

.type-button.active .type-name {
  color: #409eff;
}

/* 留言内容 */
.comment-content {
  margin-bottom: 24px;
}

.comment-content :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.8;
  border-radius: 8px;
}

/* 温馨提示 */
.tips {
  background: linear-gradient(135deg, #fff7e6 0%, #fff9f0 100%);
  border-left: 4px solid #faad14;
  padding: 16px;
  border-radius: 8px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #8c6d1f;
  line-height: 1.6;
}

.tip-item:not(:last-child) {
  margin-bottom: 8px;
}

.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .type-buttons {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }
  
  .type-button {
    padding: 12px 4px;
  }
  
  .type-icon {
    font-size: 24px;
  }
  
  .type-name {
    font-size: 12px;
  }
  
  .type-desc {
    font-size: 11px;
  }
}
</style>
