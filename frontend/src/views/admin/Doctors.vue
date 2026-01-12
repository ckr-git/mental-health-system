<template>
  <div class="doctors-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>👨‍⚕️ 医生管理</h2>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索医生姓名或用户名"
            style="width: 300px"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <!-- Status filter tabs -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="status-tabs">
        <el-tab-pane label="全部医生" name="all">
          <template #label>
            <span>全部医生 <el-badge :value="statistics.all" class="badge" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="待审核" name="pending">
          <template #label>
            <span>⏳ 待审核 <el-badge :value="statistics.pending" type="warning" class="badge" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已通过" name="approved">
          <template #label>
            <span>✅ 已通过 <el-badge :value="statistics.approved" type="success" class="badge" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已拒绝" name="rejected">
          <template #label>
            <span>❌ 已拒绝 <el-badge :value="statistics.rejected" type="danger" class="badge" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="患者分配审核" name="patientAssignments">
          <template #label>
            <span>🔄 患者分配审核 <el-badge :value="assignmentRequests.length" type="warning" class="badge" /></span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- Doctor list table -->
      <el-table
        v-if="activeTab !== 'patientAssignments'"
        :data="doctorList"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="#" width="60" />

        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar
              :src="row.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
              :size="50"
            />
          </template>
        </el-table-column>

        <el-table-column prop="nickname" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />

        <el-table-column prop="specialization" label="专业领域" min-width="200">
          <template #default="{ row }">
            <el-tag v-if="row.specialization" type="info">{{ row.specialization }}</el-tag>
            <span v-else style="color: #909399">未填写</span>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="手机号" width="120">
          <template #default="{ row }">
            {{ row.phone || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" width="180">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 2" type="danger">已拒绝</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="180" />

        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <!-- Pending status: show approve/reject buttons -->
            <template v-if="row.status === 0">
              <el-button type="success" size="small" @click="handleApprove(row, true)">
                ✅ 通过
              </el-button>
              <el-button type="danger" size="small" @click="handleApprove(row, false)">
                ❌ 拒绝
              </el-button>
            </template>

            <!-- Approved status: show disable button -->
            <template v-else-if="row.status === 1">
              <el-button type="warning" size="small" @click="handleToggleStatus(row)">
                🔒 禁用
              </el-button>
            </template>

            <!-- Rejected or disabled status: show enable button -->
            <template v-else>
              <el-button type="success" size="small" @click="handleToggleStatus(row)">
                🔓 启用
              </el-button>
            </template>

            <el-button type="primary" size="small" @click="handleEdit(row)">
              📋 编辑
            </el-button>
            <el-button type="info" size="small" @click="handleViewStatistics(row)">
              📊 统计
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Patient Assignment Requests Table -->
      <el-table
        v-if="activeTab === 'patientAssignments'"
        :data="assignmentRequests"
        v-loading="assignmentLoading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="#" width="60" />

        <el-table-column prop="id" label="请求ID" width="80" />

        <el-table-column label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.operationType === 'claim' ? 'success' : 'warning'">
              {{ row.operationType === 'claim' ? '认领' : '释放' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="医生信息" min-width="150">
          <template #default="{ row }">
            <div>{{ row.doctorNickname || row.doctorUsername }}</div>
            <div style="font-size: 12px; color: #909399">ID: {{ row.doctorId }}</div>
          </template>
        </el-table-column>

        <el-table-column label="患者信息" min-width="150">
          <template #default="{ row }">
            <div>{{ row.patientNickname || row.patientUsername }}</div>
            <div style="font-size: 12px; color: #909399">ID: {{ row.patientId }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="requestReason" label="申请理由" min-width="200" show-overflow-tooltip />

        <el-table-column prop="createTime" label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApproveAssignment(row)">
              ✅ 通过
            </el-button>
            <el-button type="danger" size="small" @click="handleRejectAssignment(row)">
              ❌ 拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchDoctorList"
        @current-change="fetchDoctorList"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="editDialogVisible"
      title="📋 编辑医生信息"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="100px" :rules="editRules" ref="editFormRef">
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="专业领域" prop="specialization">
          <el-input
            v-model="editForm.specialization"
            placeholder="如：临床心理学、认知行为疗法等"
            type="textarea"
            :rows="2"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
            <el-radio :label="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="年龄">
          <el-input-number v-model="editForm.age" :min="20" :max="100" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit" :loading="saveLoading">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- Statistics Dialog -->
    <el-dialog
      v-model="statsDialogVisible"
      title="📊 医生工作统计"
      width="700px"
    >
      <div v-loading="statsLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="医生姓名">
            {{ currentStats.doctorName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="专业领域">
            {{ currentStats.specialization || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <el-tag v-if="currentStats.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="currentStats.status === 1" type="success">已通过</el-tag>
            <el-tag v-else-if="currentStats.status === 2" type="danger">已拒绝</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="医生ID">
            {{ currentStats.doctorId || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>工作数据统计</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-statistic title="👥 服务患者数" :value="currentStats.patientCount || 0" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="💬 咨询次数" :value="currentStats.consultationCount || 0" />
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12">
            <el-statistic title="📋 评估报告数" :value="currentStats.reportCount || 0" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="⭐ 平均评分" :value="currentStats.averageRating || 0" :precision="1" />
          </el-col>
        </el-row>

        <el-alert
          v-if="currentStats.patientCount === 0 && currentStats.consultationCount === 0"
          title="暂无工作数据"
          type="info"
          :closable="false"
          style="margin-top: 20px"
        />
      </div>

      <template #footer>
        <el-button @click="statsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api'
import type { UserInfo } from '@/types'

// State
const loading = ref(false)
const saveLoading = ref(false)
const statsLoading = ref(false)
const assignmentLoading = ref(false)
const activeTab = ref('all')
const searchKeyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const doctorList = ref<UserInfo[]>([])
const assignmentRequests = ref<any[]>([])

// Dialog states
const editDialogVisible = ref(false)
const statsDialogVisible = ref(false)

// Edit form
const editFormRef = ref<FormInstance>()
const editForm = reactive<Partial<UserInfo>>({
  id: undefined,
  nickname: '',
  specialization: '',
  phone: '',
  email: '',
  gender: 0,
  age: undefined
})

const editRules: FormRules = {
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}

// Statistics
const currentStats = reactive<Record<string, any>>({
  doctorId: null,
  doctorName: '',
  specialization: '',
  status: null,
  patientCount: 0,
  consultationCount: 0,
  reportCount: 0,
  averageRating: 0
})

// Computed statistics for tabs
const statistics = computed(() => {
  return {
    all: doctorList.value.length,
    pending: doctorList.value.filter(d => d.status === 0).length,
    approved: doctorList.value.filter(d => d.status === 1).length,
    rejected: doctorList.value.filter(d => d.status === 2).length
  }
})

// Fetch doctor list
const fetchDoctorList = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      role: 'DOCTOR'
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    const res = await adminApi.getUsers(params)
    if (res.code === 200 && res.data) {
      const allDoctors = res.data.records || []

      // Filter by status based on active tab
      if (activeTab.value === 'pending') {
        doctorList.value = allDoctors.filter(d => d.status === 0)
      } else if (activeTab.value === 'approved') {
        doctorList.value = allDoctors.filter(d => d.status === 1)
      } else if (activeTab.value === 'rejected') {
        doctorList.value = allDoctors.filter(d => d.status === 2)
      } else {
        doctorList.value = allDoctors
      }

      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch doctor list:', error)
    ElMessage.error('获取医生列表失败')
  } finally {
    loading.value = false
  }
}

// Handle tab change
const handleTabChange = () => {
  pageNum.value = 1
  if (activeTab.value === 'patientAssignments') {
    fetchAssignmentRequests()
  } else {
    fetchDoctorList()
  }
}

// Handle search
const handleSearch = () => {
  pageNum.value = 1
  fetchDoctorList()
}

// Handle approve/reject
const handleApprove = async (doctor: UserInfo, approve: boolean) => {
  const action = approve ? '通过' : '拒绝'
  const confirmText = approve
    ? `确定通过医生 "${doctor.nickname}" 的审核吗？通过后该医生将可以正常登录使用系统。`
    : `确定拒绝医生 "${doctor.nickname}" 的审核吗？拒绝后该医生将无法登录系统。`

  try {
    await ElMessageBox.confirm(confirmText, `${action}审核`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: approve ? 'success' : 'warning'
    })

    const res = await adminApi.approveDoctor(doctor.id!, approve)
    if (res.code === 200) {
      ElMessage.success(res.data || `${action}成功`)
      fetchDoctorList()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to approve doctor:', error)
      ElMessage.error(`${action}失败`)
    }
  }
}

// Handle toggle status (enable/disable)
const handleToggleStatus = async (doctor: UserInfo) => {
  const isActive = doctor.status === 1
  const action = isActive ? '禁用' : '启用'
  const confirmText = isActive
    ? `确定要禁用医生 "${doctor.nickname}" 吗？禁用后该医生将无法登录系统。`
    : `确定要启用医生 "${doctor.nickname}" 吗？启用后该医生将可以正常登录使用系统。`

  try {
    await ElMessageBox.confirm(confirmText, `${action}账户`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const res = await adminApi.toggleUserStatus(doctor.id!)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchDoctorList()
    } else {
      ElMessage.error(res.message || `${action}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to toggle status:', error)
      ElMessage.error(`${action}失败`)
    }
  }
}

// Handle edit
const handleEdit = (doctor: UserInfo) => {
  Object.assign(editForm, {
    id: doctor.id,
    nickname: doctor.nickname,
    specialization: doctor.specialization || '',
    phone: doctor.phone || '',
    email: doctor.email || '',
    gender: doctor.gender || 0,
    age: doctor.age || undefined
  })
  editDialogVisible.value = true
}

// Handle save edit
const handleSaveEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        const res = await adminApi.updateDoctor(editForm.id!, editForm)
        if (res.code === 200) {
          ElMessage.success('更新成功')
          editDialogVisible.value = false
          fetchDoctorList()
        } else {
          ElMessage.error(res.message || '更新失败')
        }
      } catch (error) {
        console.error('Failed to update doctor:', error)
        ElMessage.error('更新失败')
      } finally {
        saveLoading.value = false
      }
    }
  })
}

// Handle view statistics
const handleViewStatistics = async (doctor: UserInfo) => {
  statsDialogVisible.value = true
  statsLoading.value = true

  try {
    const res = await adminApi.getDoctorStatistics(doctor.id!)
    if (res.code === 200 && res.data) {
      Object.assign(currentStats, res.data)
    }
  } catch (error) {
    console.error('Failed to fetch doctor statistics:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    statsLoading.value = false
  }
}

// Fetch assignment requests
const fetchAssignmentRequests = async () => {
  assignmentLoading.value = true
  try {
    const res = await adminApi.getPendingAssignments({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.code === 200 && res.data) {
      assignmentRequests.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to fetch assignment requests:', error)
    ElMessage.error('获取患者分配请求失败')
  } finally {
    assignmentLoading.value = false
  }
}

// Handle approve assignment
const handleApproveAssignment = async (request: any) => {
  const operationType = request.operationType === 'claim' ? '认领' : '释放'
  try {
    await ElMessageBox.confirm(
      `确定通过此${operationType}申请吗？`,
      '审核通过',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    const res = await adminApi.approveAssignment(request.id)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      fetchAssignmentRequests()
    } else {
      ElMessage.error(res.message || '审核失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to approve assignment:', error)
      ElMessage.error('审核失败')
    }
  }
}

// Handle reject assignment
const handleRejectAssignment = async (request: any) => {
  const operationType = request.operationType === 'claim' ? '认领' : '释放'
  try {
    const { value: adminNote } = await ElMessageBox.prompt(
      `请输入拒绝理由（${operationType}申请）`,
      '审核拒绝',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入拒绝理由',
        inputValidator: (value) => {
          if (!value || !value.trim()) {
            return '请输入拒绝理由'
          }
          return true
        }
      }
    )

    const res = await adminApi.rejectAssignment(request.id, { adminNote })
    if (res.code === 200) {
      ElMessage.success('已拒绝申请')
      fetchAssignmentRequests()
    } else {
      ElMessage.error(res.message || '拒绝失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to reject assignment:', error)
      ElMessage.error('拒绝失败')
    }
  }
}

// Format date time
const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '暂无'
  return dateTime.replace('T', ' ').substring(0, 16)
}

// Lifecycle
onMounted(() => {
  fetchDoctorList()
})
</script>

<style scoped>
.doctors-container {
  padding: 20px;
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

.status-tabs {
  margin-bottom: 20px;
}

.badge {
  margin-left: 5px;
}

:deep(.el-statistic__head) {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

:deep(.el-statistic__content) {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}
</style>
