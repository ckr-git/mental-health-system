<template>
  <div class="treatment-page">
    <template v-if="!currentPlan">
      <div class="page-header">
        <h2>治疗计划</h2>
        <el-button type="primary" @click="showCreateDialog = true">新建计划</el-button>
      </div>
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="状态" clearable @change="loadPlans" style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="进行中" value="ACTIVE" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
      </div>
      <div v-loading="loading" class="plan-list">
        <div v-if="plans.length === 0" class="empty-state">
          <el-empty description="暂无治疗计划" />
        </div>
        <div v-for="p in plans" :key="p.id" class="plan-card" @click="openPlan(p.id)">
          <div class="plan-header">
            <span class="plan-title">{{ p.title }}</span>
            <el-tag :type="getPlanStatusType(p.planStatus)" size="small">{{ getPlanStatusLabel(p.planStatus) }}</el-tag>
          </div>
          <div class="plan-meta">
            <span v-if="p.diagnosis">诊断：{{ p.diagnosis }}</span>
            <span v-if="p.startDate">开始：{{ p.startDate }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- Plan Detail View -->
    <template v-else>
      <div class="page-header">
        <el-button @click="currentPlan = null" link>&lt; 返回列表</el-button>
        <h2>{{ detail?.plan?.title }}</h2>
        <div class="plan-actions">
          <el-button v-if="detail?.plan?.planStatus === 'DRAFT'" type="primary" size="small" @click="handleActivate">激活计划</el-button>
          <el-button v-if="detail?.plan?.planStatus === 'ACTIVE'" type="success" size="small" @click="handleComplete">完成计划</el-button>
        </div>
      </div>

      <div class="detail-stats">
        <div class="stat-card"><div class="stat-num">{{ detail?.totalGoals || 0 }}</div><div class="stat-label">目标总数</div></div>
        <div class="stat-card"><div class="stat-num">{{ detail?.achievedGoals || 0 }}</div><div class="stat-label">已达成</div></div>
        <div class="stat-card"><div class="stat-num">{{ detail?.avgProgress || 0 }}%</div><div class="stat-label">平均进度</div></div>
        <div class="stat-card"><div class="stat-num">{{ detail?.sessionCount || 0 }}</div><div class="stat-label">咨询次数</div></div>
      </div>

      <el-tabs v-model="detailTab">
        <el-tab-pane label="治疗目标" name="goals">
          <div class="section-header">
            <el-button size="small" type="primary" @click="showGoalDialog = true">添加目标</el-button>
          </div>
          <div v-for="g in detail?.goals" :key="g.id" class="goal-card">
            <div class="goal-header">
              <el-tag size="small" :type="g.goalType === 'SHORT_TERM' ? 'warning' : 'info'">
                {{ g.goalType === 'SHORT_TERM' ? '短期' : '长期' }}
              </el-tag>
              <span class="goal-title">{{ g.title }}</span>
              <el-tag size="small" :type="getGoalStatusType(g.status)">{{ getGoalStatusLabel(g.status) }}</el-tag>
            </div>
            <div v-if="g.description" class="goal-desc">{{ g.description }}</div>
            <el-progress :percentage="g.progressPct" :color="g.progressPct >= 100 ? '#67c23a' : '#409eff'" style="margin-top: 8px" />
            <div class="goal-actions">
              <el-button size="small" link @click="openProgressDialog(g)">更新进度</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="干预措施" name="interventions">
          <div class="section-header">
            <el-button size="small" type="primary" @click="showInterventionDialog = true">添加干预</el-button>
          </div>
          <div v-for="i in detail?.interventions" :key="i.id" class="intervention-card">
            <el-tag size="small">{{ getInterventionTypeLabel(i.interventionType) }}</el-tag>
            <span class="int-title">{{ i.title }}</span>
            <span v-if="i.frequency" class="int-freq">{{ i.frequency }}</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="咨询记录" name="notes">
          <div class="section-header">
            <el-button size="small" type="primary" @click="showNoteDialog = true">添加记录</el-button>
          </div>
          <div v-for="n in detail?.sessionNotes" :key="n.id" class="note-card">
            <div class="note-header">
              <span class="note-date">{{ n.sessionDate }}</span>
              <el-tag size="small">{{ n.sessionType }}</el-tag>
            </div>
            <div v-if="n.subjective" class="note-section"><strong>S - 主诉：</strong>{{ n.subjective }}</div>
            <div v-if="n.objective" class="note-section"><strong>O - 观察：</strong>{{ n.objective }}</div>
            <div v-if="n.assessment" class="note-section"><strong>A - 评估：</strong>{{ n.assessment }}</div>
            <div v-if="n.planNotes" class="note-section"><strong>P - 计划：</strong>{{ n.planNotes }}</div>
            <div v-if="n.homework" class="note-section"><strong>作业：</strong>{{ n.homework }}</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- Create Plan Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建治疗计划" width="500px" destroy-on-close>
      <el-form :model="planForm" label-width="100px">
        <el-form-item label="患者ID"><el-input-number v-model="planForm.patientId" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="planForm.title" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="planForm.diagnosis" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="目标概述"><el-input v-model="planForm.summary" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="目标完成日"><el-date-picker v-model="planForm.targetEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreatePlan">创建</el-button>
      </template>
    </el-dialog>

    <!-- Add Goal Dialog -->
    <el-dialog v-model="showGoalDialog" title="添加治疗目标" width="450px" destroy-on-close>
      <el-form :model="goalForm" label-width="100px">
        <el-form-item label="类型">
          <el-radio-group v-model="goalForm.goalType">
            <el-radio value="SHORT_TERM">短期</el-radio>
            <el-radio value="LONG_TERM">长期</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标"><el-input v-model="goalForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="goalForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="衡量标准"><el-input v-model="goalForm.measurableTarget" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGoalDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddGoal">添加</el-button>
      </template>
    </el-dialog>

    <!-- Add Intervention Dialog -->
    <el-dialog v-model="showInterventionDialog" title="添加干预措施" width="450px" destroy-on-close>
      <el-form :model="intForm" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="intForm.interventionType" style="width: 100%">
            <el-option label="认知行为" value="CBT" />
            <el-option label="药物治疗" value="MEDICATION" />
            <el-option label="行为练习" value="EXERCISE" />
            <el-option label="家庭作业" value="HOMEWORK" />
            <el-option label="正念冥想" value="MEDITATION" />
            <el-option label="日志记录" value="JOURNALING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="intForm.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="intForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="频率"><el-input v-model="intForm.frequency" placeholder="如：每天、每周3次" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInterventionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddIntervention">添加</el-button>
      </template>
    </el-dialog>

    <!-- Add Session Note Dialog -->
    <el-dialog v-model="showNoteDialog" title="添加咨询记录 (SOAP)" width="600px" destroy-on-close>
      <el-form :model="noteForm" label-width="100px">
        <el-form-item label="患者ID"><el-input-number v-model="noteForm.patientId" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="noteForm.sessionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="noteForm.sessionType" style="width: 100%">
            <el-option label="个体咨询" value="INDIVIDUAL" />
            <el-option label="团体咨询" value="GROUP" />
            <el-option label="危机干预" value="CRISIS" />
            <el-option label="电话" value="PHONE" />
            <el-option label="线上" value="ONLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="S - 主诉"><el-input v-model="noteForm.subjective" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="O - 观察"><el-input v-model="noteForm.objective" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="A - 评估"><el-input v-model="noteForm.assessment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="P - 计划"><el-input v-model="noteForm.planNotes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="家庭作业"><el-input v-model="noteForm.homework" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddNote">保存</el-button>
      </template>
    </el-dialog>

    <!-- Progress Dialog -->
    <el-dialog v-model="showProgressDialog" title="更新目标进度" width="400px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="进度">
          <el-slider v-model="progressForm.progressPct" :max="100" show-input />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="progressForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProgressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProgress">更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { treatmentApi } from '@/api/modules/treatment'
import type { TreatmentPlanItem, PlanDetailDTO } from '@/api/modules/treatment'

const loading = ref(false)
const plans = ref<TreatmentPlanItem[]>([])
const statusFilter = ref('')
const currentPlan = ref<number | null>(null)
const detail = ref<PlanDetailDTO | null>(null)
const detailTab = ref('goals')

const showCreateDialog = ref(false)
const showGoalDialog = ref(false)
const showInterventionDialog = ref(false)
const showNoteDialog = ref(false)
const showProgressDialog = ref(false)

const planForm = ref({ patientId: 0, title: '', diagnosis: '', summary: '', targetEndDate: '' })
const goalForm = ref({ goalType: 'SHORT_TERM', title: '', description: '', measurableTarget: '' })
const intForm = ref({ interventionType: 'CBT', title: '', description: '', frequency: '' })
const noteForm = ref({ patientId: 0, sessionDate: new Date().toISOString().slice(0, 10), sessionType: 'INDIVIDUAL', subjective: '', objective: '', assessment: '', planNotes: '', homework: '' })
const progressForm = ref({ goalId: 0, progressPct: 0, note: '' })

onMounted(() => loadPlans())

async function loadPlans() {
  loading.value = true
  try {
    const res = await treatmentApi.getDoctorPlans(statusFilter.value || undefined)
    if (res.code === 200) plans.value = res.data || []
  } finally { loading.value = false }
}

async function openPlan(id: number) {
  currentPlan.value = id
  const res = await treatmentApi.getPlanDetail(id)
  if (res.code === 200) detail.value = res.data
}

async function handleCreatePlan() {
  const res = await treatmentApi.createPlan(planForm.value)
  if (res.code === 200) { ElMessage.success('创建成功'); showCreateDialog.value = false; loadPlans() }
}

async function handleActivate() {
  if (!currentPlan.value) return
  const res = await treatmentApi.activatePlan(currentPlan.value)
  if (res.code === 200) { ElMessage.success('已激活'); openPlan(currentPlan.value) }
}

async function handleComplete() {
  if (!currentPlan.value) return
  const res = await treatmentApi.completePlan(currentPlan.value)
  if (res.code === 200) { ElMessage.success('已完成'); openPlan(currentPlan.value) }
}

async function handleAddGoal() {
  if (!currentPlan.value) return
  const res = await treatmentApi.addGoal(currentPlan.value, goalForm.value)
  if (res.code === 200) { ElMessage.success('已添加'); showGoalDialog.value = false; openPlan(currentPlan.value) }
}

async function handleAddIntervention() {
  if (!currentPlan.value) return
  const res = await treatmentApi.addIntervention(currentPlan.value, intForm.value)
  if (res.code === 200) { ElMessage.success('已添加'); showInterventionDialog.value = false; openPlan(currentPlan.value) }
}

async function handleAddNote() {
  if (!currentPlan.value) return
  const data = { ...noteForm.value, planId: currentPlan.value }
  const res = await treatmentApi.createSessionNote(data)
  if (res.code === 200) { ElMessage.success('已保存'); showNoteDialog.value = false; openPlan(currentPlan.value) }
}

function openProgressDialog(goal: any) {
  progressForm.value = { goalId: goal.id, progressPct: goal.progressPct, note: '' }
  showProgressDialog.value = true
}

async function handleUpdateProgress() {
  const res = await treatmentApi.updateGoalProgress(progressForm.value.goalId, progressForm.value.progressPct, progressForm.value.note)
  if (res.code === 200) { ElMessage.success('已更新'); showProgressDialog.value = false; if (currentPlan.value) openPlan(currentPlan.value) }
}

function getPlanStatusType(s: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ DRAFT: 'info', ACTIVE: '', PAUSED: 'warning', COMPLETED: 'success', ARCHIVED: 'info' } as any)[s] || 'info'
}
function getPlanStatusLabel(s: string) { return ({ DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '暂停', COMPLETED: '已完成', ARCHIVED: '归档' } as any)[s] || s }
function getGoalStatusType(s: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  return ({ PENDING: 'info', IN_PROGRESS: 'warning', ACHIEVED: 'success', DEFERRED: 'info', DROPPED: 'danger' } as any)[s] || 'info'
}
function getGoalStatusLabel(s: string) { return ({ PENDING: '待开始', IN_PROGRESS: '进行中', ACHIEVED: '已达成', DEFERRED: '推迟', DROPPED: '放弃' } as any)[s] || s }
function getInterventionTypeLabel(t: string) { return ({ CBT: '认知行为', MEDICATION: '药物', EXERCISE: '行为练习', HOMEWORK: '作业', MEDITATION: '正念', JOURNALING: '日志', OTHER: '其他' } as any)[t] || t }
</script>

