<template>
  <div class="crisis-alerts-page">
    <div class="page-header">
      <h2>危机告警</h2>
      <div class="filter-bar">
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px" @change="loadAlerts">
          <el-option label="待处理" value="OPEN" />
          <el-option label="已确认" value="ACKNOWLEDGED" />
          <el-option label="已升级" value="ESCALATED" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
        <el-select v-model="filters.level" placeholder="级别" clearable style="width: 120px" @change="loadAlerts">
          <el-option label="CRITICAL" value="CRITICAL" />
          <el-option label="HIGH" value="HIGH" />
          <el-option label="MEDIUM" value="MEDIUM" />
        </el-select>
      </div>
    </div>

    <el-table :data="alerts" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="级别" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getLevelType(row.alertLevel)" size="small" effect="dark">
            {{ row.alertLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.alertStatus)" size="small">
            {{ getStatusText(row.alertStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="SLA截止" width="160">
        <template #default="{ row }">
          <span :class="{ 'sla-overdue': isOverdue(row.slaDeadline) }">
            {{ formatDate(row.slaDeadline) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.alertStatus === 'OPEN'" type="warning" size="small" @click="handleAck(row)">
            确认接手
          </el-button>
          <el-button v-if="['OPEN','ACKNOWLEDGED','ESCALATED'].includes(row.alertStatus)"
                     type="success" size="small" @click="handleResolve(row)">
            解决
          </el-button>
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

    <!-- 解决对话框 -->
    <el-dialog v-model="resolveDialogVisible" title="解决告警" width="480px">
      <el-form>
        <el-form-item label="处理备注">
          <el-input v-model="resolveNote" type="textarea" :rows="3" placeholder="请输入处理备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmResolve" :loading="submitting">确认解决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { crisisApi, type CrisisAlertDTO } from '@/api/modules/doctor/crisis'

const alerts = ref<CrisisAlertDTO[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filters = reactive({ status: 'OPEN', level: '' })
const resolveDialogVisible = ref(false)
const resolveNote = ref('')
const currentAlert = ref<CrisisAlertDTO | null>(null)
const submitting = ref(false)

onMounted(() => loadAlerts())

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
  } finally {
    loading.value = false
  }
}

const handleAck = async (alert: CrisisAlertDTO) => {
  try {
    const res = await crisisApi.acknowledge(alert.id)
    if (res.code === 200) {
      ElMessage.success('已确认接手')
      loadAlerts()
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleResolve = (alert: CrisisAlertDTO) => {
  currentAlert.value = alert
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

const confirmResolve = async () => {
  if (!currentAlert.value) return
  submitting.value = true
  try {
    const res = await crisisApi.resolve(currentAlert.value.id, resolveNote.value)
    if (res.code === 200) {
      ElMessage.success('告警已解决')
      resolveDialogVisible.value = false
      loadAlerts()
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const onPageChange = (p: number) => { page.value = p; loadAlerts() }

const getLevelType = (level: string) => {
  return { CRITICAL: 'danger', HIGH: 'warning', MEDIUM: '' }[level] || 'info'
}
const getStatusType = (status: string) => {
  return { OPEN: 'danger', ACKNOWLEDGED: 'warning', ESCALATED: 'danger', RESOLVED: 'success', SUPPRESSED: 'info' }[status] || 'info'
}
const getStatusText = (status: string) => {
  return { OPEN: '待处理', ACKNOWLEDGED: '已确认', ESCALATED: '已升级', RESOLVED: '已解决', SUPPRESSED: '已抑制' }[status] || status
}
const isOverdue = (deadline: string) => deadline && new Date(deadline) < new Date()

const formatDate = (d: string) => {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getMonth() + 1}/${dt.getDate()} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.crisis-alerts-page { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #2D3436; }
.filter-bar { display: flex; gap: 12px; }
.sla-overdue { color: #D63031; font-weight: 600; }
</style>
