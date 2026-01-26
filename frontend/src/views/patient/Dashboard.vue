<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-1">
        <div class="stat-icon-wrap">📝</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.recordCount }}</div>
          <div class="stat-label">情绪日记</div>
        </div>
      </div>
      <div class="stat-card stat-card-2">
        <div class="stat-icon-wrap">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.reportCount }}</div>
          <div class="stat-label">评估报告</div>
        </div>
      </div>
      <div class="stat-card stat-card-3">
        <div class="stat-icon-wrap">🤖</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.aiChatCount }}</div>
          <div class="stat-label">AI对话</div>
        </div>
      </div>
      <div class="stat-card stat-card-4">
        <div class="stat-icon-wrap">{{ getMoodEmoji(stats.avgMood) }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgMood }}/10</div>
          <div class="stat-label">平均心情</div>
        </div>
      </div>
    </div>

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
        <el-card class="quick-card">
          <template #header>
            <span class="card-title">快捷操作</span>
          </template>
          <div class="quick-actions">
            <div class="action-btn action-btn-1" @click="$router.push('/patient/mood-diary')">
              <span class="action-icon">📝</span>
              <span class="action-text">写情绪日记</span>
            </div>
            <div class="action-btn action-btn-2" @click="$router.push('/patient/time-capsule')">
              <span class="action-icon">💌</span>
              <span class="action-text">写时光信</span>
            </div>
            <div class="action-btn action-btn-3" @click="$router.push('/patient/ai-chat')">
              <span class="action-icon">🤖</span>
              <span class="action-text">AI咨询</span>
            </div>
            <div class="action-btn action-btn-4" @click="$router.push('/patient/tree-hole')">
              <span class="action-icon">🌳</span>
              <span class="action-text">心情树洞</span>
            </div>
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

// 根据心情分数获取表情
const getMoodEmoji = (score: number) => {
  if (score <= 2) return '😢'
  if (score <= 4) return '😔'
  if (score <= 6) return '😐'
  if (score <= 8) return '😊'
  return '😄'
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
  padding: 0;
}

/* 统计卡片区域 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.stat-card-1 { border-left: 4px solid #FF6B6B; }
.stat-card-2 { border-left: 4px solid #4ECDC4; }
.stat-card-3 { border-left: 4px solid #FFE66D; }
.stat-card-4 { border-left: 4px solid #FF8E8E; }

.stat-icon-wrap {
  font-size: 36px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFF5F5;
  border-radius: 12px;
}

.stat-card-2 .stat-icon-wrap { background: #E8FAF8; }
.stat-card-3 .stat-icon-wrap { background: #FFFBEB; }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2D3436;
}

.stat-label {
  font-size: 13px;
  color: #636E72;
  margin-top: 4px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.action-btn-1 { background: linear-gradient(135deg, #FFF5F5 0%, #FFE4E4 100%); }
.action-btn-1:hover { box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2); }

.action-btn-2 { background: linear-gradient(135deg, #FFFBEB 0%, #FFF3CD 100%); }
.action-btn-2:hover { box-shadow: 0 4px 12px rgba(255, 230, 109, 0.3); }

.action-btn-3 { background: linear-gradient(135deg, #E8FAF8 0%, #D4F5F0 100%); }
.action-btn-3:hover { box-shadow: 0 4px 12px rgba(78, 205, 196, 0.2); }

.action-btn-4 { background: linear-gradient(135deg, #F0FFF4 0%, #E6FFED 100%); }
.action-btn-4:hover { box-shadow: 0 4px 12px rgba(81, 207, 102, 0.2); }

.action-icon {
  font-size: 28px;
}

.action-text {
  font-size: 13px;
  color: #2D3436;
  font-weight: 500;
}

.card-title {
  font-weight: 600;
  color: #2D3436;
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
