<template>
  <el-dialog
    v-model="dialogVisible"
    title="🌳 心情树洞"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="tree-hole-dialog"
  >
    <div class="tree-hole-editor">
      <!-- 选择倾诉对象 -->
      <div class="speak-to-section">
        <div class="section-title">💬 想对谁倾诉？</div>
        <div class="speak-to-types">
          <div
            v-for="type in speakToTypes"
            :key="type.value"
            class="type-card"
            :class="{ active: form.speakToType === type.value }"
            @click="selectType(type.value)"
          >
            <div class="card-icon">{{ type.icon }}</div>
            <div class="card-title">{{ type.label }}</div>
          </div>
        </div>

        <!-- 倾诉对象名称 -->
        <el-input
          v-model="form.speakToName"
          :placeholder="getNamePlaceholder()"
          maxlength="50"
          class="speak-to-name"
        />
      </div>

      <!-- 倾诉内容 -->
      <div class="content-section">
        <div class="section-title">✍️ 想说些什么？</div>
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          placeholder="在这里记录你的心情、想法、秘密...这些文字只有你能看到"
          maxlength="2000"
          show-word-limit
        />

        <!-- 情绪标签 -->
        <div class="emotion-tags">
          <el-tag
            v-for="tag in selectedTags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            class="emotion-tag"
          >
            {{ tag }}
          </el-tag>
          <el-button
            v-if="selectedTags.length < 5"
            size="small"
            @click="showTagSelector = true"
          >
            + 添加情绪标签
          </el-button>
        </div>

        <!-- 标签选择器 -->
        <div v-if="showTagSelector" class="tag-selector">
          <el-tag
            v-for="tag in availableTags"
            :key="tag"
            @click="addTag(tag)"
            class="selectable-tag"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>

      <!-- 消失时间设置 -->
      <div class="expire-settings">
        <div class="section-title">⏰ 何时消失？</div>
        <div class="expire-options">
          <div
            v-for="option in expireOptions"
            :key="option.value"
            class="expire-card"
            :class="{ active: form.expireType === option.value }"
            @click="selectExpireType(option.value)"
          >
            <div class="expire-icon">{{ option.icon }}</div>
            <div class="expire-label">{{ option.label }}</div>
            <div class="expire-desc">{{ option.desc }}</div>
          </div>
        </div>

        <!-- 条件触发说明 -->
        <div v-if="form.expireType === 'conditional'" class="conditional-hint">
          <el-alert
            title="条件触发"
            type="info"
            :closable="false"
          >
            <div class="condition-options">
              <el-radio-group v-model="form.viewCondition">
                <el-radio label="mood_low">当心情低落时(<3分)才能查看</el-radio>
                <el-radio label="mood_high">当心情高涨时(>8分)才能查看</el-radio>
                <el-radio label="after_30days">30天后才能查看</el-radio>
              </el-radio-group>
            </div>
          </el-alert>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          投入树洞
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { treeHoleApi } from '@/api'

