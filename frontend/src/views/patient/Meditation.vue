<template>
  <div class="meditation-page">
    <!-- Exercise List View -->
    <template v-if="view === 'list'">
      <div class="page-header">
        <h2>正念冥想</h2>
        <p class="subtitle">通过呼吸练习和正念冥想，缓解焦虑、改善睡眠</p>
        <div class="stats-row" v-if="stats">
          <div class="stat-chip">{{ stats.completedSessions }} 次练习</div>
          <div class="stat-chip">{{ stats.totalMinutes }} 分钟</div>
        </div>
      </div>

      <div class="exercise-grid" v-loading="loading">
        <div v-for="ex in exercises" :key="ex.id" class="exercise-card" @click="startExercise(ex)">
          <div class="ex-category">{{ getCategoryLabel(ex.category) }}</div>
          <h3>{{ ex.title }}</h3>
          <p class="ex-desc">{{ ex.description }}</p>
          <div class="ex-meta">
            <span>{{ Math.ceil(ex.durationSeconds / 60) }} 分钟</span>
            <span v-if="ex.inhaleSeconds">{{ ex.inhaleSeconds }}-{{ ex.holdSeconds || 0 }}-{{ ex.exhaleSeconds }}-{{ ex.restSeconds || 0 }}</span>
          </div>
          <el-button type="primary" round size="small" class="ex-start-btn">开始练习</el-button>
        </div>
      </div>
    </template>

    <!-- Player View -->
    <template v-if="view === 'player' && currentExercise">
      <div class="player-container">
        <el-button text class="back-btn" @click="exitPlayer">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>

        <h2 class="player-title">{{ currentExercise.title }}</h2>

        <!-- Breathing Animation -->
        <div class="breathing-area">
          <div class="breath-circle" :class="[phase, { paused: !playing }]"
               :style="breathCircleStyle">
            <div class="breath-label">{{ phaseLabel }}</div>
            <div class="breath-timer">{{ phaseSeconds }}</div>
          </div>
        </div>

        <!-- Time Progress -->
        <div class="time-display">
          <span>{{ formatSeconds(elapsedSeconds) }}</span>
          <el-progress :percentage="progressPercent" :stroke-width="4" :show-text="false" color="#FF6B6B" />
          <span>{{ formatSeconds(currentExercise.durationSeconds) }}</span>
        </div>

        <!-- Instructions -->
        <div class="instructions" v-if="instructions.length > 0">
          <div class="instruction-step" v-for="(step, i) in instructions" :key="i"
               :class="{ active: i === currentStep }">
            {{ step }}
          </div>
        </div>

        <!-- Controls -->
        <div class="player-controls">
          <el-button circle size="large" @click="togglePlay" :type="playing ? '' : 'primary'">
            {{ playing ? '⏸' : '▶' }}
          </el-button>
          <el-button circle @click="completeEarly" v-if="elapsedSeconds > 30">
            ✓
          </el-button>
        </div>
      </div>
    </template>

    <!-- Complete View -->
    <template v-if="view === 'complete'">
      <div class="complete-container">
        <div class="complete-icon">🌿</div>
        <h2>练习完成</h2>
        <p>{{ currentExercise?.title }}</p>
        <div class="complete-stats">
          <div class="stat-item">
            <span class="stat-value">{{ formatSeconds(completedSeconds) }}</span>
            <span class="stat-label">练习时长</span>
          </div>
        </div>
        <div class="complete-actions">
          <el-button type="primary" @click="backToList">返回列表</el-button>
          <el-button @click="restartExercise">再来一次</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { meditationApi, type MeditationExercise } from '@/api/modules/patient/meditation'

const view = ref<'list' | 'player' | 'complete'>('list')
const exercises = ref<MeditationExercise[]>([])
const loading = ref(false)
const stats = ref<{ totalMinutes: number; completedSessions: number } | null>(null)
const currentExercise = ref<MeditationExercise | null>(null)
const sessionId = ref<number | null>(null)

const playing = ref(false)
const phase = ref<'inhale' | 'hold' | 'exhale' | 'rest' | 'idle'>('idle')
const phaseSeconds = ref(0)
const elapsedSeconds = ref(0)
const completedSeconds = ref(0)
const currentStep = ref(0)
const instructions = ref<string[]>([])
let timerInterval: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  loading.value = true
  try {
    const [exRes, stRes] = await Promise.all([meditationApi.getExercises(), meditationApi.getStats()])
    if (exRes.code === 200) exercises.value = exRes.data
    if (stRes.code === 200) stats.value = stRes.data
  } finally { loading.value = false }
})

