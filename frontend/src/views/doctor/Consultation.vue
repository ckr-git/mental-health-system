<template>
  <div class="consultation-container">
    <el-card class="header-card">
      <div class="header-content">
        <h2>在线咨询管理</h2>
        <p class="subtitle">管理与患者的在线咨询会话</p>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="会话状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="进行中" value="ongoing" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadConsultations">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="consultations" style="width: 100%; margin-top: 20px" v-loading="loading">
      <el-table-column prop="id" label="会话ID" width="80" />
      <el-table-column label="患者信息" min-width="150">
        <template #default="{ row }">
          <div style="display: flex; align-items: center">
            <el-avatar :size="32" style="margin-right: 10px">
              {{ row.patientNickname?.charAt(0) || 'P' }}
            </el-avatar>
            <div>
              <div>{{ row.patientNickname || row.patientUsername }}</div>
              <div style="font-size: 12px; color: #909399">ID: {{ row.patientId }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="sessionStatus" label="会话状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.sessionStatus === 'ongoing' ? 'success' : 'info'" size="small">
            {{ row.sessionStatus === 'ongoing' ? '进行中' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.startTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="160">
        <template #default="{ row }">
          {{ row.endTime ? formatDateTime(row.endTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="会话时长" width="120">
        <template #default="{ row }">
          {{ calculateDuration(row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="180">
        <template #default="{ row }">
          <el-button
            v-if="row.sessionStatus === 'ongoing'"
            type="danger"
            size="small"
            @click="showCloseDialog(row)"
          >
            结束咨询
          </el-button>
          <el-button type="primary" size="small" @click="viewDetails(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadConsultations"
      style="margin-top: 20px; text-align: center"
    />

    <!-- 结束咨询对话框 -->
    <el-dialog v-model="closeDialogVisible" title="结束咨询" width="500px">
      <div v-if="currentSession">
        <el-alert
          title="确认结束此咨询会话？"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <p>会话ID: {{ currentSession.id }}</p>
            <p>患者: {{ currentSession.patientNickname || currentSession.patientUsername }}</p>
            <p>开始时间: {{ formatDateTime(currentSession.startTime) }}</p>
          </template>
        </el-alert>
        <p style="color: #909399; font-size: 14px">
          结束后，此咨询会话将被标记为已关闭，无法再继续沟通。
        </p>
      </div>

      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="closeConsultation" :loading="submitting">
          确认结束
        </el-button>
      </template>
    </el-dialog>

    <!-- 会话详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="咨询会话详情" width="600px">
      <el-descriptions v-if="currentSession" :column="2" border>
        <el-descriptions-item label="会话ID">{{ currentSession.id }}</el-descriptions-item>
        <el-descriptions-item label="会话状态">
          <el-tag :type="currentSession.sessionStatus === 'ongoing' ? 'success' : 'info'">
            {{ currentSession.sessionStatus === 'ongoing' ? '进行中' : '已关闭' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="患者ID">{{ currentSession.patientId }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">
          {{ currentSession.patientNickname || currentSession.patientUsername }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">
          {{ formatDateTime(currentSession.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">
          {{ currentSession.endTime ? formatDateTime(currentSession.endTime) : '进行中' }}
        </el-descriptions-item>
        <el-descriptions-item label="会话时长" :span="2">
          {{ calculateDuration(currentSession) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { doctorApi } from '@/api'

const filterForm = ref({
  status: ''
})

const consultations = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const closeDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentSession = ref<any>(null)
const submitting = ref(false)

const loadConsultations = async () => {
  loading.value = true
  try {
    const res = await doctorApi.getConsultations({
      pageNum: page.value,
      pageSize: pageSize.value,
      status: filterForm.value.status || undefined
    })
    if (res.code === 200 && res.data) {
      consultations.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载咨询会话列表失败')
    console.error('Failed to load consultations:', error)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.value.status = ''
  page.value = 1
  loadConsultations()
}

const showCloseDialog = (session: any) => {
  currentSession.value = session
  closeDialogVisible.value = true
}

const closeConsultation = async () => {
  if (!currentSession.value) return

  submitting.value = true
  try {
    const res = await doctorApi.closeConsultation(currentSession.value.id)
    if (res.code === 200) {
      ElMessage.success('咨询会话已结束')
      closeDialogVisible.value = false
      loadConsultations()
    } else {
      ElMessage.error(res.message || '结束咨询会话失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '结束咨询会话失败')
    console.error('Failed to close consultation:', error)
  } finally {
    submitting.value = false
  }
}

const viewDetails = (session: any) => {
  currentSession.value = session
  detailDialogVisible.value = true
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '暂无'
  return dateTime.replace('T', ' ').substring(0, 16)
}

const calculateDuration = (session: any) => {
  if (!session.startTime) return '-'

  const start = new Date(session.startTime).getTime()
  const end = session.endTime ? new Date(session.endTime).getTime() : Date.now()
  const diff = end - start

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }
  return `${minutes}分钟`
}

onMounted(() => {
  loadConsultations()
})
</script>

<style scoped lang="scss">
.consultation-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;

  .header-content {
    h2 {
      margin: 0 0 10px 0;
      color: #303133;
    }

    .subtitle {
      margin: 0;
      color: #909399;
      font-size: 14px;
    }
  }
}

.filter-card {
  margin-bottom: 20px;
}
</style>
