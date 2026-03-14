<template>
  <div class="scheduling-page">
    <div class="page-header">
      <h2>排班设置</h2>
      <p class="subtitle">管理您的出诊时段和休假安排</p>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Weekly Schedule -->
      <el-tab-pane label="每周排班" name="weekly">
        <div class="schedule-section">
          <div class="section-header">
            <h3>固定出诊时段</h3>
            <el-button type="primary" size="small" @click="showAddSlot = true">添加时段</el-button>
          </div>
          <div v-loading="loading" class="schedule-grid">
            <div v-for="day in 7" :key="day" class="day-column">
              <div class="day-header">{{ getDayLabel(day) }}</div>
              <div v-for="slot in getSlotsByDay(day)" :key="slot.id" class="slot-card">
                <div class="slot-time">{{ slot.startTime }} - {{ slot.endTime }}</div>
                <div class="slot-meta">{{ slot.slotDuration }}分/位 | 最多{{ slot.maxPatients }}人</div>
                <div class="slot-actions">
                  <el-button size="small" link type="danger" @click="handleDeleteSlot(slot.id)">删除</el-button>
                </div>
              </div>
              <div v-if="getSlotsByDay(day).length === 0" class="no-slot">休息</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Overrides -->
      <el-tab-pane label="特殊日期" name="overrides">
        <div class="schedule-section">
          <div class="section-header">
            <h3>特殊日期设置</h3>
            <el-button type="primary" size="small" @click="showAddOverride = true">添加调整</el-button>
          </div>
          <div class="override-list" v-loading="overridesLoading">
            <div v-if="overrides.length === 0" class="empty-state">
              <el-empty description="暂无特殊日期设置" />
            </div>
            <div v-for="ov in overrides" :key="ov.id" class="override-card">
              <div class="ov-date">{{ ov.overrideDate }}</div>
              <el-tag :type="ov.overrideType === 'UNAVAILABLE' ? 'danger' : 'success'" size="small">
                {{ ov.overrideType === 'UNAVAILABLE' ? '休息' : '加班' }}
              </el-tag>
              <div v-if="ov.startTime" class="ov-time">{{ ov.startTime }} - {{ ov.endTime }}</div>
              <div v-if="ov.reason" class="ov-reason">{{ ov.reason }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- Appointments -->
      <el-tab-pane label="我的预约" name="appointments">
        <div class="schedule-section">
          <div class="filter-bar">
            <el-date-picker v-model="aptDateFilter" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
                            @change="loadDoctorAppointments" style="width: 160px" clearable />
            <el-select v-model="aptStatusFilter" placeholder="状态" clearable @change="loadDoctorAppointments" style="width: 120px; margin-left: 8px">
              <el-option label="待确认" :value="0" />
              <el-option label="已确认" :value="1" />
              <el-option label="已完成" :value="2" />
            </el-select>
          </div>
          <div class="appointment-list" v-loading="aptLoading">
            <div v-if="doctorApts.length === 0" class="empty-state">
              <el-empty description="暂无预约" />
            </div>
            <div v-for="apt in doctorApts" :key="apt.id" class="appointment-card">
              <div class="apt-header">
                <span class="apt-type">{{ getTypeLabel(apt.appointmentType) }}</span>
                <el-tag :type="getStatusType(apt.status)" size="small">
                  {{ getStatusLabel(apt.status) }}
                </el-tag>
              </div>
              <div class="apt-body">
                <div class="apt-time">{{ formatTime(apt.appointmentTime) }}</div>
                <div v-if="apt.symptoms" class="apt-symptoms">主诉：{{ apt.symptoms }}</div>
              </div>
              <div class="apt-actions">
                <el-button v-if="apt.status === 0" size="small" type="primary" @click="handleConfirm(apt.id)">确认</el-button>
                <el-button v-if="apt.status === 1" size="small" type="success" @click="handleComplete(apt.id)">完成</el-button>
                <el-button v-if="apt.status === 0 || apt.status === 1" size="small" type="danger" @click="handleDoctorCancel(apt.id)">取消</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Add Slot Dialog -->
    <el-dialog v-model="showAddSlot" title="添加出诊时段" width="400px" destroy-on-close>
      <el-form :model="slotForm" label-width="100px">
        <el-form-item label="星期">
          <el-select v-model="slotForm.dayOfWeek" style="width: 100%">
            <el-option v-for="d in 7" :key="d" :label="getDayLabel(d)" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="slotForm.startTime" start="07:00" step="00:30" end="21:00" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="slotForm.endTime" start="08:00" step="00:30" end="22:00" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每位时长(分)">
          <el-input-number v-model="slotForm.slotDuration" :min="15" :max="120" :step="5" />
        </el-form-item>
        <el-form-item label="同时段人数">
          <el-input-number v-model="slotForm.maxPatients" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddSlot = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSlot">保存</el-button>
      </template>
    </el-dialog>

    <!-- Add Override Dialog -->
    <el-dialog v-model="showAddOverride" title="特殊日期设置" width="400px" destroy-on-close>
      <el-form :model="overrideForm" label-width="100px">
        <el-form-item label="日期">
          <el-date-picker v-model="overrideForm.overrideDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="overrideForm.overrideType">
            <el-radio value="UNAVAILABLE">休息</el-radio>
            <el-radio value="AVAILABLE">加班</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="overrideForm.overrideType === 'AVAILABLE'">
          <el-form-item label="开始时间">
            <el-time-select v-model="overrideForm.startTime" start="07:00" step="00:30" end="21:00" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-time-select v-model="overrideForm.endTime" start="08:00" step="00:30" end="22:00" style="width: 100%" />
          </el-form-item>
        </template>
        <el-form-item label="原因">
          <el-input v-model="overrideForm.reason" placeholder="如：休假、培训等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddOverride = false">取消</el-button>
        <el-button type="primary" @click="handleSaveOverride">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { schedulingApi } from '@/api/modules/scheduling'
import type { DoctorScheduleItem, AppointmentItem } from '@/api/modules/scheduling'

const activeTab = ref('weekly')
const loading = ref(false)
const overridesLoading = ref(false)
const aptLoading = ref(false)

const scheduleSlots = ref<DoctorScheduleItem[]>([])
const overrides = ref<any[]>([])
const doctorApts = ref<AppointmentItem[]>([])

const showAddSlot = ref(false)
const showAddOverride = ref(false)

const aptDateFilter = ref('')
const aptStatusFilter = ref<number | undefined>(undefined)

const slotForm = ref({
  dayOfWeek: 1,
  startTime: '09:00',
  endTime: '12:00',
  slotDuration: 50,
  maxPatients: 1,
})

const overrideForm = ref({
  overrideDate: '',
  overrideType: 'UNAVAILABLE',
  startTime: '09:00',
  endTime: '12:00',
  reason: '',
})

onMounted(() => {
  loadSchedule()
})

async function loadSchedule() {
  loading.value = true
  try {
    const res = await schedulingApi.getDoctorSchedule()
    if (res.code === 200) {
      scheduleSlots.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

async function loadOverrides() {
  overridesLoading.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const future = new Date(Date.now() + 90 * 86400000).toISOString().slice(0, 10)
    const res = await schedulingApi.getOverrides(today, future)
    if (res.code === 200) {
      overrides.value = res.data || []
    }
  } finally {
    overridesLoading.value = false
  }
}

async function loadDoctorAppointments() {
  aptLoading.value = true
  try {
    const res = await schedulingApi.getDoctorAppointments(
      aptStatusFilter.value, aptDateFilter.value || undefined
    )
    if (res.code === 200) {
      doctorApts.value = res.data || []
    }
  } finally {
    aptLoading.value = false
  }
}

function getSlotsByDay(day: number): DoctorScheduleItem[] {
  return scheduleSlots.value.filter(s => s.dayOfWeek === day)
}

function getDayLabel(day: number): string {
  return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][day] || ''
}

async function handleSaveSlot() {
  try {
    const res = await schedulingApi.saveScheduleSlot({
      dayOfWeek: slotForm.value.dayOfWeek,
      startTime: slotForm.value.startTime + ':00',
      endTime: slotForm.value.endTime + ':00',
      slotDuration: slotForm.value.slotDuration,
      maxPatients: slotForm.value.maxPatients,
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showAddSlot.value = false
      loadSchedule()
    } else {
      ElMessage.error(res.message)
    }
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleDeleteSlot(id: number) {
  try {
    await ElMessageBox.confirm('确认删除此时段？', '提示')
    const res = await schedulingApi.deleteScheduleSlot(id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      loadSchedule()
    }
  } catch { /* cancelled */ }
}

async function handleSaveOverride() {
  try {
    const res = await schedulingApi.saveOverride({
      overrideDate: overrideForm.value.overrideDate,
      overrideType: overrideForm.value.overrideType,
      startTime: overrideForm.value.overrideType === 'AVAILABLE' ? overrideForm.value.startTime : undefined,
      endTime: overrideForm.value.overrideType === 'AVAILABLE' ? overrideForm.value.endTime : undefined,
      reason: overrideForm.value.reason,
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showAddOverride.value = false
      loadOverrides()
    }
  } catch {
    ElMessage.error('保存失败')
  }
}

async function handleConfirm(id: number) {
  const res = await schedulingApi.confirmAppointment(id)
  if (res.code === 200) {
    ElMessage.success('已确认')
    loadDoctorAppointments()
  } else {
    ElMessage.error(res.message)
  }
}

async function handleComplete(id: number) {
  const res = await schedulingApi.completeAppointment(id)
  if (res.code === 200) {
    ElMessage.success('已完成')
    loadDoctorAppointments()
  } else {
    ElMessage.error(res.message)
  }
}

async function handleDoctorCancel(id: number) {
  try {
    const { value } = await ElMessageBox.prompt('取消原因', '取消预约', {
      confirmButtonText: '确认',
      cancelButtonText: '返回',
    })
    const res = await schedulingApi.doctorCancelAppointment(id, value || undefined)
    if (res.code === 200) {
      ElMessage.success('已取消')
      loadDoctorAppointments()
    }
  } catch { /* cancelled */ }
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getTypeLabel(type: string) {
  const map: Record<string, string> = { CONSULTATION: '咨询', THERAPY: '治疗', REVIEW: '复查' }
  return map[type] || type
}

function getStatusLabel(status: number) {
  const map: Record<number, string> = { 0: '待确认', 1: '已确认', 2: '已完成', 3: '已取消', 4: '已过期' }
  return map[status] || '未知'
}

function getStatusType(status: number): '' | 'success' | 'warning' | 'danger' | 'info' {
  return ({ 0: 'warning', 1: '', 2: 'success', 3: 'danger', 4: 'info' } as Record<number, any>)[status] || 'info'
}
</script>

<style scoped>
.scheduling-page { padding: 20px; max-width: 1100px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 22px; color: #2c3e50; }
.subtitle { color: #8492a6; font-size: 14px; margin: 4px 0 0; }

.schedule-section { margin-top: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { margin: 0; font-size: 16px; color: #2c3e50; }

.schedule-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.day-column { min-height: 100px; }
.day-header { text-align: center; font-weight: 600; padding: 8px 0; color: #2c3e50; border-bottom: 2px solid #409eff; margin-bottom: 8px; }
.slot-card { background: #f0f9ff; border-radius: 8px; padding: 8px 10px; margin-bottom: 6px; font-size: 13px; }
.slot-time { font-weight: 500; color: #409eff; }
.slot-meta { color: #8492a6; font-size: 12px; margin-top: 2px; }
.slot-actions { text-align: right; margin-top: 4px; }
.no-slot { text-align: center; color: #c0c4cc; font-size: 13px; padding: 12px 0; }

.override-list { display: flex; flex-direction: column; gap: 10px; }
.override-card { background: #fff; border-radius: 10px; padding: 12px 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; align-items: center; gap: 12px; }
.ov-date { font-weight: 600; color: #2c3e50; }
.ov-time { color: #606266; font-size: 13px; }
.ov-reason { color: #8492a6; font-size: 13px; }

.filter-bar { margin-bottom: 16px; display: flex; align-items: center; }
.appointment-list { display: flex; flex-direction: column; gap: 12px; }
.appointment-card { background: #fff; border-radius: 12px; padding: 16px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.apt-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.apt-type { font-weight: 600; color: #2c3e50; }
.apt-body { color: #606266; font-size: 14px; line-height: 1.8; }
.apt-time { font-weight: 500; color: #409eff; }
.apt-symptoms { color: #8492a6; }
.apt-actions { margin-top: 12px; text-align: right; display: flex; gap: 8px; justify-content: flex-end; }
.empty-state { padding: 40px 0; }

@media (max-width: 768px) {
  .schedule-grid { grid-template-columns: 1fr; }
  .day-header { text-align: left; }
}
</style>
