<template>
  <div class="assessment-page">
    <!-- 量表选择视图 -->
    <template v-if="view === 'scales'">
      <div class="page-header">
        <h2>心理评估</h2>
        <p class="subtitle">选择量表开始专业心理评估，了解您的心理健康状况</p>
      </div>

      <div class="scale-grid" v-loading="store.scalesLoading">
        <div
          v-for="scale in store.scales"
          :key="scale.id"
          class="scale-card"
          @click="showIntro(scale)"
        >
          <div class="scale-icon">{{ getScaleIcon(scale.scaleCode) }}</div>
          <h3>{{ scale.scaleName }}</h3>
          <p class="scale-desc">{{ scale.introText }}</p>
          <div class="scale-meta">
            <span>{{ scale.itemCount }} 题</span>
            <span>约 {{ scale.estimatedMinutes }} 分钟</span>
          </div>
          <el-button type="primary" class="start-btn">开始评估</el-button>
        </div>
      </div>

      <!-- 历史记录 -->
      <div class="history-section">
        <h3>评估历史</h3>
        <el-table
          v-if="store.history && store.history.records.length > 0"
          :data="store.history.records"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="scaleName" label="量表" min-width="140" />
          <el-table-column prop="totalScore" label="总分" width="80" align="center" />
          <el-table-column label="严重程度" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getSeverityTagType(row.severityLevel)" size="small">
                {{ row.severityLevel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评估时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.submittedAt) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无评估记录" />
        <el-pagination
          v-if="store.history && store.history.total > 10"
          :current-page="historyPage"
          :page-size="10"
          :total="store.history.total"
          layout="prev, pager, next"
          class="history-pagination"
          @current-change="onHistoryPageChange"
        />
      </div>
    </template>

    <!-- 量表介绍弹窗 -->
    <el-dialog
      v-model="introVisible"
      :title="selectedScale?.scaleName"
      width="480px"
      :close-on-click-modal="false"
    >
      <p class="intro-text">{{ selectedScale?.introText }}</p>
      <div class="intro-meta">
        <div class="meta-item">
          <span class="meta-label">题目数量</span>
          <span class="meta-value">{{ selectedScale?.itemCount }} 题</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">预计时间</span>
          <span class="meta-value">约 {{ selectedScale?.estimatedMinutes }} 分钟</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="introVisible = false">取消</el-button>
        <el-button type="primary" @click="beginAssessment" :loading="starting">
          开始答题
        </el-button>
      </template>
    </el-dialog>

    <!-- 答题视图 -->
    <template v-if="view === 'quiz'">
      <div class="quiz-container" v-if="store.currentScale">
        <!-- 进度条 -->
        <div class="quiz-header">
          <el-button text @click="confirmExit">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <div class="progress-info">
            <span>{{ store.currentScale.scaleName }}</span>
            <span class="progress-text">
              {{ store.currentQuestionIndex + 1 }} / {{ store.currentScale.items.length }}
            </span>
          </div>
          <div class="autosave-indicator" v-if="store.autosaving">
            自动保存中...
          </div>
        </div>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="6"
          :show-text="false"
          color="#FF6B6B"
          class="quiz-progress"
        />

        <!-- 题目卡片 -->
        <div class="question-card" v-if="currentItem">
          <h3 class="question-text">{{ currentItem.questionText }}</h3>
          <div class="options-list">
            <div
              v-for="option in currentOptions"
              :key="option.value"
              class="option-item"
              :class="{ selected: currentAnswer?.answerValue === option.value }"
              @click="selectOption(option)"
            >
              <div class="option-radio">
                <div class="radio-inner" v-if="currentAnswer?.answerValue === option.value" />
              </div>
              <span class="option-label">{{ option.label }}</span>
            </div>
          </div>
        </div>

        <!-- 导航按钮 -->
        <div class="quiz-nav">
          <el-button
            @click="prevQuestion"
            :disabled="store.currentQuestionIndex === 0"
          >
            上一题
          </el-button>
          <el-button
            v-if="store.currentQuestionIndex < store.currentScale.items.length - 1"
            type="primary"
            @click="nextQuestion"
            :disabled="!currentAnswer"
          >
            下一题
          </el-button>
          <el-button
            v-else
            type="primary"
            @click="handleSubmit"
            :loading="submitting"
            :disabled="!allAnswered"
          >
            提交评估
          </el-button>
        </div>
      </div>
    </template>

    <!-- 结果视图 -->
    <template v-if="view === 'result'">
      <div class="result-container" v-if="store.result">
        <div class="result-header">
          <div class="result-icon">{{ getResultEmoji(store.result.severityLevel) }}</div>
          <h2>{{ store.result.scaleName }}</h2>
          <p class="result-subtitle">评估完成</p>
        </div>

        <div class="result-score-card">
          <div class="score-circle" :class="getSeverityClass(store.result.severityLevel)">
            <span class="score-number">{{ store.result.totalScore }}</span>
            <span class="score-label">总分</span>
          </div>
          <div class="severity-badge" :class="getSeverityClass(store.result.severityLevel)">
            {{ store.result.severityLevel }}
          </div>
        </div>

        <div class="result-interpretation">
          <h3>评估解读</h3>
          <p>{{ store.result.interpretation }}</p>
        </div>

        <div class="result-actions">
          <el-button type="primary" @click="backToScales">返回量表列表</el-button>
          <el-button @click="retakeAssessment">重新评估</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAssessmentStore } from '@/stores/assessment'
