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
import { onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useDoctors } from './doctors/useDoctors'

const {
  loading, saveLoading, statsLoading, assignmentLoading,
  activeTab, searchKeyword, pageNum, pageSize, total,
  doctorList, assignmentRequests,
  editDialogVisible, statsDialogVisible, editFormRef, editForm, editRules,
  currentStats, statistics,
  fetchDoctorList, handleTabChange, handleSearch,
  handleApprove, handleToggleStatus, handleEdit, handleSaveEdit,
  handleViewStatistics,
  handleApproveAssignment, handleRejectAssignment, formatDateTime
} = useDoctors()

onMounted(() => { fetchDoctorList() })
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
