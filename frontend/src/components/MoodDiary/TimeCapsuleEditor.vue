<template>
  <el-dialog
    v-model="dialogVisible"
    title="✉️ 写给未来的自己"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="capsule-dialog"
  >
    <div class="capsule-editor">
      <!-- 智能推荐卡片 -->
      <LetterRecommendationCard
        v-if="showRecommendation && recommendation"
        :recommendation="recommendation"
        @apply="handleApplyRecommendation"
        @close="showRecommendation = false"
      />

      <!-- 智能推荐按钮 -->
      <div class="recommendation-section" v-if="!showRecommendation">
        <el-button
          type="primary"
          :loading="loadingRecommendation"
          @click="handleLoadRecommendation"
          plain
        >
          ✨ 获取AI智能推荐
        </el-button>
      </div>

      <!-- 选择信件类型 -->
      <div class="letter-types">
        <div class="section-title">💌 选择信件类型</div>
        <div class="type-cards">
          <div 
            v-for="type in letterTypes" 
            :key="type.value"
            class="type-card"
            :class="{ active: form.letterType === type.value }"
            @click="selectType(type.value)"
          >
            <div class="card-icon">{{ type.icon }}</div>
            <div class="card-title">{{ type.label }}</div>
            <div class="card-desc">{{ type.desc }}</div>
            <div class="card-scene" v-if="type.scene">{{ type.scene }}</div>
          </div>
        </div>
      </div>
      
      <!-- 信件内容 -->
      <div class="letter-content">
        <div class="section-title">✍️ 信件内容</div>
        
        <!-- 信件标题 -->
        <el-input
          v-model="form.title"
          placeholder="给这封信起个标题..."
          maxlength="50"
          show-word-limit
          class="letter-title-input"
        />
        
        <!-- 信件正文 -->
        <div class="letter-body">
          <div class="letter-greeting">{{ currentGreeting }}</div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            :placeholder="currentPlaceholder"
            maxlength="1000"
            show-word-limit
          />
          <div class="letter-signature">
            <span>{{ formatDate(new Date()) }} 的我</span>
          </div>
        </div>
      </div>
      
      <!-- 解锁时间设置 -->
      <div class="unlock-settings">
        <div class="section-title">⏰ 何时打开这封信？</div>
        <div class="unlock-options">
          <el-radio-group v-model="form.unlockType" class="unlock-radios">
            <el-radio label="days">指定天数后</el-radio>
            <el-radio label="date">指定日期</el-radio>
            <el-radio label="condition">特殊条件</el-radio>
          </el-radio-group>
          
          <!-- 指定天数 -->
          <div v-if="form.unlockType === 'days'" class="unlock-input">
            <el-select v-model="form.unlockDays" placeholder="选择天数">
              <el-option :value="7" label="7天后" />
              <el-option :value="15" label="15天后" />
              <el-option :value="30" label="30天后" />
              <el-option :value="60" label="60天后" />
              <el-option :value="90" label="90天后" />
              <el-option :value="180" label="半年后" />
              <el-option :value="365" label="一年后" />
            </el-select>
            <span class="unlock-hint">
              将在 {{ formatFutureDate(form.unlockDays) }} 解锁
            </span>
          </div>
          
          <!-- 指定日期 -->
          <div v-if="form.unlockType === 'date'" class="unlock-input">
            <el-date-picker
              v-model="form.unlockDate"
              type="date"
              placeholder="选择日期"
              :disabled-date="disabledDate"
              format="YYYY年MM月DD日"
            />
          </div>
          
          <!-- 特殊条件 -->
          <div v-if="form.unlockType === 'condition'" class="unlock-conditions">
            <el-checkbox-group v-model="form.unlockConditions">
              <el-checkbox label="mood_low">
                情绪低落时（心情 &lt; 3分）
              </el-checkbox>
              <el-checkbox label="mood_high">
                情绪高涨时（心情 &gt; 8分）
              </el-checkbox>
              <el-checkbox label="days_30">
                30天后自动解锁
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </div>
      
      <!-- 温馨提示 -->
      <div class="tips">
        <div class="tip-item">
          <span class="tip-icon">💡</span>
          <span>信件寄出后无法修改，解锁后可以查看和回复</span>
        </div>
        <div class="tip-item">
          <span class="tip-icon">🔒</span>
          <span>在解锁前，你将看不到信件的内容</span>
        </div>
        <div class="tip-item">
          <span class="tip-icon">💌</span>
          <span>给未来的自己一份惊喜，或是一份鼓励</span>
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button 
        type="primary" 
        @click="handleSubmit"
        :loading="submitting"
        :disabled="!canSubmit"
      >
        📮 寄出信件
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { capsuleApi } from '@/api'
import LetterRecommendationCard from './LetterRecommendationCard.vue'
import request from '@/utils/request'