import type { ScaleListItem, AnswerOption } from '@/types'

const store = useAssessmentStore()

const view = ref<'scales' | 'quiz' | 'result'>('scales')
const introVisible = ref(false)
const selectedScale = ref<ScaleListItem | null>(null)
const starting = ref(false)
const submitting = ref(false)
const historyPage = ref(1)
const lastScaleCode = ref('')

onMounted(() => {
  store.loadScales()
  store.loadHistory()
})

const getScaleIcon = (code: string) => {
  const icons: Record<string, string> = {
    PHQ9: '💙', GAD7: '💛', SDS: '🩵', SAS: '🧡'
  }
  return icons[code] || '📋'
}

const showIntro = (scale: ScaleListItem) => {
  selectedScale.value = scale
  introVisible.value = true
}

const beginAssessment = async () => {
  if (!selectedScale.value) return
  starting.value = true
  try {
    const res = await store.startSession(selectedScale.value.scaleCode)
    if (res.code === 200) {
      lastScaleCode.value = selectedScale.value.scaleCode
      introVisible.value = false
      view.value = 'quiz'
    } else {
      ElMessage.error(res.message || '开始评估失败')
    }
  } catch {
    ElMessage.error('开始评估失败')
  } finally {
    starting.value = false
  }
}

const currentItem = computed(() => {
  if (!store.currentScale) return null
  return store.currentScale.items[store.currentQuestionIndex]
})

const currentOptions = computed<AnswerOption[]>(() => {
  if (!currentItem.value) return []
  return store.parseAnswerOptions(currentItem.value.answerOptions)
})

const currentAnswer = computed(() => {
  if (!currentItem.value) return null
  return store.answers.get(currentItem.value.id) || null
})

const progressPercent = computed(() => {
  if (!store.currentScale) return 0
  return Math.round(((store.currentQuestionIndex + 1) / store.currentScale.items.length) * 100)
})

const allAnswered = computed(() => {
  if (!store.currentScale) return false
  return store.currentScale.items.every(item =>
    item.requiredFlag !== 1 || store.answers.has(item.id)
  )
})

const selectOption = async (option: AnswerOption) => {
  if (!currentItem.value) return
  await store.saveAnswer(currentItem.value.id, option.value, option.label)
}

const nextQuestion = () => {
  if (!store.currentScale) return
  if (store.currentQuestionIndex < store.currentScale.items.length - 1) {
    store.currentQuestionIndex++
  }
}

const prevQuestion = () => {
  if (store.currentQuestionIndex > 0) {
    store.currentQuestionIndex--
  }
}

const handleSubmit = async () => {
  if (!allAnswered.value) {
    ElMessage.warning('请完成所有必答题后再提交')
    return
  }
  submitting.value = true
  try {
    const res = await store.submitSession()
    if (res && res.code === 200) {
      view.value = 'result'
    } else {
      ElMessage.error(res?.message || '提交失败')
    }
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const confirmExit = () => {
  ElMessageBox.confirm('退出将丢失当前答题进度，确定退出吗？', '提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '继续答题',
    type: 'warning'
  }).then(() => {
    store.resetSession()
    view.value = 'scales'
  })
}

const backToScales = () => {
  store.resetSession()
  store.loadHistory()
  view.value = 'scales'
}

const retakeAssessment = async () => {
  if (lastScaleCode.value) {
    starting.value = true
    try {
      const res = await store.startSession(lastScaleCode.value)
      if (res.code === 200) {
        view.value = 'quiz'
      }
    } finally {
      starting.value = false
    }
  }
}

const onHistoryPageChange = (page: number) => {
  historyPage.value = page
  store.loadHistory(page)
}

const getSeverityTagType = (level: string) => {
  const map: Record<string, string> = {
    '极轻微': 'success', '轻度': 'info', '中度': 'warning',
    '中重度': 'danger', '重度': 'danger'
  }
  return map[level] || 'info'
}

const getSeverityClass = (level: string) => {
  const map: Record<string, string> = {
    '极轻微': 'severity-minimal', '轻度': 'severity-mild',
    '中度': 'severity-moderate', '中重度': 'severity-moderately-severe',
    '重度': 'severity-severe'
  }
  return map[level] || 'severity-minimal'
}

const getResultEmoji = (level: string) => {
  const map: Record<string, string> = {
    '极轻微': '🌟', '轻度': '🌤️', '中度': '🌥️',
    '中重度': '🌧️', '重度': '⛈️'
  }
  return map[level] || '📊'
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.assessment-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 22px;
  color: #2D3436;
  margin: 0 0 8px;
}

.subtitle {
  color: #636E72;
  font-size: 14px;
  margin: 0;
}

/* Scale Grid */
.scale-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.scale-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #FFE4E4;
  text-align: center;
}

