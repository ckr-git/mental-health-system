<template>
  <div class="reports-container">
    <el-card class="header-card">
      <div class="header-content">
        <h2>我的评估报告</h2>
        <el-button type="primary" @click="createAssessment">
          <el-icon><Plus /></el-icon> 创建新评估
        </el-button>
      </div>
    </el-card>

    <el-timeline style="margin-top: 30px; padding: 0 20px">
      <el-timeline-item
        v-for="report in reports"
        :key="report.id"
        :timestamp="report.createTime"
        placement="top"
        :color="getStatusColor(report.status)"
      >
        <el-card shadow="hover" class="report-card">
          <div class="report-header">
            <div>
              <h3>{{ report.title || '心理健康评估报告' }}</h3>
              <el-tag :type="getStatusType(report.status)" size="small">
                {{ getStatusText(report.status) }}
              </el-tag>
            </div>
            <div class="report-actions">
              <el-button type="primary" size="small" @click="viewReport(report)" v-if="report.status === 1">
                查看报告
              </el-button>
              <el-button size="small" disabled v-else>
                {{ report.status === 2 ? '评估中...' : '草稿' }}
              </el-button>
            </div>
          </div>

          <div class="report-content" v-if="report.status === 1">
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-label">综合评分</div>
                  <div class="stat-value" :style="{ color: getScoreColor(report.overallScore) }">
                    {{ report.overallScore }}
                  </div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-label">焦虑指数</div>
                  <div class="stat-value">{{ report.anxietyScore || 'N/A' }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-label">抑郁指数</div>
                  <div class="stat-value">{{ report.depressionScore || 'N/A' }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-label">压力指数</div>
                  <div class="stat-value">{{ report.stressScore || 'N/A' }}</div>
                </div>
              </el-col>
            </el-row>
            
            <el-divider />
            
            <div class="report-summary">
              <p><strong>评估摘要：</strong></p>
              <p>{{ report.summary || '暂无摘要' }}</p>
            </div>
            
            <div class="report-doctor" v-if="report.doctorName">
              <el-icon><UserFilled /></el-icon>
              <span>评估医生：{{ report.doctorName }}</span>
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>

    <el-empty v-if="reports.length === 0 && !loading" description="暂无评估报告">
      <el-button type="primary" @click="createAssessment">创建首个评估</el-button>
    </el-empty>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadReports"
      style="margin-top: 20px; text-align: center"
    />

    <!-- 报告详情对话框 -->
    <el-dialog v-model="detailVisible" title="评估报告详情" width="80%" top="5vh">
      <div v-if="currentReport" class="report-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报告编号">{{ currentReport.id }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentReport.createTime }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">
            <el-tag :type="currentReport.overallScore >= 80 ? 'success' : currentReport.overallScore >= 60 ? 'warning' : 'danger'">
              {{ currentReport.overallScore }} 分
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评估医生">{{ currentReport.doctorName || '系统自动评估' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">详细分析</el-divider>
        
        <div class="detail-section">
          <h4>各维度评分</h4>
          <div id="scoreChart" style="height: 300px"></div>
        </div>

        <div class="detail-section">
          <h4>评估结论</h4>
          <div class="conclusion-content" v-html="currentReport.conclusion"></div>
        </div>

        <div class="detail-section">
          <h4>建议与指导</h4>
          <div class="suggestion-content" v-html="currentReport.suggestions"></div>
        </div>

        <div class="detail-section" v-if="currentReport.doctorComments">
          <h4>医生评语</h4>
          <el-alert :title="currentReport.doctorComments" type="info" :closable="false" />
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadReport">下载报告</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, UserFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { reportApi, aiApi } from '@/api'
import type { AssessmentReport } from '@/types'

const router = useRouter()

const reports = ref<AssessmentReport[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentReport = ref<AssessmentReport | null>(null)

const loadReports = async () => {
  loading.value = true
  try {
    const { data } = await reportApi.getList({
      pageNum: page.value,
      pageSize: pageSize.value
    })
    reports.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error('加载报告失败')
  } finally {
    loading.value = false
  }
}

const createAssessment = async () => {
  const loading = ElMessage({
    message: '正在生成评估报告，请稍候...',
    type: 'info',
    duration: 0
  })

  try {
    const { data } = await aiApi.generateReport()
    loading.close()
    ElMessage.success(data || '评估报告生成成功！')
    // 重新加载报告列表
    await loadReports()
  } catch (error: any) {
    loading.close()
    ElMessage.error(error.message || '生成报告失败，请确保您已记录足够的症状数据')
  }
}

const viewReport = async (report: AssessmentReport) => {
  try {
    const { data } = await reportApi.getDetail(report.id)
    currentReport.value = data
    detailVisible.value = true
    
    // 渲染图表
    nextTick(() => {
      renderChart(data)
    })
  } catch (error) {
    ElMessage.error('加载报告详情失败')
  }
}

const renderChart = (report: AssessmentReport) => {
  const chartDom = document.getElementById('scoreChart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    radar: {
      indicator: [
        { name: '焦虑', max: 100 },
        { name: '抑郁', max: 100 },
        { name: '压力', max: 100 },
        { name: '睡眠质量', max: 100 },
        { name: '情绪稳定', max: 100 },
        { name: '社交能力', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          report.anxietyScore || 0,
          report.depressionScore || 0,
          report.stressScore || 0,
          report.sleepScore || 0,
          report.emotionScore || 0,
          report.socialScore || 0
        ],
        name: '评估分数',
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.3)'
        }
      }]
    }]
  }
  
  myChart.setOption(option)
}

const downloadReport = () => {
  if (!currentReport.value) return
  window.open(`/api/reports/${currentReport.value.id}/download`, '_blank')
  ElMessage.success('正在下载报告...')
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '草稿',
    1: '已完成',
    2: '评估中',
    3: '已取消'
  }
  return map[status] || '未知'
}

const getStatusType = (status: number): any => {
  const map: Record<number, string> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return map[status] || 'info'
}

const getStatusColor = (status: number) => {
  const map: Record<number, string> = {
    0: '#909399',
    1: '#67C23A',
    2: '#E6A23C',
    3: '#F56C6C'
  }
  return map[status] || '#409EFF'
}

const getScoreColor = (score: number) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped lang="scss">
.reports-container {
  padding: 20px;
}

.header-card {
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0;
      font-size: 24px;
    }
  }
}

.report-card {
  .report-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;

    h3 {
      margin: 0 0 10px 0;
      font-size: 18px;
    }
  }

  .report-content {
    .stat-item {
      text-align: center;

      .stat-label {
        font-size: 13px;
        color: #999;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #409EFF;
      }
    }

    .report-summary {
      margin-top: 15px;

      p {
        margin: 5px 0;
        line-height: 1.6;
        color: #666;
      }
    }

    .report-doctor {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 15px;
      font-size: 14px;
      color: #999;
    }
  }
}

.report-detail {
  .detail-section {
    margin: 20px 0;

    h4 {
      margin: 0 0 15px 0;
      font-size: 16px;
      color: #303133;
    }

    .conclusion-content,
    .suggestion-content {
      line-height: 1.8;
      color: #606266;
    }
  }
}
</style>
