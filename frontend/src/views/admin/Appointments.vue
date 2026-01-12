<template>
  <div class="appointments-container">
    <!-- Statistics Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF"><Calendar /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalAppointments || 0 }}</div>
              <div class="stat-label">总预约数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C"><Clock /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.pendingCount || 0 }}</div>
              <div class="stat-label">待确认</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A"><Select /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.confirmedCount || 0 }}</div>
              <div class="stat-label">已确认</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#909399"><CircleCheck /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completedCount || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C"><CircleClose /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.cancelledCount || 0 }}</div>
              <div class="stat-label">已取消</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#C0C4CC"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.expiredCount || 0 }}</div>
              <div class="stat-label">已过期</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Main Content -->
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <h2>📅 预约管理</h2>
          <el-button type="primary" @click="showAppointmentDialog()">新建预约</el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="医生">
          <el-input v-model="filters.doctorId" placeholder="医生ID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="患者">
          <el-input v-model="filters.patientId" placeholder="患者ID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
            <el-option label="已过期" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.appointmentType" placeholder="全部类型" clearable style="width: 130px">
            <el-option label="咨询" value="CONSULTATION" />
            <el-option label="治疗" value="THERAPY" />
            <el-option label="复诊" value="REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAppointments">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- Table -->
      <el-table :data="appointmentList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="patientName" label="患者" min-width="120" />
        <el-table-column prop="doctorName" label="医生" min-width="120" />
        <el-table-column prop="appointmentType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getAppointmentTypeTag(row.appointmentType)">
              {{ getAppointmentTypeName(row.appointmentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appointmentTime" label="预约时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.appointmentTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="80">
          <template #default="{ row }">{{ row.duration }}分钟</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="symptoms" label="症状描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="confirmAppointment(row.id)">
              确认
            </el-button>
            <el-button v-if="row.status === 1" type="primary" size="small" @click="completeAppointment(row.id)">
              完成
            </el-button>
            <el-button v-if="[0, 1].includes(row.status)" type="warning" size="small" @click="showCancelDialog(row)">
              取消
            </el-button>
            <el-button type="info" size="small" @click="showAppointmentDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteAppointment(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadAppointments"
        @current-change="loadAppointments"
        class="pagination"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="appointmentDialogVisible"
      :title="currentAppointment.id ? '编辑预约' : '新建预约'"
      width="600px"
      @close="resetAppointmentForm"
    >
      <el-form :model="currentAppointment" label-width="100px">
        <el-form-item label="患者ID" required>
          <el-input v-model="currentAppointment.patientId" placeholder="请输入患者ID" />
        </el-form-item>
        <el-form-item label="医生ID" required>
          <el-input v-model="currentAppointment.doctorId" placeholder="请输入医生ID" />
        </el-form-item>
        <el-form-item label="预约类型" required>
          <el-select v-model="currentAppointment.appointmentType" placeholder="请选择类型">
            <el-option label="咨询" value="CONSULTATION" />
            <el-option label="治疗" value="THERAPY" />
            <el-option label="复诊" value="REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间" required>
          <el-date-picker
            v-model="currentAppointment.appointmentTime"
            type="datetime"
            placeholder="请选择预约时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时长(分钟)" required>
          <el-input-number v-model="currentAppointment.duration" :min="15" :max="240" :step="15" />
        </el-form-item>
        <el-form-item label="症状描述">
          <el-input
            v-model="currentAppointment.symptoms"
            type="textarea"
            :rows="3"
            placeholder="请输入症状描述"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="currentAppointment.notes"
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAppointment" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Cancel Dialog -->
    <el-dialog v-model="cancelDialogVisible" title="取消预约" width="500px">
      <el-form label-width="100px">
        <el-form-item label="取消原因" required>
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="4"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitCancel" :loading="saving">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Clock, Select, CircleCheck, CircleClose, Warning } from '@element-plus/icons-vue'
import { adminApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const appointmentDialogVisible = ref(false)
const cancelDialogVisible = ref(false)

const statistics = reactive({
  totalAppointments: 0,
  pendingCount: 0,
  confirmedCount: 0,
  completedCount: 0,
  cancelledCount: 0,
  expiredCount: 0
})

const filters = reactive({
  doctorId: '',
  patientId: '',
  status: undefined as number | undefined,
  appointmentType: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const appointmentList = ref<any[]>([])

const currentAppointment = reactive({
  id: null as number | null,
  patientId: '',
  doctorId: '',
  appointmentType: '',
  appointmentTime: '',
  duration: 60,
  symptoms: '',
  notes: ''
})

const cancelReason = ref('')
const currentCancelId = ref<number | null>(null)

// Load statistics
const loadStatistics = async () => {
  try {
    const res = await adminApi.getAppointmentStatistics()
    if (res.code === 200 && res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

// Load appointments
const loadAppointments = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      doctorId: filters.doctorId ? Number(filters.doctorId) : undefined,
      patientId: filters.patientId ? Number(filters.patientId) : undefined,
      status: filters.status,
      appointmentType: filters.appointmentType || undefined
    }
    const res = await adminApi.getAppointments(params)
    if (res.code === 200 && res.data) {
      appointmentList.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载预约列表失败')
    console.error('Failed to load appointments:', error)
  } finally {
    loading.value = false
  }
}

// Reset filters
const resetFilters = () => {
  filters.doctorId = ''
  filters.patientId = ''
  filters.status = undefined
  filters.appointmentType = ''
  pagination.pageNum = 1
  loadAppointments()
}

// Show appointment dialog
const showAppointmentDialog = (appointment?: any) => {
  if (appointment) {
    currentAppointment.id = appointment.id
    currentAppointment.patientId = appointment.patientId
    currentAppointment.doctorId = appointment.doctorId
    currentAppointment.appointmentType = appointment.appointmentType
    currentAppointment.appointmentTime = appointment.appointmentTime
    currentAppointment.duration = appointment.duration
    currentAppointment.symptoms = appointment.symptoms || ''
    currentAppointment.notes = appointment.notes || ''
  }
  appointmentDialogVisible.value = true
}

// Reset appointment form
const resetAppointmentForm = () => {
  currentAppointment.id = null
  currentAppointment.patientId = ''
  currentAppointment.doctorId = ''
  currentAppointment.appointmentType = ''
  currentAppointment.appointmentTime = ''
  currentAppointment.duration = 60
  currentAppointment.symptoms = ''
  currentAppointment.notes = ''
}

// Save appointment
const saveAppointment = async () => {
  if (!currentAppointment.patientId || !currentAppointment.doctorId ||
      !currentAppointment.appointmentType || !currentAppointment.appointmentTime) {
    ElMessage.warning('请填写必填项')
    return
  }

  saving.value = true
  try {
    const data = {
      patientId: Number(currentAppointment.patientId),
      doctorId: Number(currentAppointment.doctorId),
      appointmentType: currentAppointment.appointmentType,
      appointmentTime: currentAppointment.appointmentTime,
      duration: currentAppointment.duration,
      symptoms: currentAppointment.symptoms,
      notes: currentAppointment.notes
    }

    const res = currentAppointment.id
      ? await adminApi.updateAppointment(currentAppointment.id, data)
      : await adminApi.createAppointment(data)

    if (res.code === 200) {
      ElMessage.success(currentAppointment.id ? '更新成功' : '创建成功')
      appointmentDialogVisible.value = false
      loadAppointments()
      loadStatistics()
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('Failed to save appointment:', error)
  } finally {
    saving.value = false
  }
}

// Confirm appointment
const confirmAppointment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要确认此预约吗？', '提示', { type: 'warning' })
    const res = await adminApi.confirmAppointment(id)
    if (res.code === 200) {
      ElMessage.success('确认成功')
      loadAppointments()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认失败')
    }
  }
}

// Show cancel dialog
const showCancelDialog = (appointment: any) => {
  currentCancelId.value = appointment.id
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

// Submit cancel
const submitCancel = async () => {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }

  saving.value = true
  try {
    const res = await adminApi.cancelAppointment(currentCancelId.value!, cancelReason.value)
    if (res.code === 200) {
      ElMessage.success('取消成功')
      cancelDialogVisible.value = false
      loadAppointments()
      loadStatistics()
    }
  } catch (error) {
    ElMessage.error('取消失败')
    console.error('Failed to cancel appointment:', error)
  } finally {
    saving.value = false
  }
}

// Complete appointment
const completeAppointment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要将此预约标记为已完成吗？', '提示', { type: 'warning' })
    const res = await adminApi.completeAppointment(id)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadAppointments()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// Delete appointment
const deleteAppointment = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此预约吗？此操作不可恢复。', '警告', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    const res = await adminApi.deleteAppointment(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAppointments()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// Helper functions
const getStatusName = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '待确认',
    1: '已确认',
    2: '已完成',
    3: '已取消',
    4: '已过期'
  }
  return statusMap[status] || '未知'
}

const getStatusTag = (status: number) => {
  const tagMap: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'info',
    3: 'danger',
    4: 'info'
  }
  return tagMap[status] || ''
}

const getAppointmentTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    'CONSULTATION': '咨询',
    'THERAPY': '治疗',
    'REVIEW': '复诊'
  }
  return typeMap[type] || type
}

const getAppointmentTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    'CONSULTATION': 'primary',
    'THERAPY': 'success',
    'REVIEW': 'warning'
  }
  return tagMap[type] || ''
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ')
}

onMounted(() => {
  loadStatistics()
  loadAppointments()
})
</script>

<style scoped>
.appointments-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 36px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.main-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.filter-form {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
