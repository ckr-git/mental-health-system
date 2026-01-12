<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF"><Edit /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.recordCount }}</div>
              <div class="stat-label">情绪日记</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.reportCount }}</div>
              <div class="stat-label">评估报告</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C"><ChatDotRound /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.aiChatCount }}</div>
              <div class="stat-label">AI对话</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C"><TrendCharts /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stats.avgMood }}/10</div>
              <div class="stat-label">平均心情</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>情绪趋势分析</span>
              <div class="chart-controls">
                <el-radio-group v-model="chartTimeRange" size="small" @change="loadData">
                  <el-radio-button label="7">7天</el-radio-button>
                  <el-radio-button label="30">30天</el-radio-button>
                  <el-radio-button label="90">90天</el-radio-button>
                </el-radio-group>
                <el-checkbox-group v-model="chartDimensions" size="small" style="margin-left: 16px">
                  <el-checkbox label="mood">心情</el-checkbox>
                  <el-checkbox label="energy">精力</el-checkbox>
                  <el-checkbox label="sleep">睡眠</el-checkbox>
                  <el-checkbox label="stress">压力</el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
          </template>
          <v-chart :option="moodChartOption" style="height: 350px" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/patient/mood-diary')">📝 写情绪日记</el-button>
            <el-button type="success" @click="$router.push('/patient/room-decoration')">🏠 装饰我的房间</el-button>
            <el-button type="warning" @click="$router.push('/patient/time-capsule')">💌 写时光信</el-button>
            <el-button type="info" @click="$router.push('/patient/ai-chat')">🤖 AI咨询</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最近情绪日记</span>
          </template>
          <el-timeline>
            <el-timeline-item v-for="record in recentRecords" :key="record.id" :timestamp="record.createTime" placement="top">
              <el-card>
                <h4>{{ record.symptomType }}</h4>
                <p>{{ record.description }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>推荐资源</span>
          </template>
          <el-space direction="vertical" style="width: 100%">
            <el-card v-for="resource in recommendedResources" :key="resource.id" shadow="hover" class="resource-card">
              <h4>{{ resource.title }}</h4>
              <p>{{ resource.content?.substring(0, 100) }}...</p>
              <el-button type="primary" link @click="viewResource(resource.id)">查看详情</el-button>
            </el-card>
          </el-space>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { symptomApi, recommendApi } from '@/api'
import type { SymptomRecord, MentalResource } from '@/types'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const router = useRouter()

const stats = ref({
  recordCount: 0,
  reportCount: 0,
  aiChatCount: 0,
  avgMood: 0
})

const recentRecords = ref<SymptomRecord[]>([])
const recommendedResources = ref<MentalResource[]>([])

// 图表控制
const chartTimeRange = ref('7')
const chartDimensions = ref(['mood'])  // 默认只显示心情

// 维度配置
const dimensionConfig: Record<string, any> = {
  mood: {
    name: '心情指数',
    color: '#409EFF',
    field: 'moodScore'
  },
  energy: {
    name: '精力水平',
    color: '#67C23A',
    field: 'energyLevel'
  },
  sleep: {
    name: '睡眠质量',
    color: '#E6A23C',
    field: 'sleepQuality'
  },
  stress: {
    name: '压力水平',
    color: '#F56C6C',
    field: 'stressLevel'
  }
}

const moodChartOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross'
    }
  },
  legend: {
    data: [],
    bottom: 0
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '50px',
    top: '10px',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: []
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 10,
    splitLine: {
      lineStyle: {
        type: 'dashed'
      }
    }
  },
  series: []
})

const loadData = async () => {
  try {
    // 获取指定天数的情绪日记数据
    const days = parseInt(chartTimeRange.value)
    const diaryRes = await symptomApi.getRecent(days)
    const diaries = diaryRes.data || []

    // 计算统计数据
    stats.value.recordCount = diaries.length
    if (diaries.length > 0) {
      stats.value.avgMood = Math.round(
        diaries.reduce((sum, d) => sum + d.moodScore, 0) / diaries.length
      )
    }

    // 更新心情趋势图表
    if (diaries.length > 0) {
      // 按日期排序(从旧到新)
      const sortedDiaries = [...diaries].sort((a, b) =>
        new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
      )

      // 提取日期
      const dates = sortedDiaries.map(d => {
        const date = new Date(d.createTime)
        return `${date.getMonth() + 1}/${date.getDate()}`
      })

      // 更新图表X轴
      moodChartOption.value.xAxis.data = dates

      // 根据选中的维度生成series
      const series: any[] = []
      const legendData: string[] = []

      chartDimensions.value.forEach(dim => {
        const config = dimensionConfig[dim]
        if (!config) return

        const data = sortedDiaries.map(d => d[config.field] || 0)

        legendData.push(config.name)
        series.push({
          name: config.name,
          type: 'line',
          data: data,
          smooth: true,
          lineStyle: {
            color: config.color,
            width: 2
          },
          itemStyle: {
            color: config.color
          },
          areaStyle: dim === 'mood' ? {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: config.color.replace(')', ', 0.3)').replace('rgb', 'rgba') },
                { offset: 1, color: config.color.replace(')', ', 0.1)').replace('rgb', 'rgba') }
              ]
            }
          } : undefined
        })
      })

      moodChartOption.value.series = series
      moodChartOption.value.legend.data = legendData
    } else {
      // 无数据时清空图表
      moodChartOption.value.xAxis.data = []
      moodChartOption.value.series = []
      moodChartOption.value.legend.data = []
    }

    // 最近情绪日记(用于时间线显示)
    recentRecords.value = diaries.slice(0, 5)

    // 加载推荐资源
    const resources = await recommendApi.getResources(5)
    recommendedResources.value = resources.data
  } catch (error) {
    console.error('Failed to load data:', error)
  }
}

const viewResource = (id: number) => {
  router.push(`/patient/resources?id=${id}`)
}

// 监听维度变化重新加载数据
watch(chartDimensions, () => {
  loadData()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.chart-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
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
  font-size: 40px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-actions .el-button {
  width: 100%;
}

.resource-card {
  cursor: pointer;
}

.resource-card h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.resource-card p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}
</style>
