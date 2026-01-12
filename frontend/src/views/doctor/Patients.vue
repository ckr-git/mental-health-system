<template>
  <div class="patients-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="患者姓名">
          <el-input v-model="searchForm.keyword" placeholder="搜索患者" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="searchForm.riskLevel" placeholder="全部" clearable style="width: 150px">
            <el-option label="高风险" value="HIGH" />
            <el-option label="中风险" value="MEDIUM" />
            <el-option label="低风险" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPatients">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="patients" style="width: 100%; margin-top: 20px" v-loading="loading">
      <el-table-column prop="name" label="患者姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 'MALE' ? '男' : '女' }}
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="riskLevel" label="风险等级" width="100">
        <template #default="{ row }">
          <el-tag :type="getRiskType(row.riskLevel)" size="small">
            {{ getRiskText(row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastConsultTime" label="最近咨询" width="150" />
      <el-table-column prop="symptomsCount" label="症状记录" width="100" />
      <el-table-column prop="reportsCount" label="评估报告" width="100" />
      <el-table-column label="操作" fixed="right" width="250">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button type="success" size="small" @click="chat(row)">沟通</el-button>
          <el-button type="warning" size="small" @click="showReleaseDialog(row)">释放</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadPatients"
      style="margin-top: 20px; text-align: center"
    />

    <!-- 患者详情对话框 -->
    <el-dialog v-model="detailVisible" title="患者详情" width="80%" top="5vh">
      <el-descriptions v-if="currentPatient" :column="2" border>
        <el-descriptions-item label="姓名">{{ currentPatient.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentPatient.gender === 'MALE' ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentPatient.age }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentPatient.phone }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">
          <el-tag :type="getRiskType(currentPatient.riskLevel)">
            {{ getRiskText(currentPatient.riskLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近咨询">{{ currentPatient.lastConsultTime }}</el-descriptions-item>
        <el-descriptions-item label="病史" :span="2">{{ currentPatient.medicalHistory || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 释放患者对话框 -->
    <el-dialog v-model="releaseDialogVisible" title="释放患者" width="500px">
      <div v-if="currentPatient">
        <el-alert
          title="确认释放此患者？"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <template #default>
            <p>患者姓名: {{ currentPatient.name }}</p>
            <p style="margin-top: 8px; color: #E6A23C;">
              释放后，患者将返回患者公海，您将不再负责此患者的诊疗工作。
            </p>
          </template>
        </el-alert>

        <el-form :model="releaseForm">
          <el-form-item label="释放理由" required>
            <el-input
              v-model="releaseForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入释放理由（必填），提交后需等待管理员审核"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="releaseDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitRelease" :loading="submitting">提交释放申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { doctorApi } from '@/api'
import type { User } from '@/types'

const router = useRouter()

const searchForm = ref({
  keyword: '',
  riskLevel: ''
})

const patients = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const releaseDialogVisible = ref(false)
const currentPatient = ref<User | null>(null)
const releaseForm = ref({
  reason: ''
})
const submitting = ref(false)

const loadPatients = async () => {
  loading.value = true
  try {
    const res = await doctorApi.getPatients({
      pageNum: page.value,
      pageSize: pageSize.value,
      keyword: searchForm.value.keyword || undefined
    })
    if (res.code === 200 && res.data) {
      patients.value = res.data.records.map((item: any) => ({
        id: item.id,
        name: item.nickname || item.username,
        gender: item.gender,
        age: item.age,
        phone: item.phone,
        riskLevel: 'LOW',
        lastConsultTime: item.lastAppointmentTime ?
          item.lastAppointmentTime.replace('T', ' ').substring(0, 16) : '暂无',
        symptomsCount: item.appointmentCount || 0,
        reportsCount: item.assessmentReports || 0
      }))
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载患者列表失败')
    console.error('Failed to load patients:', error)
  } finally {
    loading.value = false
  }
}

const viewDetail = async (patient: any) => {
  try {
    const res = await doctorApi.getPatientDetail(patient.id)
    if (res.code === 200 && res.data) {
      currentPatient.value = {
        ...res.data,
        name: res.data.nickname || res.data.username,
        riskLevel: 'LOW',
        lastConsultTime: res.data.lastAppointmentTime ?
          res.data.lastAppointmentTime.replace('T', ' ').substring(0, 16) : '暂无',
        medicalHistory: '暂无病史'
      }
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载患者详情失败')
    console.error('Failed to load patient detail:', error)
  }
}

const chat = (patient: User) => {
  router.push({
    path: '/doctor/chat',
    query: { patientId: patient.id }
  })
}

const getRiskText = (level: string) => {
  const map: Record<string, string> = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  }
  return map[level] || '未评估'
}

const getRiskType = (level: string): any => {
  const map: Record<string, string> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  }
  return map[level] || 'info'
}

const showReleaseDialog = (patient: User) => {
  currentPatient.value = patient
  releaseForm.value.reason = ''
  releaseDialogVisible.value = true
}

const submitRelease = async () => {
  if (!releaseForm.value.reason.trim()) {
    ElMessage.warning('请输入释放理由')
    return
  }

  if (!currentPatient.value) return

  submitting.value = true
  try {
    const res = await doctorApi.releasePatient(currentPatient.value.id, {
      reason: releaseForm.value.reason
    })
    if (res.code === 200) {
      ElMessage.success('释放申请已提交，请等待管理员审核')
      releaseDialogVisible.value = false
      loadPatients()
    } else {
      ElMessage.error(res.message || '提交释放申请失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交释放申请失败')
    console.error('Failed to release patient:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPatients()
})
</script>

<style scoped lang="scss">
.patients-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}
</style>
