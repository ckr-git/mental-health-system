<template>
  <div class="patient-pool-container">
    <el-card class="header-card">
      <div class="header-content">
        <h2>患者公海</h2>
        <p class="subtitle">从公海中认领患者（需管理员审核通过）</p>
      </div>
    </el-card>

    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="患者姓名">
          <el-input v-model="searchForm.keyword" placeholder="搜索患者" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPatientPool">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="patients" style="width: 100%; margin-top: 20px" v-loading="loading">
      <el-table-column prop="nickname" label="患者姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" />
      <el-table-column prop="createTime" label="注册时间" width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showClaimDialog(row)">认领</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadPatientPool"
      style="margin-top: 20px; text-align: center"
    />

    <!-- 认领患者对话框 -->
    <el-dialog v-model="claimDialogVisible" title="认领患者" width="500px">
      <div v-if="currentPatient">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="患者姓名">{{ currentPatient.nickname || currentPatient.username }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ currentPatient.gender === 'MALE' ? '男' : currentPatient.gender === 'FEMALE' ? '女' : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="年龄">{{ currentPatient.age || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentPatient.phone || '未填写' }}</el-descriptions-item>
        </el-descriptions>

        <el-form :model="claimForm" style="margin-top: 20px">
          <el-form-item label="认领理由" required>
            <el-input
              v-model="claimForm.reason"
              type="textarea"
              :rows="4"
              placeholder="请输入认领理由（必填），提交后需等待管理员审核"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="claimDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClaim" :loading="submitting">提交认领申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { doctorApi } from '@/api'

const searchForm = ref({
  keyword: ''
})

const patients = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const claimDialogVisible = ref(false)
const currentPatient = ref<any>(null)
const claimForm = ref({
  reason: ''
})
const submitting = ref(false)

const loadPatientPool = async () => {
  loading.value = true
  try {
    const res = await doctorApi.getPatientPool({
      pageNum: page.value,
      pageSize: pageSize.value,
      keyword: searchForm.value.keyword || undefined
    })
    if (res.code === 200 && res.data) {
      patients.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载患者公海失败')
    console.error('Failed to load patient pool:', error)
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.value.keyword = ''
  page.value = 1
  loadPatientPool()
}

const showClaimDialog = (patient: any) => {
  currentPatient.value = patient
  claimForm.value.reason = ''
  claimDialogVisible.value = true
}

const submitClaim = async () => {
  if (!claimForm.value.reason.trim()) {
    ElMessage.warning('请输入认领理由')
    return
  }

  submitting.value = true
  try {
    const res = await doctorApi.claimPatient(currentPatient.value.id, {
      reason: claimForm.value.reason
    })
    if (res.code === 200) {
      ElMessage.success('认领申请已提交，请等待管理员审核')
      claimDialogVisible.value = false
      loadPatientPool()
    } else {
      ElMessage.error(res.message || '提交认领申请失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交认领申请失败')
    console.error('Failed to claim patient:', error)
  } finally {
    submitting.value = false
  }
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '暂无'
  return dateTime.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  loadPatientPool()
})
</script>

<style scoped lang="scss">
.patient-pool-container {
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

.search-card {
  margin-bottom: 20px;
}
</style>
