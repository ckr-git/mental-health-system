<template>
  <div class="appointments-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>预约管理</h2>
          <div class="filters">
            <el-select v-model="filters.status" placeholder="全部状态" clearable size="small" @change="handleFilter">
              <el-option label="待确认" :value="0" />
              <el-option label="已确认" :value="1" />
              <el-option label="已完成" :value="2" />
              <el-option label="已取消" :value="3" />
            </el-select>
            <el-button type="primary" size="small" @click="handleFilter">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="appointments" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="patientName" label="患者" width="140" />
        <el-table-column prop="appointmentType" label="类型" width="140">
          <template #default="{ row }">
            <el-tag type="info">{{ row.appointmentType || '问诊' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appointmentTime" label="预约时间" width="190">
          <template #default="{ row }">
            {{ formatDate(row.appointmentTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="120">
          <template #default="{ row }">{{ row.duration || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="symptoms" label="症状/备注" show-overflow-tooltip />
        <el-table-column prop="notes" label="补充说明" show-overflow-tooltip />
      </el-table>

      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next, sizes, total"
          :page-sizes="[10, 20, 30]"
          :current-page="filters.pageNum"
          :page-size="filters.pageSize"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { doctorApi } from '@/api'

const loading = ref(false)
const appointments = ref<any[]>([])
const total = ref(0)

const filters = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null as number | null
})

const statusText = (status: number) => {
  switch (status) {
    case 0:
      return '待确认'
    case 1:
      return '已确认'
    case 2:
      return '已完成'
    case 3:
      return '已取消'
    default:
      return '未知'
  }
}

const statusType = (status: number) => {
  switch (status) {
    case 0:
      return 'warning'
    case 1:
      return 'info'
    case 2:
      return 'success'
    case 3:
      return 'danger'
    default:
      return 'info'
  }
}

const formatDate = (val: string) => (val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-')

const loadAppointments = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    }
    if (filters.status !== null && filters.status !== undefined) {
      params.status = filters.status
    }
    const res = await doctorApi.getAppointments(params)
    appointments.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载预约失败', error)
    ElMessage.error('加载预约失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  filters.pageNum = 1
  loadAppointments()
}

const handlePageChange = (page: number) => {
  filters.pageNum = page
  loadAppointments()
}

const handleSizeChange = (size: number) => {
  filters.pageSize = size
  filters.pageNum = 1
  loadAppointments()
}

onMounted(() => {
  loadAppointments()
})
</script>

<style scoped>
.appointments-container {
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

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