.scale-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(255, 107, 107, 0.15);
  border-color: #FF6B6B;
}

.scale-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.scale-card h3 {
  font-size: 16px;
  color: #2D3436;
  margin: 0 0 8px;
}

.scale-desc {
  font-size: 13px;
  color: #636E72;
  line-height: 1.5;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scale-meta {
  display: flex;
  justify-content: center;
  gap: 16px;
  font-size: 12px;
  color: #B2BEC3;
  margin-bottom: 16px;
}

.start-btn {
  width: 100%;
  border-radius: 8px;
}

/* History */
.history-section {
  margin-top: 16px;
}

.history-section h3 {
  font-size: 18px;
  color: #2D3436;
  margin: 0 0 16px;
}

.history-pagination {
  margin-top: 16px;
  justify-content: center;
}

/* Intro Dialog */
.intro-text {
  font-size: 14px;
  color: #636E72;
  line-height: 1.7;
}

.intro-meta {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #B2BEC3;
}

.meta-value {
  font-size: 16px;
  font-weight: 600;
  color: #2D3436;
}

/* Quiz */
.quiz-container {
  max-width: 640px;
  margin: 0 auto;
}

.quiz-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.progress-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 14px;
  color: #2D3436;
}

.progress-text {
  font-size: 12px;
  color: #B2BEC3;
}

.autosave-indicator {
  font-size: 12px;
  color: #B2BEC3;
}

.quiz-progress {
  margin-bottom: 32px;
}

.question-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  border: 1px solid #FFE4E4;
  margin-bottom: 24px;
}

.question-text {
  font-size: 18px;
  color: #2D3436;
  margin: 0 0 24px;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 12px;
  border: 2px solid #F0F0F0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: #FFB8B8;
  background: #FFF5F5;
}

.option-item.selected {
  border-color: #FF6B6B;
  background: linear-gradient(135deg, #FFF5F5, #FFE4E4);
}

.option-radio {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #DFE6E9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.option-item.selected .option-radio {
  border-color: #FF6B6B;
}

.radio-inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #FF6B6B;
}

.option-label {
  font-size: 15px;
  color: #2D3436;
}

.quiz-nav {
  display: flex;
  justify-content: space-between;
}

/* Result */
.result-container {
  max-width: 560px;
  margin: 0 auto;
  text-align: center;
}

.result-header {
  margin-bottom: 32px;
}

.result-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.result-header h2 {
  font-size: 22px;
  color: #2D3436;
  margin: 0 0 4px;
}

.result-subtitle {
  color: #636E72;
  font-size: 14px;
  margin: 0;
}

.result-score-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 4px solid;
}

.score-number {
  font-size: 36px;
  font-weight: 700;
}

.score-label {
  font-size: 12px;
  color: #636E72;
}

.severity-badge {
  padding: 6px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.severity-minimal { border-color: #00B894; color: #00B894; background: #E8F8F5; }
.severity-mild { border-color: #0984E3; color: #0984E3; background: #EBF5FB; }
.severity-moderate { border-color: #FDCB6E; color: #F39C12; background: #FEF9E7; }
.severity-moderately-severe { border-color: #E17055; color: #E17055; background: #FDEDEC; }
.severity-severe { border-color: #D63031; color: #D63031; background: #FADBD8; }

.result-interpretation {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #FFE4E4;
  margin-bottom: 24px;
  text-align: left;
}

.result-interpretation h3 {
  font-size: 16px;
  color: #2D3436;
  margin: 0 0 12px;
}

.result-interpretation p {
  font-size: 14px;
  color: #636E72;
  line-height: 1.7;
  margin: 0;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .scale-grid {
    grid-template-columns: 1fr;
  }
  .question-card {
    padding: 20px;
  }
  .question-text {
    font-size: 16px;
  }
}
</style>