// Props
const props = defineProps<{
  modelValue: boolean
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

// 对话框显示状态
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 表单数据
const form = ref({
  speakToType: 'self',
  speakToName: '',
  content: '',
  emotionTags: [] as string[],
  expireType: '1hour',
  viewCondition: 'mood_low'
})

const loading = ref(false)
const selectedTags = ref<string[]>([])
const showTagSelector = ref(false)

// 倾诉对象类型
const speakToTypes = [
  { value: 'self', label: '自己', icon: '🧘' },
  { value: 'person', label: '某人', icon: '👤' },
  { value: 'role', label: '角色', icon: '🎭' },
  { value: 'thing', label: '某物', icon: '🎈' },
  { value: 'custom', label: '自定义', icon: '✨' }
]

// 消失时间选项
const expireOptions = [
  { value: '5sec', label: '5秒消失', icon: '⚡', desc: '阅后即焚' },
  { value: '1hour', label: '1小时', icon: '⏱️', desc: '短暂保留' },
  { value: 'tonight', label: '今晚12点', icon: '🌙', desc: '今日专属' },
  { value: 'tomorrow', label: '明天凌晨', icon: '🌅', desc: '睡醒消失' },
  { value: 'forever', label: '永久保存', icon: '💎', desc: '珍贵记忆' },
  { value: 'conditional', label: '条件触发', icon: '🔐', desc: '特殊条件下查看' }
]

// 可选情绪标签
const availableTags = [
  '😊开心', '😢难过', '😡愤怒', '😰焦虑', '😌平静',
  '😴疲惫', '🤔困惑', '😍感动', '😎自信', '😖压抑',
  '🥰温暖', '😓无奈', '😨恐惧', '🤗期待', '😔失落'
]

// 获取名称输入框占位符
const getNamePlaceholder = () => {
  const placeholders: Record<string, string> = {
    self: '给自己起个昵称...',
    person: '想对谁说？(可以是真名或代号)',
    role: '想象中的角色...(如:未来的我、理想的自己)',
    thing: '什么东西？(如:我的吉他、毛绒玩具)',
    custom: '自定义一个倾诉对象...'
  }
  return placeholders[form.value.speakToType] || '请输入名称'
}

// 选择类型
const selectType = (type: string) => {
  form.value.speakToType = type
  form.value.speakToName = ''
}

// 选择消失类型
const selectExpireType = (type: string) => {
  form.value.expireType = type
}

// 添加标签
const addTag = (tag: string) => {
  if (!selectedTags.value.includes(tag) && selectedTags.value.length < 5) {
    selectedTags.value.push(tag)
  }
  showTagSelector.value = false
}

// 移除标签
const removeTag = (tag: string) => {
  selectedTags.value = selectedTags.value.filter(t => t !== tag)
}

// 提交
const handleSubmit = async () => {
  // 验证必填字段
  if (!form.value.speakToName) {
    ElMessage.warning('请输入倾诉对象名称')
    return
  }

  if (!form.value.content.trim()) {
    ElMessage.warning('请输入倾诉内容')
    return
  }

  try {
    loading.value = true

    // 准备提交数据
    const data = {
      speakToType: form.value.speakToType,
      speakToName: form.value.speakToName,
      content: form.value.content,
      emotionTags: JSON.stringify(selectedTags.value),
      expireType: form.value.expireType,
      viewCondition: form.value.expireType === 'conditional' ? form.value.viewCondition : null
    }

    await treeHoleApi.add(data)

    ElMessage.success('已投入树洞')
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('Failed to add tree hole:', error)
    ElMessage.error(error.response?.data?.message || '投入失败,请重试')
  } finally {
    loading.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  // 重置表单
  form.value = {
    speakToType: 'self',
    speakToName: '',
    content: '',
    emotionTags: [],
    expireType: '1hour',
    viewCondition: 'mood_low'
  }
  selectedTags.value = []
  showTagSelector.value = false

  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.tree-hole-dialog {
  .tree-hole-editor {
    max-height: 70vh;
    overflow-y: auto;
    padding: 10px;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  // 倾诉对象区域
  .speak-to-section {
    margin-bottom: 25px;

    .speak-to-types {
      display: flex;
      gap: 12px;
      margin-bottom: 15px;
      flex-wrap: wrap;
    }

    .type-card {
      flex: 1;
      min-width: 100px;
      padding: 15px;
      border: 2px solid #e4e7ed;
      border-radius: 12px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: #409eff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
      }

      &.active {
        border-color: #409eff;
        background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
      }

      .card-icon {
        font-size: 28px;
        margin-bottom: 8px;
      }

      .card-title {
        font-size: 14px;
        font-weight: 500;
        color: #606266;
      }
    }

    .speak-to-name {
      margin-top: 10px;
    }
  }

  // 内容区域
  .content-section {
    margin-bottom: 25px;

    .emotion-tags {
      margin-top: 12px;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;

      .emotion-tag {
        font-size: 13px;
      }
    }

    .tag-selector {
      margin-top: 12px;
      padding: 12px;
      background: #f5f7fa;
      border-radius: 8px;
      display: flex;
      gap: 8px;
      flex-wrap: wrap;

      .selectable-tag {
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          transform: scale(1.1);
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
        }
      }
    }
  }

  // 消失时间设置
  .expire-settings {
    margin-bottom: 20px;

    .expire-options {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
      margin-bottom: 15px;
    }

    .expire-card {
      padding: 15px;
      border: 2px solid #e4e7ed;
      border-radius: 12px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: #409eff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
      }

      &.active {
        border-color: #409eff;
        background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
      }

      .expire-icon {
        font-size: 24px;
        margin-bottom: 6px;
      }

      .expire-label {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
      }

      .expire-desc {
        font-size: 12px;
        color: #909399;
      }
    }

    .conditional-hint {
      margin-top: 15px;

      .condition-options {
        margin-top: 10px;

        :deep(.el-radio) {
          display: block;
          margin: 8px 0;
        }
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

// 滚动条样式
.tree-hole-editor::-webkit-scrollbar {
  width: 6px;
}

.tree-hole-editor::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;

  &:hover {
    background: #c0c4cc;
  }
}
</style>