// Props
const props = defineProps<{
  visible: boolean
  diaryId?: number
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

// 推荐相关状态
const recommendation = ref<any>(null)
const showRecommendation = ref(false)
const loadingRecommendation = ref(false)

// 信件类型配置
const letterTypes = [
  {
    value: 'praise',
    label: '表扬信',
    icon: '🎉',
    desc: '表扬现在努力的自己',
    scene: '心情好时写',
    greeting: '未来的我，你好！',
    placeholder: '写下你现在的成就和努力...\n例如：今天的我完成了XX，虽然很累但很开心。未来的你一定要记住这份坚持！'
  },
  {
    value: 'hope',
    label: '期望信',
    icon: '💌',
    desc: '写下对未来的期待',
    scene: '有目标时写',
    greeting: '致未来更好的我：',
    placeholder: '写下你对未来的期望...\n例如：希望30天后的你，已经实现了XX目标，变得更加自信和强大。'
  },
  {
    value: 'thanks',
    label: '感谢信',
    icon: '✉️',
    desc: '感谢未来坚持的自己',
    scene: '低谷时写',
    greeting: '未来度过难关的我：',
    placeholder: '写下对未来的感谢...\n例如：现在的我虽然很难，但我相信你已经走出来了。谢谢你的坚持，谢谢你没有放弃。'
  }
]

// 表单数据
const form = ref({
  letterType: '',
  title: '',
  content: '',
  unlockType: 'days',
  unlockDays: 30,
  unlockDate: null as Date | null,
  unlockConditions: [] as string[]
})

const submitting = ref(false)

// 当前问候语
const currentGreeting = computed(() => {
  const type = letterTypes.find(t => t.value === form.value.letterType)
  return type?.greeting || '亲爱的未来的我：'
})

// 当前占位符
const currentPlaceholder = computed(() => {
  const type = letterTypes.find(t => t.value === form.value.letterType)
  return type?.placeholder || '写下你想对未来的自己说的话...'
})

// 是否可以提交
const canSubmit = computed(() => {
  if (!form.value.letterType || !form.value.content.trim()) return false
  
  if (form.value.unlockType === 'days' && !form.value.unlockDays) return false
  if (form.value.unlockType === 'date' && !form.value.unlockDate) return false
  if (form.value.unlockType === 'condition' && form.value.unlockConditions.length === 0) return false
  
  return true
})

// 选择信件类型
const selectType = (type: string) => {
  form.value.letterType = type
  // 自动聚焦到标题输入框
  setTimeout(() => {
    const input = document.querySelector('.letter-title-input input') as HTMLInputElement
    input?.focus()
  }, 100)
}

// 禁用过去的日期
const disabledDate = (date: Date) => {
  return date.getTime() < Date.now()
}

// 格式化日期
const formatDate = (date: Date) => {
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 格式化未来日期
const formatFutureDate = (days: number) => {
  if (!days) return ''
  const future = new Date()
  future.setDate(future.getDate() + days)
  return formatDate(future)
}

// 格式化日期为 YYYY-MM-DD 格式
const formatDateToString = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 提交信件
const handleSubmit = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请完善信件内容和解锁设置')
    return
  }

  try {
    submitting.value = true

    // 构建解锁日期（统一使用 YYYY-MM-DD 格式）
    let unlockDate = null
    if (form.value.unlockType === 'days') {
      const future = new Date()
      future.setDate(future.getDate() + form.value.unlockDays)
      unlockDate = formatDateToString(future)
    } else if (form.value.unlockType === 'date') {
      if (form.value.unlockDate) {
        unlockDate = formatDateToString(form.value.unlockDate)
      }
    } else if (form.value.unlockType === 'condition') {
      // 特殊条件时，如果包含 days_30，设置30天后解锁
      if (form.value.unlockConditions.includes('days_30')) {
        const future = new Date()
        future.setDate(future.getDate() + 30)
        unlockDate = formatDateToString(future)
      } else {
        // 对于纯心情条件（mood_low/mood_high），设置今天的日期
        // 后端会根据当前心情立即判断是否解锁
        unlockDate = formatDateToString(new Date())
      }
    }

    const res = await capsuleApi.create({
      letterType: form.value.letterType,
      title: form.value.title,
      content: form.value.content,
      unlockType: form.value.unlockType,
      unlockDate: unlockDate,
      unlockConditions: JSON.stringify(form.value.unlockConditions)
    })

    if (res.code === 200) {
      ElMessage.success('📮 信件已寄出！期待未来的相遇~')
      emit('success')
      handleClose()
    }
  } catch (error) {
    console.error('Failed to send letter:', error)
    ElMessage.error('寄出信件失败')
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
      letterType: '',
      title: '',
      content: '',
      unlockType: 'days',
      unlockDays: 30,
      unlockDate: null,
      unlockConditions: []
    }
  }, 300)
}

