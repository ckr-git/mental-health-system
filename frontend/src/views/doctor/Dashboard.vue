<template>
  <div class="doctor-dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="32"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.title }}</div>
            </div>
          </div>
          <div class="stat-trend" :class="{ increase: stat.trend > 0, decrease: stat.trend < 0 }">
            <el-icon><component :is="stat.trend > 0 ? 'TrendCharts' : 'Bottom'" /></el-icon>
            <span>{{ Math.abs(stat.trend) }}% vs. 历史</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预约趋势</span>
              <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
                <el-radio-button label="week">近 7 天</el-radio-button>
                <el-radio-button label="month">近 30 天</el-radio-button>
                <el-radio-button label="quarter">近 90 天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div id="trendChart" style="height: 350px"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card style="height: 100%">
          <template #header>
            <span>近期日程</span>
          </template>
          <el-scrollbar height="320px">
            <el-timeline>
              <el-timeline-item
                v-for="schedule in schedules"
                :key="schedule.id"
                :timestamp="schedule.time"
                placement="top"
                :color="schedule.status === 'completed' ? '#67C23A' : '#409EFF'"
              >
                <el-card shadow="never" class="schedule-card">
                  <div class="schedule-info">
                    <span class="schedule-patient">{{ schedule.patientName }}</span>
                    <el-tag :type="schedule.type === 'consultation' ? 'primary' : 'success'" size="small">
                      {{ schedule.type === 'consultation' ? '问诊' : '会谈' }}
                    </el-tag>
                  </div>
                  <div class="schedule-desc">{{ schedule.description }}</div>
                  <el-button
                    v-if="schedule.status !== 'completed'"
                    type="primary"
                    size="small"
                    @click="startConsultation(schedule)"
                  >
                    开始
                  </el-button>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="schedules.length === 0" description="暂无日程" :image-size="80" />
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最新报告</span>
              <el-button type="text" @click="$router.push('/doctor/reports')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="pendingReports" style="width: 100%" max-height="300">
            <el-table-column prop="patientName" label="患者" width="120" />
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleReport(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>消息提醒</span>
              <el-button type="text" @click="$router.push('/doctor/chat')">进入会话</el-button>
            </div>
          </template>
          <el-table :data="unreadMessages" style="width: 100%" max-height="300">
            <el-table-column prop="patientName" label="患者" width="120" />
            <el-table-column prop="lastMessage" label="最新消息" show-overflow-tooltip />
            <el-table-column prop="time" label="时间" width="140" />
            <template #empty>
              <el-empty description="即时沟通待接入 WebSocket" :image-size="60" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Document, ChatDotRound, Calendar, TrendCharts, Bottom } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { doctorApi } from '@/api'

const router = useRouter()

const stats = ref([
  { title: '我的患者', value: 0, trend: 0, color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', icon: User },
  { title: '评估报告', value: 0, trend: 0, color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', icon: Document },
  { title: '今日会话', value: 0, trend: 0, color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', icon: ChatDotRound },
  { title: '总预约', value: 0, trend: 0, color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', icon: Calendar }
])

const trendPeriod = ref('week')
const schedules = ref<any[]>([])
const pendingReports = ref<any[]>([])
const unreadMessages = ref<any[]>([])

const loadStats = async () => {
  try {
    const res = await doctorApi.getDashboardStatistics()
    if (res.code === 200 && res.data) {
      stats.value[0].value = res.data.totalPatients || 0
      stats.value[1].value = res.data.totalReports || 0
      stats.value[2].value = res.data.todayAppointments || 0
      stats.value[3].value = res.data.totalAppointments || 0
      // 趋势数据暂无专用接口，保持 0 确保界面稳定
      stats.value.forEach((item) => (item.trend = 0))
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败')
    console.error('Failed to load statistics:', error)
  }
}

const loadTrendData = async () => {
  try {
    const days = trendPeriod.value === 'week' ? 7 : trendPeriod.value === 'month' ? 30 : 90
    const res = await doctorApi.getAppointments({ pageNum: 1, pageSize: 200 })
    const appointments = res.data?.records || []
    const today = dayjs().startOf('day')

    const dates: string[] = []
    const counts: number[] = []
    for (let i = days - 1; i >= 0; i--) {
      const date = today.subtract(i, 'day').format('YYYY-MM-DD')
      dates.push(date)
      const count = appointments.filter((item: any) =>
        dayjs(item.appointmentTime).format('YYYY-MM-DD') === date
      ).length
      counts.push(count)
    }

    renderTrendChart({ dates, counts })
  } catch (error) {
    console.error('Failed to load trend data', error)
    ElMessage.error('加载趋势数据失败')
  }
}

const renderTrendChart = (data: { dates: string[]; counts: number[] }) => {
  const chartDom = document.getElementById('trendChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预约量'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.dates },
    yAxis: { type: 'value' },
    series: [
      {
        name: '预约量',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: data.counts
      }
    ]
  }

  myChart.setOption(option)
  window.addEventListener('resize', () => myChart.resize())
}

const loadSchedules = async () => {
  try {
    const res = await doctorApi.getRecentAppointments()
    const list = res.data || []
    schedules.value = list.map((appointment: any) => ({
      id: appointment.id,
      patientId: appointment.patientId,
      patientName: appointment.patientName || '未知患者',
      time: appointment.appointmentTime ? dayjs(appointment.appointmentTime).format('MM-DD HH:mm') : '',
      type: appointment.appointmentType || 'consultation',
      status: appointment.status === 2 ? 'completed' : 'upcoming',
      description: appointment.appointmentType || '随访/问诊'
    }))
  } catch (error) {
    console.error('Failed to load schedules:', error)
  }
}

const loadPendingReports = async () => {
  try {
    const res = await doctorApi.getReports({ pageNum: 1, pageSize: 5 })
    const list = res.data?.records || []
    pendingReports.value = list.map((item: any) => ({
      id: item.id,
      patientName: item.patientName || '未知患者',
      createTime: item.createTime ? dayjs(item.createTime).format('YYYY-MM-DD HH:mm') : '',
      type: item.reportType || 'General'
    }))
  } catch (error) {
    console.error('加载报告列表失败', error)
  }
}

const loadUnreadMessages = () => {
  // WebSocket 通信尚未接入，先留空以避免页面报错
  unreadMessages.value = []
}

const startConsultation = (schedule: any) => {
  router.push({ path: '/doctor/chat', query: { patientId: schedule.patientId } })
}

const handleReport = (report: any) => {
  router.push({ path: '/doctor/reports', query: { reportId: report.id } })
}

const replyMessage = (message: any) => {
  router.push({ path: '/doctor/chat', query: { patientId: message.patientId } })
}

onMounted(() => {
  loadStats()
  loadTrendData()
  loadSchedules()
  loadPendingReports()
  loadUnreadMessages()
})
</script>

<style scoped lang="scss">
.doctor-dashboard {
  padding: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-bottom: 15px;

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        margin-bottom: 5px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .stat-trend {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    color: #909399;

    &.increase {
      color: #67c23a;
    }

    &.decrease {
      color: #f56c6c;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.schedule-card {
  background: #f5f7fa;
  border: none;

  .schedule-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .schedule-patient {
      font-weight: 600;
    }
  }

  .schedule-desc {
    font-size: 13px;
    color: #909399;
    margin-bottom: 10px;
  }
}
</style>