<style scoped>
.treatment-page { padding: 20px; max-width: 1000px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; color: #2c3e50; }
.filter-bar { margin-bottom: 16px; }

.plan-list { display: flex; flex-direction: column; gap: 12px; }
.plan-card { background: #fff; border-radius: 12px; padding: 16px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); cursor: pointer; transition: box-shadow 0.2s; }
.plan-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.plan-header { display: flex; justify-content: space-between; align-items: center; }
.plan-title { font-weight: 600; font-size: 16px; color: #2c3e50; }
.plan-meta { color: #8492a6; font-size: 13px; margin-top: 6px; display: flex; gap: 16px; }

.detail-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 10px; padding: 16px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.stat-num { font-size: 24px; font-weight: 700; color: #409eff; }
.stat-label { font-size: 13px; color: #8492a6; margin-top: 4px; }

.section-header { margin-bottom: 12px; }
.goal-card { background: #fff; border-radius: 10px; padding: 14px 18px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.goal-header { display: flex; align-items: center; gap: 8px; }
.goal-title { font-weight: 500; flex: 1; }
.goal-desc { color: #8492a6; font-size: 13px; margin-top: 4px; }
.goal-actions { text-align: right; margin-top: 6px; }

.intervention-card { background: #fff; border-radius: 8px; padding: 10px 14px; margin-bottom: 8px; display: flex; align-items: center; gap: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.int-title { font-weight: 500; flex: 1; }
.int-freq { color: #8492a6; font-size: 13px; }

.note-card { background: #fff; border-radius: 10px; padding: 14px 18px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.note-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.note-date { font-weight: 600; color: #409eff; }
.note-section { font-size: 14px; color: #606266; margin-top: 4px; line-height: 1.6; }

.plan-actions { display: flex; gap: 8px; }
.empty-state { padding: 40px 0; }
</style>
