<template>
  <div class="reports-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>📋 评估报告管理</h2>
          <el-button type="primary" @click="showCreateDialog">创建报告</el-button>
        </div>
      </template>

      <!-- Filters -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="患者">
          <el-input v-model="filters.patientId" placeholder="患者ID" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadReports">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- Table -->
      <el-table :data="reportList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="patientName" label="患者" min-width="120" />
        <el-table-column prop="reportType" label="报告类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getReportTypeTag(row.reportType)" size="small">
              {{ getReportTypeName(row.reportType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assessmentDate" label="评估日期" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.assessmentDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="info" size="small" @click="showViewDialog(row)">查看</el-button>
            <el-button type="primary" size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteReport(row.id)">删除</el-button>
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
        @size-change="loadReports"
        @current-change="loadReports"
        class="pagination"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="reportDialogVisible"
      :title="currentReport.id ? '编辑报告' : '创建报告'"
      width="700px"
      @close="resetReportForm"
    >
      <el-form :model="currentReport" label-width="100px">
        <el-form-item label="患者ID" required>
          <el-input v-model="currentReport.patientId" placeholder="请输入患者ID" />
        </el-form-item>
        <el-form-item label="报告类型" required>
          <el-select v-model="currentReport.reportType" placeholder="请选择类型">
            <el-option label="心理评估" value="PSYCHOLOGICAL" />
            <el-option label="情绪分析" value="EMOTIONAL" />
            <el-option label="治疗进展" value="PROGRESS" />
            <el-option label="综合报告" value="COMPREHENSIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估日期" required>
          <el-date-picker
            v-model="currentReport.assessmentDate"
            type="datetime"
            placeholder="请选择评估日期"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报告摘要" required>
          <el-input
            v-model="currentReport.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入报告摘要"
          />
        </el-form-item>
        <el-form-item label="详细内容" required>
          <el-input
            v-model="currentReport.content"
            type="textarea"
            :rows="6"
            placeholder="请输入详细评估内容"
          />
        </el-form-item>
        <el-form-item label="建议">
          <el-input
            v-model="currentReport.recommendations"
            type="textarea"
            :rows="4"
            placeholder="请输入建议和后续治疗方案"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveReport" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- View Dialog -->
    <el-dialog
      v-model="viewDialogVisible"
      title="报告详情"
      width="800px"
    >
      <el-descriptions v-if="viewReport" :column="2" border>
        <el-descriptions-item label="报告ID">{{ viewReport.id }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ viewReport.patientName }}</el-descriptions-item>
        <el-descriptions-item label="报告类型">
          <el-tag :type="getReportTypeTag(viewReport.reportType)" size="small">
            {{ getReportTypeName(viewReport.reportType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="评估日期">
          {{ formatDateTime(viewReport.assessmentDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          {{ viewReport.summary }}
        </el-descriptions-item>
        <el-descriptions-item label="详细内容" :span="2">
          <div style="white-space: pre-wrap">{{ viewReport.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="建议" :span="2">
          <div style="white-space: pre-wrap">{{ viewReport.recommendations }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(viewReport.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(viewReport.updateTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { doctorApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const reportDialogVisible = ref(false)
const viewDialogVisible = ref(false)

const filters = reactive({
  patientId: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const reportList = ref<any[]>([])

const currentReport = reactive({
  id: null as number | null,
  patientId: '',
  reportType: '',
  assessmentDate: '',
  summary: '',
  content: '',
  recommendations: ''
})

const viewReport = ref<any>(null)

// Load reports
const loadReports = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      patientId: filters.patientId ? Number(filters.patientId) : undefined
    }
    const res = await doctorApi.getReports(params)
    if (res.code === 200 && res.data) {
      reportList.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载报告列表失败')
    console.error('Failed to load reports:', error)
  } finally {
    loading.value = false
  }
}

// Reset filters
const resetFilters = () => {
  filters.patientId = ''
  pagination.pageNum = 1
  loadReports()
}

// Show create dialog
const showCreateDialog = () => {
  reportDialogVisible.value = true
}

// Show edit dialog
const showEditDialog = async (report: any) => {
  try {
    const res = await doctorApi.getReportDetail(report.id)
    if (res.code === 200 && res.data) {
      currentReport.id = res.data.id
      currentReport.patientId = String(res.data.patientId)
      currentReport.reportType = res.data.reportType
      currentReport.assessmentDate = res.data.assessmentDate
      currentReport.summary = res.data.summary || ''
      currentReport.content = res.data.content || ''
      currentReport.recommendations = res.data.recommendations || ''
      reportDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载报告详情失败')
    console.error('Failed to load report detail:', error)
  }
}

// Show view dialog
const showViewDialog = async (report: any) => {
  try {
    const res = await doctorApi.getReportDetail(report.id)
    if (res.code === 200 && res.data) {
      viewReport.value = {
        ...res.data,
        patientName: report.patientName
      }
      viewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载报告详情失败')
    console.error('Failed to load report detail:', error)
  }
}

// Reset report form
const resetReportForm = () => {
  currentReport.id = null
  currentReport.patientId = ''
  currentReport.reportType = ''
  currentReport.assessmentDate = ''
  currentReport.summary = ''
  currentReport.content = ''
  currentReport.recommendations = ''
}

// Save report
const saveReport = async () => {
  if (!currentReport.patientId || !currentReport.reportType ||
      !currentReport.assessmentDate || !currentReport.summary) {
    ElMessage.warning('请填写必填项')
    return
  }

  saving.value = true
  try {
    const data = {
      patientId: Number(currentReport.patientId),
      reportType: currentReport.reportType,
      assessmentDate: currentReport.assessmentDate,
      summary: currentReport.summary,
      content: currentReport.content,
      recommendations: currentReport.recommendations
    }

    const res = currentReport.id
      ? await doctorApi.updateReport(currentReport.id, data)
      : await doctorApi.createReport(data)

    if (res.code === 200) {
      ElMessage.success(currentReport.id ? '更新成功' : '创建成功')
      reportDialogVisible.value = false
      loadReports()
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('Failed to save report:', error)
  } finally {
    saving.value = false
  }
}

// Delete report
const deleteReport = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此报告吗？此操作不可恢复。', '警告', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    const res = await doctorApi.deleteReport(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadReports()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('Failed to delete report:', error)
    }
  }
}

// Helper functions
const getReportTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    'PSYCHOLOGICAL': '心理评估',
    'EMOTIONAL': '情绪分析',
    'PROGRESS': '治疗进展',
    'COMPREHENSIVE': '综合报告'
  }
  return typeMap[type] || type
}

const getReportTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    'PSYCHOLOGICAL': 'primary',
    'EMOTIONAL': 'success',
    'PROGRESS': 'warning',
    'COMPREHENSIVE': 'danger'
  }
  return tagMap[type] || ''
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.reports-container {
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