// 监听对话框打开
watch(() => props.visible, (val) => {
  if (val) {
    // 重置表单
    form.value = {
      letterType: '',
      title: '',
      content: '',
      unlockType: 'days',
      unlockDays: 30,
      unlockDate: null,
      unlockConditions: []
    }
  }
})

// 手动加载推荐
const handleLoadRecommendation = async () => {
  try {
    loadingRecommendation.value = true
    const res = await request.get('/patient/time-capsule/recommend')
    if (res.code === 200 && res.data) {
      recommendation.value = res.data
      showRecommendation.value = true
    }
  } catch (error) {
    console.error('Failed to load recommendation:', error)
    ElMessage.warning('暂时无法获取推荐，请手动选择信件类型')
  } finally {
    loadingRecommendation.value = false
  }
}

// 应用推荐模板
const handleApplyRecommendation = (rec: any) => {
  form.value.letterType = rec.recommendType
  form.value.content = rec.template
  showRecommendation.value = false
  ElMessage.success('已应用推荐模板，请继续完善信件内容')

  // 自动聚焦到标题输入框
  setTimeout(() => {
    const input = document.querySelector('.letter-title-input input') as HTMLInputElement
    input?.focus()
  }, 100)
}
</script>

<style scoped>
.capsule-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 75vh;
  overflow-y: auto;
}

.capsule-editor {
  padding: 0 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

/* 推荐按钮 */
.recommendation-section {
  text-align: center;
  margin-bottom: 20px;
}

/* 信件类型选择 */
.letter-types {
  margin-bottom: 32px;
}

.type-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 12px;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.type-card:hover {
  border-color: #409eff;
  transform: translateY(-4px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.2);
}

.type-card.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.3);
}

.card-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.card-desc {
  font-size: 13px;
  color: #606266;
  text-align: center;
  line-height: 1.4;
  margin-bottom: 8px;
}

.card-scene {
  font-size: 12px;
  color: #909399;
  padding: 4px 12px;
  background: #f5f7fa;
  border-radius: 12px;
}

.type-card.active .card-title {
  color: #409eff;
}

/* 信件内容 */
.letter-content {
  margin-bottom: 32px;
}

.letter-title-input {
  margin-bottom: 16px;
}

.letter-title-input :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 600;
}

.letter-body {
  padding: 20px;
  background: linear-gradient(135deg, #fffbf0 0%, #fff9e6 100%);
  border: 2px dashed #ffd700;
  border-radius: 12px;
}

.letter-greeting {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.letter-body :deep(.el-textarea__inner) {
  background: transparent;
  border: none;
  font-size: 14px;
  line-height: 1.8;
  color: #303133;
}

.letter-signature {
  text-align: right;
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
  font-style: italic;
}

/* 解锁设置 */
.unlock-settings {
  margin-bottom: 24px;
}

.unlock-options {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.unlock-radios {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.unlock-input {
  display: flex;
  align-items: center;
  gap: 12px;
}

.unlock-hint {
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}

.unlock-conditions {
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.unlock-conditions :deep(.el-checkbox) {
  display: block;
  margin: 12px 0;
}

/* 温馨提示 */
.tips {
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-left: 4px solid #1890ff;
  padding: 16px;
  border-radius: 8px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #0050b3;
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
  .type-cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .unlock-radios {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
