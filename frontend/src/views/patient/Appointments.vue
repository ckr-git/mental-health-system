<template>
  <div class="appointments-page">
    <div class="page-header">
      <h2>预约管理</h2>
      <p class="subtitle">选择医生和时间，预约咨询</p>
      <el-button type="primary" @click="showBookDialog = true">新建预约</el-button>
    </div>

    <!-- Tab switching -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="我的预约" name="appointments">
        <div class="filter-bar">
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadAppointments" style="width: 140px">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </div>

        <div class="appointment-list" v-loading="loading">
          <div v-if="appointments.length === 0" class="empty-state">
            <el-empty description="暂无预约记录">
              <el-button type="primary" @click="showBookDialog = true">立即预约</el-button>
            </el-empty>
          </div>
          <div v-for="apt in appointments" :key="apt.id" class="appointment-card"
               :class="'status-' + apt.status">
            <div class="apt-header">
              <span class="apt-type">{{ getTypeLabel(apt.appointmentType) }}</span>
              <el-tag :type="getStatusType(apt.status)" size="small">
                {{ getStatusLabel(apt.status) }}
              </el-tag>
            </div>
            <div class="apt-body">
              <div class="apt-time">{{ formatTime(apt.appointmentTime) }}</div>
              <div class="apt-duration" v-if="apt.duration">{{ apt.duration }}分钟</div>
              <div class="apt-symptoms" v-if="apt.symptoms">主诉：{{ apt.symptoms }}</div>
            </div>
            <div class="apt-actions" v-if="apt.status === 0 || apt.status === 1">
              <el-button size="small" type="danger" @click="handleCancel(apt)">取消预约</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="等候名单" name="waitlist">
        <div class="appointment-list" v-loading="wlLoading">
          <div v-if="waitlist.length === 0" class="empty-state">
            <el-empty description="暂无等候记录" />
          </div>
          <div v-for="wl in waitlist" :key="wl.id" class="appointment-card">
            <div class="apt-header">
              <span class="apt-type">{{ getTypeLabel(wl.appointmentType) }}</span>
              <el-tag type="warning" size="small">等候中</el-tag>
            </div>
            <div class="apt-body">
              <div class="apt-time">期望日期：{{ wl.preferredDate }}</div>
              <div v-if="wl.preferredTimeStart">
                时段：{{ wl.preferredTimeStart }} - {{ wl.preferredTimeEnd }}
              </div>
            </div>
            <div class="apt-actions">
              <el-button size="small" @click="handleCancelWaitlist(wl.id)">取消等候</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Book Dialog -->
    <el-dialog v-model="showBookDialog" title="预约咨询" width="600px" destroy-on-close>
      <el-form :model="bookForm" label-width="100px">
        <el-form-item label="选择医生">
          <el-select v-model="bookForm.doctorId" placeholder="请选择医生" @change="handleDoctorChange" style="width: 100%">
            <el-option v-for="d in doctors" :key="d.id" :label="d.nickname || d.username" :value="d.id">
              <span>{{ d.nickname || d.username }}</span>
              <span style="color: #8492a6; margin-left: 8px">{{ d.specialization }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择日期">
          <el-date-picker v-model="bookForm.date" type="date" placeholder="选择日期"
                          :disabled-date="disabledDate" value-format="YYYY-MM-DD"
                          @change="handleDateChange" style="width: 100%" />
        </el-form-item>
        <el-form-item label="选择时段" v-if="availableSlots.length > 0">
          <div class="slots-grid">
            <div v-for="(slot, idx) in availableSlots" :key="idx"
                 class="slot-chip" :class="{ selected: selectedSlot === idx }"
                 @click="selectedSlot = idx">
              {{ slot.startTime }} - {{ slot.endTime }}
              <span class="slot-avail">(剩{{ slot.available }})</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="选择时段" v-else-if="bookForm.doctorId && bookForm.date && !slotsLoading">
          <el-empty description="该日期没有可用时段" :image-size="60" />
        </el-form-item>
        <el-form-item label="预约类型">
          <el-select v-model="bookForm.appointmentType" style="width: 100%">
            <el-option label="咨询" value="CONSULTATION" />
            <el-option label="治疗" value="THERAPY" />
            <el-option label="复查" value="REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item label="主诉症状">
          <el-input v-model="bookForm.symptoms" type="textarea" :rows="3" placeholder="请描述您的主要症状" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBookDialog = false">取消</el-button>
        <el-button type="primary" :loading="bookLoading" @click="handleBook"
                   :disabled="selectedSlot === null">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { schedulingApi } from '@/api/modules/scheduling'
import type { AppointmentItem, TimeSlot, WaitlistItem } from '@/api/modules/scheduling'
import request from '@/utils/request'
import type { ApiResponse } from '@/types'

const activeTab = ref('appointments')
const loading = ref(false)
const wlLoading = ref(false)
const slotsLoading = ref(false)
const bookLoading = ref(false)
const showBookDialog = ref(false)

const appointments = ref<AppointmentItem[]>([])
const waitlist = ref<WaitlistItem[]>([])
const statusFilter = ref<number | undefined>(undefined)

const doctors = ref<any[]>([])
const availableSlots = ref<TimeSlot[]>([])
const selectedSlot = ref<number | null>(null)

const bookForm = ref({
  doctorId: null as number | null,
  date: '',
  appointmentType: 'CONSULTATION',
  symptoms: '',
})

onMounted(() => {
  loadAppointments()
  loadDoctors()
})

async function loadAppointments() {
  loading.value = true
  try {
    const res = await schedulingApi.getMyAppointments(statusFilter.value)
    if (res.code === 200) {
      appointments.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

async function loadWaitlist() {
  wlLoading.value = true
  try {
    const res = await schedulingApi.getMyWaitlist()
    if (res.code === 200) {
      waitlist.value = res.data || []
    }
  } finally {
    wlLoading.value = false
  }
}

async function loadDoctors() {
  try {
    const res = await request.get<any, ApiResponse<any[]>>('/patient/doctors')
    if (res.code === 200 && res.data) {
      doctors.value = res.data.records || res.data
    }
  } catch { /* ignore */ }
}

function handleTabChange(tab: string) {
  if (tab === 'waitlist') loadWaitlist()
}

async function handleDoctorChange() {
  availableSlots.value = []
  selectedSlot.value = null
  if (bookForm.value.date) {
    await loadSlots()
  }
}

async function handleDateChange() {
  availableSlots.value = []
  selectedSlot.value = null
  if (bookForm.value.doctorId) {
    await loadSlots()
  }
}

async function loadSlots() {
  if (!bookForm.value.doctorId || !bookForm.value.date) return
  slotsLoading.value = true
  try {
    const res = await schedulingApi.getAvailableSlots(
      bookForm.value.doctorId, bookForm.value.date, bookForm.value.date
    )
    if (res.code === 200) {
      availableSlots.value = res.data || []
    }
  } finally {
    slotsLoading.value = false
  }
}

function disabledDate(date: Date) {
  return date.getTime() < Date.now() - 86400000
}

async function handleBook() {
  if (selectedSlot.value === null || !bookForm.value.doctorId || !bookForm.value.date) return
  const slot = availableSlots.value[selectedSlot.value]
  bookLoading.value = true
  try {
    const appointmentTime = `${bookForm.value.date}T${slot.startTime}`
    const res = await schedulingApi.bookAppointment({
      doctorId: bookForm.value.doctorId,
      appointmentTime,
      appointmentType: bookForm.value.appointmentType,
      symptoms: bookForm.value.symptoms,
    })
    if (res.code === 200) {
      ElMessage.success('预约成功')
      showBookDialog.value = false
      loadAppointments()
      // Reset form
      bookForm.value = { doctorId: null, date: '', appointmentType: 'CONSULTATION', symptoms: '' }
      selectedSlot.value = null
      availableSlots.value = []
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch {
    ElMessage.error('预约失败')
  } finally {
    bookLoading.value = false
  }
}

async function handleCancel(apt: AppointmentItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因（选填）', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '取消原因',
    })
    const res = await schedulingApi.cancelAppointment(apt.id, value || undefined)
    if (res.code === 200) {
      ElMessage.success('已取消')
      loadAppointments()
    } else {
      ElMessage.error(res.message)
    }
  } catch { /* cancelled */ }
}

async function handleCancelWaitlist(id: number) {
  const res = await schedulingApi.cancelWaitlist(id)
  if (res.code === 200) {
    ElMessage.success('已取消等候')
    loadWaitlist()
  }
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
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'warning', 1: '', 2: 'success', 3: 'danger', 4: 'info'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.appointments-page { padding: 20px; max-width: 900px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 22px; color: #2c3e50; }
.subtitle { color: #8492a6; font-size: 14px; margin: 4px 0 0; flex-basis: 100%; }
.filter-bar { margin-bottom: 16px; }

.appointment-list { display: flex; flex-direction: column; gap: 12px; }
.appointment-card {
  background: #fff; border-radius: 12px; padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: box-shadow 0.2s;
}
.appointment-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.apt-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.apt-type { font-weight: 600; font-size: 15px; color: #2c3e50; }
.apt-body { color: #606266; font-size: 14px; line-height: 1.8; }
.apt-time { font-weight: 500; color: #409eff; }
.apt-symptoms { color: #8492a6; }
.apt-actions { margin-top: 12px; text-align: right; }

.empty-state { padding: 40px 0; }

.slots-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.slot-chip {
  padding: 8px 16px; border-radius: 8px; border: 1px solid #dcdfe6;
  cursor: pointer; font-size: 14px; transition: all 0.2s;
}
.slot-chip:hover { border-color: #409eff; color: #409eff; }
.slot-chip.selected { background: #409eff; color: #fff; border-color: #409eff; }
.slot-avail { font-size: 12px; opacity: 0.7; margin-left: 4px; }
</style>
