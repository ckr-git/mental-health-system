<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-header">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-value">{{ stat.value }}</div>
          </div>
          <div class="stat-label">{{ stat.title }}</div>
          <div class="stat-trend" :class="{ up: stat.trend > 0 }">
            <span>{{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%</span>
            <span class="trend-text">较昨日</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
              <el-radio-group v-model="userTrendPeriod" size="small" @change="loadUserTrend">
                <el-radio-button label="week">近7天</el-radio-button>
                <el-radio-button label="month">近30天</el-radio-button>
                <el-radio-button label="year">近一年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div id="userTrendChart" style="height: 350px"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card style="height: 100%">
          <template #header>
            <span>用户类型分布</span>
          </template>
          <div id="userTypeChart" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>热门资源排行</span>
          </template>
          <div id="resourceChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>系统活跃度</span>
          </template>
          <div id="activityChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近用户注册</span>
              <el-button type="text" @click="$router.push('/admin/users')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentUsers" style="width: 100%">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag :type="row.role === 'PATIENT' ? 'primary' : row.role === 'ADMIN' ? 'danger' : 'success'" size="small">
                  {{ row.role === 'PATIENT' ? '患者' : row.role === 'ADMIN' ? '管理员' : '医生' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统异常日志</span>
              <el-button type="text" @click="viewLogs">查看全部</el-button>
            </div>
          </template>
          <el-table :data="errorLogs" style="width: 100%">
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="row.level === 'ERROR' ? 'danger' : 'warning'" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="错误信息" show-overflow-tooltip />
            <el-table-column prop="time" label="时间" width="150" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Document, ChatDotRound } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'

const router = useRouter()

const stats = ref([
  { title: '总用户数', value: 0, trend: 0, color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', icon: markRaw(User) },
  { title: '患者数量', value: 0, trend: 0, color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', icon: markRaw(UserFilled) },
  { title: '医生数量', value: 0, trend: 0, color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)', icon: markRaw(UserFilled) },
  { title: '评估报告', value: 0, trend: 0, color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', icon: markRaw(Document) }
])

const userTrendPeriod = ref('week')
const recentUsers = ref<any[]>([])
const errorLogs = ref<any[]>([])

const loadStats = async () => {
  try {
    const res = await adminApi.getUserStatistics()
    if (res.code === 200 && res.data) {
      stats.value[0].value = res.data.totalUsers || 0
      stats.value[1].value = res.data.patientCount || 0
      stats.value[2].value = res.data.doctorCount || 0
      stats.value[3].value = 0 // TODO: 评估报告数
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadUserTrend = async () => {
  try {
    // Mock数据
    const mockData = {
      dates: ['11-10', '11-11', '11-12', '11-13', '11-14', '11-15', '11-16'],
      patients: [10, 15, 20, 25, 30, 32, 35],
      doctors: [2, 2, 3, 3, 4, 4, 5],
      total: [12, 17, 23, 28, 34, 36, 40]
    }
    renderUserTrendChart(mockData)
  } catch (error) {
    console.error('加载用户趋势失败')
  }
}

const loadUserType = async () => {
  try {
    const res = await adminApi.getUserStatistics()
    if (res.code === 200 && res.data) {
      renderUserTypeChart({
        patients: res.data.patientCount || 0,
        doctors: res.data.doctorCount || 0,
        admins: 1
      })
    }
  } catch (error) {
    console.error('加载用户分布失败')
  }
}

const loadResourceRank = async () => {
  try {
    // Mock数据
    const mockData = {
      names: ['心理健康指南', '焦虑症自测', '冥想练习', '睡眠改善', '情绪管理'],
      views: [120, 98, 85, 72, 65]
    }
    renderResourceChart(mockData)
  } catch (error) {
    console.error('加载资源排行失败')
  }
}

const loadActivity = async () => {
  try {
    // Mock数据
    const mockData = {
      hours: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'],
      activity: [5, 8, 25, 45, 60, 30]
    }
    renderActivityChart(mockData)
  } catch (error) {
    console.error('加载活跃度失败')
  }
}

const renderUserTrendChart = (data: any) => {
  const chartDom = document.getElementById('userTrendChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['患者', '医生', '总用户'] },
    xAxis: { type: 'category', data: data.dates },
    yAxis: { type: 'value' },
    series: [
      { name: '患者', type: 'line', data: data.patients, smooth: true },
      { name: '医生', type: 'line', data: data.doctors, smooth: true },
      { name: '总用户', type: 'line', data: data.total, smooth: true }
    ]
  }
  myChart.setOption(option)
}

const renderUserTypeChart = (data: any) => {
  const chartDom = document.getElementById('userTypeChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '70%',
      data: [
        { value: data.patients, name: '患者' },
        { value: data.doctors, name: '医生' },
        { value: data.admins, name: '管理员' }
      ],
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  myChart.setOption(option)
}

const renderResourceChart = (data: any) => {
  const chartDom = document.getElementById('resourceChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: data.names },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.views, itemStyle: { color: '#409EFF' } }]
  }
  myChart.setOption(option)
}

const renderActivityChart = (data: any) => {
  const chartDom = document.getElementById('activityChart')
  if (!chartDom) return

  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.hours },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: data.activity,
      smooth: true,
      areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }
    }]
  }
  myChart.setOption(option)
}

const loadRecentUsers = async () => {
  try {
    const res = await adminApi.getUsers({ pageNum: 1, pageSize: 5, role: '' })
    if (res.code === 200 && res.data) {
      recentUsers.value = res.data.records.map((user: any) => ({
        name: user.nickname || user.username,
        phone: user.phone || '-',
        role: user.role,
        createTime: user.createTime
      }))
    }
  } catch (error) {
    console.error('加载最近用户失败', error)
  }
}

const loadErrorLogs = async () => {
  try {
    // Mock数据
    errorLogs.value = []
  } catch (error) {
    console.error('加载错误日志失败')
  }
}

const viewLogs = () => {
  router.push('/admin/logs')
}

onMounted(() => {
  loadStats()
  loadUserTrend()
  loadUserType()
  loadResourceRank()
  loadActivity()
  loadRecentUsers()
  loadErrorLogs()
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  padding: 20px;
}

.stat-card {
  .stat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .stat-icon {
      width: 50px;
      height: 50px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .stat-value {
      font-size: 32px;
      font-weight: bold;
      color: #303133;
    }
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }

  .stat-trend {
    font-size: 13px;
    color: #F56C6C;

    &.up {
      color: #67C23A;
    }

    .trend-text {
      margin-left: 5px;
      color: #909399;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
</style>
