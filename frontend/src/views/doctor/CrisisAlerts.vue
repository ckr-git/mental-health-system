<template>
  <div class="crisis-alerts-page">
    <div class="page-header">
      <h2>危机案例管理</h2>
      <div class="filter-bar">
        <el-radio-group v-model="viewMode" size="small" @change="loadData">
          <el-radio-button value="cases">案例视图</el-radio-button>
          <el-radio-button value="alerts">告警视图</el-radio-button>
        </el-radio-group>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px" @change="loadData">
          <template v-if="viewMode === 'cases'">
            <el-option label="新建" value="NEW" />
            <el-option label="已分诊" value="TRIAGED" />
            <el-option label="已确认" value="ACKED" />
            <el-option label="联系中" value="CONTACTING" />
            <el-option label="干预中" value="INTERVENING" />
            <el-option label="已升级" value="ESCALATED" />
            <el-option label="已稳定" value="STABILIZED" />
            <el-option label="已解决" value="RESOLVED" />
          </template>
          <template v-else>
            <el-option label="待处理" value="OPEN" />
            <el-option label="已确认" value="ACKNOWLEDGED" />
            <el-option label="已升级" value="ESCALATED" />
            <el-option label="已解决" value="RESOLVED" />
          </template>
        </el-select>
        <el-select v-model="filters.level" placeholder="级别" clearable style="width: 120px" @change="loadData">
          <el-option label="CRITICAL" value="CRITICAL" />
          <el-option label="HIGH" value="HIGH" />
          <el-option label="MEDIUM" value="MEDIUM" />
        </el-select>
      </div>
    </div>

    <!-- 案例统计卡片 -->
    <div class="stats-row" v-if="viewMode === 'cases'">
      <div class="stat-card critical">
        <div class="stat-value">{{ stats.critical }}</div>
        <div class="stat-label">CRITICAL</div>
      </div>
      <div class="stat-card high">
        <div class="stat-value">{{ stats.high }}</div>
        <div class="stat-label">HIGH</div>
      </div>
      <div class="stat-card medium">
        <div class="stat-value">{{ stats.medium }}</div>
        <div class="stat-label">MEDIUM</div>
      </div>
      <div class="stat-card overdue">
        <div class="stat-value">{{ stats.overdue }}</div>
        <div class="stat-label">SLA超时</div>
      </div>
    </div>

    <!-- 案例列表 -->
    <el-table v-if="viewMode === 'cases'" :data="cases" v-loading="loading" stripe style="width: 100%"
              @row-click="openCaseDetail" row-class-name="clickable-row">
      <el-table-column label="级别" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.triageLevel)" size="small" effect="dark">
            {{ row.triageLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="案例标题" min-width="180" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getCaseStatusType(row.caseStatus)" size="small">
            {{ getCaseStatusText(row.caseStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="合并事件" width="80" align="center">
        <template #default="{ row }">
          <el-badge :value="row.mergedEventCount" v-if="row.mergedEventCount > 1" />
          <span v-else>1</span>
        </template>
      </el-table-column>
      <el-table-column label="SLA截止" width="160">
        <template #default="{ row }">
          <span :class="{ 'sla-overdue': isOverdue(row.slaDeadline) }">
            {{ formatDate(row.slaDeadline) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="开启时间" width="160">
        <template #default="{ row }">{{ formatDate(row.openedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.caseStatus === 'NEW'" type="primary" size="small" @click.stop="handleTriage(row)">
            分诊
          </el-button>
          <el-button v-if="row.caseStatus === 'TRIAGED'" type="warning" size="small"
                     @click.stop="handleTransition(row, 'ACKED')">
            确认接手
          </el-button>
          <el-button v-if="row.caseStatus === 'ACKED'" size="small"
                     @click.stop="handleTransition(row, 'CONTACTING')">
            开始联系
          </el-button>
          <el-button v-if="row.caseStatus === 'CONTACTING'" size="small"
                     @click.stop="handleTransition(row, 'INTERVENING')">
            开始干预
          </el-button>
          <el-button v-if="row.caseStatus === 'INTERVENING'" type="success" size="small"
                     @click.stop="handleTransition(row, 'STABILIZED')">
            已稳定
          </el-button>
          <el-button v-if="row.caseStatus === 'STABILIZED'" type="success" size="small"
                     @click.stop="handleResolve(row)">
            解决
          </el-button>
          <el-button v-if="!['RESOLVED','POST_REVIEW'].includes(row.caseStatus)"
                     type="danger" size="small" plain @click.stop="handleTransition(row, 'ESCALATED')">
            升级
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 旧版告警列表（保留兼容） -->
    <el-table v-else :data="alerts" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="级别" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.alertLevel)" size="small" effect="dark">{{ row.alertLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.alertStatus)" size="small">{{ getStatusText(row.alertStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SLA截止" width="160">
        <template #default="{ row }">
          <span :class="{ 'sla-overdue': isOverdue(row.slaDeadline) }">{{ formatDate(row.slaDeadline) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.alertStatus === 'OPEN'" type="warning" size="small" @click="handleAck(row)">确认接手</el-button>
          <el-button v-if="['OPEN','ACKNOWLEDGED','ESCALATED'].includes(row.alertStatus)"
                     type="success" size="small" @click="handleOldResolve(row)">解决</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 20"
      :current-page="page"
      :page-size="20"
      :total="total"
      layout="prev, pager, next"
      style="margin-top: 16px; justify-content: center"
      @current-change="onPageChange"
    />

    <!-- 案例详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" :title="currentCase?.title || '案例详情'" size="560px">
      <template v-if="currentCase">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="状态">
            <el-tag :type="getCaseStatusType(currentCase.caseStatus)" size="small">
              {{ getCaseStatusText(currentCase.caseStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="级别">
            <el-tag :type="getLevelType(currentCase.triageLevel)" size="small" effect="dark">
              {{ currentCase.triageLevel }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="合并事件数">{{ currentCase.mergedEventCount }}</el-descriptions-item>
          <el-descriptions-item label="重开次数">{{ currentCase.reopenCount }}</el-descriptions-item>
          <el-descriptions-item label="SLA截止" :span="2">
            <span :class="{ 'sla-overdue': isOverdue(currentCase.slaDeadline) }">
              {{ formatDate(currentCase.slaDeadline) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="来源">{{ currentCase.caseSource }}</el-descriptions-item>
          <el-descriptions-item label="开启时间">{{ formatDate(currentCase.openedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="detailTab" style="margin-top: 16px">
          <!-- 升级记录 -->
          <el-tab-pane label="处置时间线" name="timeline">
            <el-timeline v-if="escalationSteps.length > 0">
              <el-timeline-item v-for="step in escalationSteps" :key="step.id"
                :type="step.stepResult === 'SUCCESS' ? 'success' : 'warning'"
                :timestamp="formatDate(step.executedAt)" placement="top">
                <strong>{{ step.escalationType }}</strong>: {{ step.fromStatus }} → {{ step.toStatus }}
                <div v-if="step.note" style="color: #666; margin-top: 4px">{{ step.note }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无升级记录" :image-size="60" />
          </el-tab-pane>

          <!-- 联系记录 -->
          <el-tab-pane label="联系记录" name="contacts">
            <el-button size="small" type="primary" style="margin-bottom: 12px" @click="showContactDialog = true">
              记录联系尝试
            </el-button>
            <el-table :data="contactAttempts" size="small" v-if="contactAttempts.length > 0">
              <el-table-column prop="contactTarget" label="对象" width="100" />
              <el-table-column prop="contactChannel" label="渠道" width="80" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.attemptStatus === 'REACHED' ? 'success' : 'warning'" size="small">
                    {{ row.attemptStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="outcomeNote" label="备注" min-width="120" show-overflow-tooltip />
              <el-table-column label="时间" width="140">
                <template #default="{ row }">{{ formatDate(row.attemptedAt) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无联系记录" :image-size="60" />
          </el-tab-pane>

          <!-- 安全计划 -->
          <el-tab-pane label="安全计划" name="safety">
            <div v-if="safetyPlan">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="警告信号">{{ safetyPlan.warningSigns }}</el-descriptions-item>
                <el-descriptions-item label="应对策略">{{ safetyPlan.copingStrategies }}</el-descriptions-item>
                <el-descriptions-item label="支持联系人">{{ safetyPlan.supportContacts }}</el-descriptions-item>
                <el-descriptions-item label="危机热线">{{ safetyPlan.crisisHotlines }}</el-descriptions-item>
                <el-descriptions-item label="活着的理由">{{ safetyPlan.reasonsToLive }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <el-empty v-else description="暂无安全计划">
              <el-button type="primary" size="small" @click="showSafetyPlanDialog = true">创建安全计划</el-button>
            </el-empty>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 分诊对话框 -->
    <el-dialog v-model="triageDialogVisible" title="分诊" width="480px">
      <el-form label-width="80px">
        <el-form-item label="风险等级">
          <el-radio-group v-model="triageForm.triageLevel">
            <el-radio-button value="CRITICAL">CRITICAL</el-radio-button>
            <el-radio-button value="HIGH">HIGH</el-radio-button>
            <el-radio-button value="MEDIUM">MEDIUM</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="triageForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTriage" :loading="submitting">确认分诊</el-button>
      </template>
    </el-dialog>

    <!-- 解决对话框 -->
    <el-dialog v-model="resolveDialogVisible" title="解决案例" width="480px">
      <el-form label-width="80px">
        <el-form-item label="解决原因">
          <el-select v-model="resolveForm.resolutionCode" style="width: 100%">
            <el-option label="已稳定" value="STABLE" />
            <el-option label="已转诊" value="REFERRED" />
            <el-option label="已住院" value="HOSPITALIZED" />
            <el-option label="误报" value="FALSE_POSITIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="resolveForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResolve" :loading="submitting">确认解决</el-button>
      </template>
    </el-dialog>

    <!-- 联系记录对话框 -->
    <el-dialog v-model="showContactDialog" title="记录联系尝试" width="480px">
      <el-form label-width="80px">
        <el-form-item label="联系对象">
          <el-select v-model="contactForm.target" style="width: 100%">
            <el-option label="患者" value="PATIENT" />
            <el-option label="紧急联系人" value="EMERGENCY_CONTACT" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系渠道">
          <el-select v-model="contactForm.channel" style="width: 100%">
            <el-option label="电话" value="PHONE" />
            <el-option label="短信" value="SMS" />
            <el-option label="站内消息" value="IN_APP" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系结果">
          <el-select v-model="contactForm.status" style="width: 100%">
            <el-option label="已联系到" value="REACHED" />
            <el-option label="无人接听" value="NO_ANSWER" />
            <el-option label="联系失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contactForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showContactDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmContact" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { crisisApi, type CrisisAlertDTO } from '@/api/modules/doctor/crisis'
import {
  getDoctorCrisisCases, getCrisisCaseDetail, getEscalationSteps,
  getContactAttempts, triageCase, transitionCaseStatus,
  recordContactAttempt, getPatientSafetyPlan
} from '@/api/modules/doctor/crisis-case'

const viewMode = ref<'cases' | 'alerts'>('cases')
const cases = ref<any[]>([])
const alerts = ref<CrisisAlertDTO[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filters = reactive({ status: '', level: '' })
const submitting = ref(false)

// 案例详情
const detailDrawerVisible = ref(false)
const currentCase = ref<any>(null)
const detailTab = ref('timeline')
const escalationSteps = ref<any[]>([])
const contactAttempts = ref<any[]>([])
const safetyPlan = ref<any>(null)

// 分诊
const triageDialogVisible = ref(false)
const triageForm = reactive({ triageLevel: 'HIGH', note: '' })

// 解决
const resolveDialogVisible = ref(false)
const resolveForm = reactive({ resolutionCode: 'STABLE', note: '' })

// 联系
const showContactDialog = ref(false)
const contactForm = reactive({ target: 'PATIENT', channel: 'PHONE', status: 'REACHED', note: '' })
const showSafetyPlanDialog = ref(false)

// 统计
const stats = computed(() => {
  const data = cases.value
  return {
    critical: data.filter(c => c.triageLevel === 'CRITICAL' && !['RESOLVED','POST_REVIEW'].includes(c.caseStatus)).length,
    high: data.filter(c => c.triageLevel === 'HIGH' && !['RESOLVED','POST_REVIEW'].includes(c.caseStatus)).length,
    medium: data.filter(c => c.triageLevel === 'MEDIUM' && !['RESOLVED','POST_REVIEW'].includes(c.caseStatus)).length,
    overdue: data.filter(c => isOverdue(c.slaDeadline) && !['RESOLVED','POST_REVIEW'].includes(c.caseStatus)).length,
  }
})

onMounted(() => loadData())

const loadData = async () => {
  if (viewMode.value === 'cases') await loadCases()
  else await loadAlerts()
}

const loadCases = async () => {
  loading.value = true
  try {
    const res = await getDoctorCrisisCases({
      status: filters.status || undefined,
      pageNum: page.value, pageSize: 20
    })
    if (res.data?.code === 200 && res.data?.data) {
      cases.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } catch { /* ignore */ } finally { loading.value = false }
}

const loadAlerts = async () => {
  loading.value = true
  try {
    const res = await crisisApi.getAlerts({
      pageNum: page.value, pageSize: 20,
      status: filters.status || undefined,
      level: filters.level || undefined
    })
    if (res.code === 200 && res.data) {
      alerts.value = res.data.records
      total.value = res.data.total
    }
  } finally { loading.value = false }
}

const openCaseDetail = async (row: any) => {
  currentCase.value = row
  detailTab.value = 'timeline'
  detailDrawerVisible.value = true
  // 加载详情数据
  try {
    const [stepsRes, contactsRes] = await Promise.all([
      getEscalationSteps(row.id),
      getContactAttempts(row.id)
    ])
    escalationSteps.value = stepsRes.data?.data || []
    contactAttempts.value = contactsRes.data?.data || []

    if (row.patientId) {
      const planRes = await getPatientSafetyPlan(row.patientId)
      safetyPlan.value = planRes.data?.data || null
    }
  } catch { /* ignore */ }
}

const handleTriage = (row: any) => {
  currentCase.value = row
  triageForm.triageLevel = row.triageLevel || 'HIGH'
  triageForm.note = ''
  triageDialogVisible.value = true
}

const confirmTriage = async () => {
  if (!currentCase.value) return
  submitting.value = true
  try {
    await triageCase(currentCase.value.id, triageForm)
    ElMessage.success('分诊完成')
    triageDialogVisible.value = false
    loadCases()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const handleTransition = async (row: any, targetStatus: string) => {
  if (targetStatus === 'ESCALATED') {
    currentCase.value = row
    // 直接升级
    try {
      await transitionCaseStatus(row.id, { targetStatus, note: '手动升级' })
      ElMessage.warning('案例已升级')
      loadCases()
    } catch { ElMessage.error('操作失败') }
    return
  }
  try {
    await transitionCaseStatus(row.id, { targetStatus })
    ElMessage.success('状态已更新')
    loadCases()
  } catch { ElMessage.error('操作失败') }
}

const handleResolve = (row: any) => {
  currentCase.value = row
  resolveForm.resolutionCode = 'STABLE'
  resolveForm.note = ''
  resolveDialogVisible.value = true
}

const confirmResolve = async () => {
  if (!currentCase.value) return
  submitting.value = true
  try {
    await transitionCaseStatus(currentCase.value.id, {
      targetStatus: 'RESOLVED',
      note: `[${resolveForm.resolutionCode}] ${resolveForm.note}`
    })
    ElMessage.success('案例已解决')
    resolveDialogVisible.value = false
    loadCases()
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

const confirmContact = async () => {
  if (!currentCase.value) return
  submitting.value = true
  try {
    await recordContactAttempt(currentCase.value.id, contactForm)
    ElMessage.success('联系记录已保存')
    showContactDialog.value = false
    // 刷新详情
    const res = await getContactAttempts(currentCase.value.id)
    contactAttempts.value = res.data?.data || []
  } catch { ElMessage.error('操作失败') } finally { submitting.value = false }
}

// 旧版兼容
const handleAck = async (alert: CrisisAlertDTO) => {
  try {
    const res = await crisisApi.acknowledge(alert.id)
    if (res.code === 200) { ElMessage.success('已确认接手'); loadAlerts() }
  } catch { ElMessage.error('操作失败') }
}
const handleOldResolve = async (alert: CrisisAlertDTO) => {
  try {
    const res = await crisisApi.resolve(alert.id, '')
    if (res.code === 200) { ElMessage.success('告警已解决'); loadAlerts() }
  } catch { ElMessage.error('操作失败') }
}

const onPageChange = (p: number) => { page.value = p; loadData() }

const getLevelType = (level: string) =>
  ({ CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '' } as Record<string, string>)[level] || 'info'

const getCaseStatusType = (status: string) => ({
  NEW: 'danger', TRIAGED: 'warning', ACKED: '', CONTACTING: '',
  INTERVENING: 'warning', ESCALATED: 'danger', STABILIZED: 'success',
  RESOLVED: 'success', POST_REVIEW: 'info'
} as Record<string, string>)[status] || 'info'

const getCaseStatusText = (status: string) => ({
  NEW: '新建', TRIAGED: '已分诊', ACKED: '已确认', CONTACTING: '联系中',
  INTERVENING: '干预中', ESCALATED: '已升级', STABILIZED: '已稳定',
  RESOLVED: '已解决', POST_REVIEW: '复盘中'
} as Record<string, string>)[status] || status

const getStatusType = (status: string) =>
  ({ OPEN: 'danger', ACKNOWLEDGED: 'warning', ESCALATED: 'danger', RESOLVED: 'success', SUPPRESSED: 'info' } as Record<string, string>)[status] || 'info'
const getStatusText = (status: string) =>
  ({ OPEN: '待处理', ACKNOWLEDGED: '已确认', ESCALATED: '已升级', RESOLVED: '已解决', SUPPRESSED: '已抑制' } as Record<string, string>)[status] || status

const isOverdue = (deadline: string) => deadline && new Date(deadline) < new Date()

const formatDate = (d: string) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.crisis-alerts-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h2 { margin: 0; font-size: 20px; color: #2D3436; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.sla-overdue { color: #D63031; font-weight: 600; }

.stats-row { display: flex; gap: 16px; margin-bottom: 20px; }
.stat-card { flex: 1; padding: 16px; border-radius: 8px; text-align: center; color: #fff; }
.stat-card.critical { background: linear-gradient(135deg, #D63031, #e17055); }
.stat-card.high { background: linear-gradient(135deg, #e17055, #fdcb6e); }
.stat-card.medium { background: linear-gradient(135deg, #0984e3, #74b9ff); }
.stat-card.overdue { background: linear-gradient(135deg, #636e72, #b2bec3); }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; margin-top: 4px; opacity: 0.9; }

:deep(.clickable-row) { cursor: pointer; }
:deep(.clickable-row:hover td) { background: #f0f9ff !important; }
</style>
