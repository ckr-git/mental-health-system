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
import { onMounted } from 'vue'
import { Calendar, Clock, Select, CircleCheck, CircleClose, Warning } from '@element-plus/icons-vue'
import { useAppointments } from './appointments/useAppointments'
import { getStatusName, getStatusTag, getAppointmentTypeName, getAppointmentTypeTag, formatDateTime } from './appointments/helpers'

const {
  loading, saving, appointmentDialogVisible, cancelDialogVisible,
  statistics, filters, pagination, appointmentList,
  currentAppointment, cancelReason,
  loadStatistics, loadAppointments, resetFilters,
  showAppointmentDialog, resetAppointmentForm, saveAppointment,
  confirmAppointment, showCancelDialog, submitCancel,
  completeAppointment, deleteAppointment
} = useAppointments()

onMounted(() => { loadStatistics(); loadAppointments() })
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