onUnmounted(() => { if (timerInterval) clearInterval(timerInterval) })

const phaseLabel = computed(() => {
  return { inhale: '吸气', hold: '屏息', exhale: '呼气', rest: '休息', idle: '准备' }[phase.value]
})

const progressPercent = computed(() => {
  if (!currentExercise.value) return 0
  return Math.min(100, Math.round((elapsedSeconds.value / currentExercise.value.durationSeconds) * 100))
})

const breathCircleStyle = computed(() => {
  const scale = phase.value === 'inhale' ? 1.4 : phase.value === 'hold' ? 1.4 : phase.value === 'exhale' ? 0.8 : 1.0
  return { transform: `scale(${scale})`, transition: `transform ${phaseSeconds.value}s ease-in-out` }
})

const startExercise = async (ex: MeditationExercise) => {
  currentExercise.value = ex
  try { instructions.value = JSON.parse(ex.instructionSteps) } catch { instructions.value = [] }

  const res = await meditationApi.startSession(ex.id)
  if (res.code === 200) {
    sessionId.value = res.data
    view.value = 'player'
    elapsedSeconds.value = 0
    currentStep.value = 0
    if (ex.inhaleSeconds && ex.inhaleSeconds > 0) {
      startBreathingCycle()
    } else {
      phase.value = 'idle'
      playing.value = true
      startTimer()
    }
  }
}

const startBreathingCycle = () => {
  const ex = currentExercise.value!
  phase.value = 'inhale'
  phaseSeconds.value = ex.inhaleSeconds || 4
  playing.value = true
  startTimer()
}

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    if (!playing.value) return
    elapsedSeconds.value++
    phaseSeconds.value = Math.max(0, phaseSeconds.value - 1)

    const stepInterval = currentExercise.value
      ? Math.floor(currentExercise.value.durationSeconds / Math.max(instructions.value.length, 1))
      : 30
    currentStep.value = Math.min(instructions.value.length - 1, Math.floor(elapsedSeconds.value / stepInterval))

    if (phaseSeconds.value <= 0 && currentExercise.value?.inhaleSeconds) {
      advancePhase()
    }

    if (currentExercise.value && elapsedSeconds.value >= currentExercise.value.durationSeconds) {
      finishExercise()
    }
  }, 1000)
}

const advancePhase = () => {
  const ex = currentExercise.value!
  if (phase.value === 'inhale') {
    if (ex.holdSeconds && ex.holdSeconds > 0) { phase.value = 'hold'; phaseSeconds.value = ex.holdSeconds }
    else { phase.value = 'exhale'; phaseSeconds.value = ex.exhaleSeconds || 4 }
  } else if (phase.value === 'hold') {
    phase.value = 'exhale'; phaseSeconds.value = ex.exhaleSeconds || 4
  } else if (phase.value === 'exhale') {
    if (ex.restSeconds && ex.restSeconds > 0) { phase.value = 'rest'; phaseSeconds.value = ex.restSeconds }
    else { phase.value = 'inhale'; phaseSeconds.value = ex.inhaleSeconds || 4 }
  } else if (phase.value === 'rest') {
    phase.value = 'inhale'; phaseSeconds.value = ex.inhaleSeconds || 4
  }
}

const togglePlay = () => { playing.value = !playing.value }

const completeEarly = () => {
  ElMessageBox.confirm('确定提前结束练习吗？', '提示', { type: 'info' }).then(() => finishExercise())
}

const finishExercise = async () => {
  if (timerInterval) clearInterval(timerInterval)
  playing.value = false
  completedSeconds.value = elapsedSeconds.value

  if (sessionId.value) {
    await meditationApi.completeSession(sessionId.value, completedSeconds.value)
  }

  view.value = 'complete'
}

const exitPlayer = () => {
  ElMessageBox.confirm('退出将结束当前练习，确定吗？', '提示', { type: 'warning' }).then(async () => {
    if (timerInterval) clearInterval(timerInterval)
    playing.value = false
    if (sessionId.value && elapsedSeconds.value > 10) {
      await meditationApi.completeSession(sessionId.value, elapsedSeconds.value)
    }
    view.value = 'list'
    meditationApi.getStats().then(r => { if (r.code === 200) stats.value = r.data })
  })
}

const backToList = () => {
  view.value = 'list'
  meditationApi.getStats().then(r => { if (r.code === 200) stats.value = r.data })
}

const restartExercise = () => {
  if (currentExercise.value) startExercise(currentExercise.value)
}

const getCategoryLabel = (cat: string) => {
  return { BREATHING: '呼吸练习', BODY_SCAN: '身体扫描', PMR: '渐进放松', GROUNDING: '落地法' }[cat] || cat
}

const formatSeconds = (s: number) => {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}
</script>

<style scoped>
.meditation-page { max-width: 900px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 22px; color: #2D3436; margin: 0 0 8px; }
.subtitle { color: #636E72; font-size: 14px; margin: 0 0 12px; }
.stats-row { display: flex; gap: 12px; }
.stat-chip { padding: 4px 14px; background: #FFF5F5; border-radius: 16px; font-size: 13px; color: #FF6B6B; }

.exercise-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px; }
.exercise-card { background: #fff; border-radius: 16px; padding: 24px; cursor: pointer; transition: all 0.3s; border: 1px solid #F0F0F0; }
.exercise-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); border-color: #FF6B6B; }
.ex-category { font-size: 12px; color: #FF6B6B; font-weight: 500; margin-bottom: 8px; }
.exercise-card h3 { font-size: 16px; color: #2D3436; margin: 0 0 8px; }
.ex-desc { font-size: 13px; color: #636E72; line-height: 1.5; margin: 0 0 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.ex-meta { display: flex; justify-content: space-between; font-size: 12px; color: #B2BEC3; margin-bottom: 16px; }
.ex-start-btn { width: 100%; }

/* Player */
.player-container { max-width: 480px; margin: 0 auto; text-align: center; }
.back-btn { margin-bottom: 16px; }
.player-title { font-size: 20px; color: #2D3436; margin: 0 0 32px; }

.breathing-area { display: flex; justify-content: center; align-items: center; height: 240px; margin-bottom: 32px; }
.breath-circle { width: 160px; height: 160px; border-radius: 50%; background: linear-gradient(135deg, #FF6B6B20, #FFE66D20); border: 3px solid #FF6B6B; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.breath-circle.paused { opacity: 0.5; }
.breath-circle.inhale { border-color: #0984E3; background: linear-gradient(135deg, #0984E320, #74B9FF20); }
.breath-circle.hold { border-color: #FDCB6E; background: linear-gradient(135deg, #FDCB6E20, #FFE66D20); }
.breath-circle.exhale { border-color: #00B894; background: linear-gradient(135deg, #00B89420, #55EFC420); }
.breath-circle.rest { border-color: #B2BEC3; background: #F8F9FA; }
.breath-label { font-size: 16px; font-weight: 600; color: #2D3436; }
.breath-timer { font-size: 28px; font-weight: 700; color: #FF6B6B; margin-top: 4px; }

.time-display { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; font-size: 13px; color: #636E72; }
.time-display .el-progress { flex: 1; }

.instructions { margin-bottom: 32px; text-align: left; }
.instruction-step { padding: 8px 16px; border-radius: 8px; font-size: 14px; color: #B2BEC3; transition: all 0.3s; }
.instruction-step.active { background: #FFF5F5; color: #2D3436; font-weight: 500; }

.player-controls { display: flex; justify-content: center; gap: 16px; }

/* Complete */
.complete-container { max-width: 400px; margin: 60px auto; text-align: center; }
.complete-icon { font-size: 64px; margin-bottom: 16px; }
.complete-container h2 { font-size: 24px; color: #2D3436; margin: 0 0 8px; }
.complete-container p { color: #636E72; margin: 0 0 24px; }
.complete-stats { margin-bottom: 32px; }
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-value { font-size: 32px; font-weight: 700; color: #FF6B6B; }
.stat-label { font-size: 13px; color: #B2BEC3; }
.complete-actions { display: flex; justify-content: center; gap: 12px; }

@media (max-width: 768px) { .exercise-grid { grid-template-columns: 1fr; } }
</style>
