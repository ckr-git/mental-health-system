<template>
  <div class="mood-forecast">
    <div class="forecast-header">
      <h3>🔮 心情展望</h3>
      <span class="forecast-subtitle">基于过去7天的趋势分析</span>
    </div>

    <div class="forecast-content" v-if="!loading && forecastData">
      <!-- 天气图标展示 -->
      <div class="forecast-weather">
        <div class="weather-icon-large">{{ forecastData.weatherIcon }}</div>
        <div class="weather-name">{{ forecastData.weatherName }}</div>
      </div>

      <!-- 趋势指示 -->
      <div class="forecast-trend">
        <div class="trend-indicator" :class="`trend-${forecastData.trend}`">
          <span class="trend-arrow">{{ forecastData.trendIcon }}</span>
          <span class="trend-text">{{ forecastData.trendText }}</span>
        </div>
        <div class="trend-score">
          预测心情指数: <span class="score-value">{{ forecastData.predictedScore }}/10</span>
        </div>
      </div>

      <!-- 详细分析 -->
      <div class="forecast-analysis">
        <div class="analysis-item">
          <span class="label">近期平均:</span>
          <span class="value">{{ forecastData.recentAvg }}/10</span>
        </div>
        <div class="analysis-item">
          <span class="label">之前平均:</span>
          <span class="value">{{ forecastData.previousAvg }}/10</span>
        </div>
        <div class="analysis-item">
          <span class="label">变化幅度:</span>
          <span class="value" :class="changeClass">{{ forecastData.changeText }}</span>
        </div>
      </div>

      <!-- 建议提示 -->
      <div class="forecast-suggestion">
        <el-icon><Sunny /></el-icon>
        <span>{{ forecastData.suggestion }}</span>
      </div>
    </div>

    <div class="forecast-loading" v-else-if="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在分析中...</span>
    </div>

    <div class="forecast-empty" v-else>
      <el-icon><Warning /></el-icon>
      <span>数据不足，至少需要4天的情绪日记</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { symptomApi } from '@/api'
import { Sunny, Loading, Warning } from '@element-plus/icons-vue'
import type { SymptomRecord } from '@/types'

interface ForecastData {
  weatherIcon: string
  weatherName: string
  trend: 'up' | 'stable' | 'down'
  trendIcon: string
  trendText: string
  predictedScore: number
  recentAvg: number
  previousAvg: number
  changeText: string
  suggestion: string
}

const loading = ref(true)
const forecastData = ref<ForecastData | null>(null)

// 根据心情分数映射天气类型
const getWeatherByScore = (score: number): { icon: string; name: string } => {
  if (score >= 8) return { icon: '✨', name: '晴空万里' }
  if (score >= 6) return { icon: '☀️', name: '晴朗' }
  if (score >= 4) return { icon: '☁️', name: '多云' }
  if (score >= 2) return { icon: '🌧️', name: '阴雨' }
  return { icon: '⛈️', name: '暴风雨' }
}

// 计算趋势
const getTrend = (change: number): { trend: 'up' | 'stable' | 'down'; icon: string; text: string } => {
  if (change > 0.5) return { trend: 'up', icon: '↗️', text: '上升趋势' }
  if (change < -0.5) return { trend: 'down', icon: '↘️', text: '下降趋势' }
  return { trend: 'stable', icon: '→', text: '平稳态势' }
}

// 获取建议
const getSuggestion = (trend: string, score: number): string => {
  if (trend === 'up') {
    return '继续保持，你的心情正在变好！'
  } else if (trend === 'down') {
    if (score < 4) {
      return '建议寻求专业帮助，你不是一个人在战斗。'
    }
    return '注意调节情绪，适当休息和放松。'
  } else {
    if (score >= 6) {
      return '保持当前的生活节奏，心情不错！'
    }
    return '尝试一些积极的活动来改善心情。'
  }
}

// 计算预测数据
const calculateForecast = async () => {
  loading.value = true
  try {
    // 获取最近7天的数据
    const { data } = await symptomApi.getRecent(7)
    const diaries = data || []

    if (diaries.length < 4) {
      forecastData.value = null
      return
    }

    // 按时间排序(从旧到新)
    const sortedDiaries = [...diaries].sort((a, b) =>
      new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
    )

    // 计算最近4天和之前的平均值（如果数据足够）
    const halfPoint = Math.floor(sortedDiaries.length / 2)
    const recentDiaries = sortedDiaries.slice(halfPoint)
    const previousDiaries = sortedDiaries.slice(0, halfPoint)

    const recentAvg = recentDiaries.reduce((sum, d) => sum + d.moodScore, 0) / recentDiaries.length
    const previousAvg = previousDiaries.length > 0
      ? previousDiaries.reduce((sum, d) => sum + d.moodScore, 0) / previousDiaries.length
      : recentAvg

    // 计算变化
    const change = recentAvg - previousAvg
    const changePercent = previousAvg > 0 ? (change / previousAvg) * 100 : 0

    // 简单线性预测：预测值 = 近期平均 + 变化趋势
    const predictedScore = Math.max(1, Math.min(10, Math.round((recentAvg + change * 0.5) * 10) / 10))

    // 获取天气和趋势
    const weather = getWeatherByScore(predictedScore)
    const trendData = getTrend(change)

    forecastData.value = {
      weatherIcon: weather.icon,
      weatherName: weather.name,
      trend: trendData.trend,
      trendIcon: trendData.icon,
      trendText: trendData.text,
      predictedScore: Math.round(predictedScore * 10) / 10,
      recentAvg: Math.round(recentAvg * 10) / 10,
      previousAvg: Math.round(previousAvg * 10) / 10,
      changeText: change >= 0
        ? `+${Math.abs(Math.round(change * 10) / 10)} (${changePercent.toFixed(1)}%)`
        : `-${Math.abs(Math.round(change * 10) / 10)} (${changePercent.toFixed(1)}%)`,
      suggestion: getSuggestion(trendData.trend, predictedScore)
    }
  } catch (error) {
    console.error('Failed to calculate forecast:', error)
    forecastData.value = null
  } finally {
    loading.value = false
  }
}

const changeClass = computed(() => {
  if (!forecastData.value) return ''
  const change = forecastData.value.recentAvg - forecastData.value.previousAvg
  if (change > 0) return 'positive'
  if (change < 0) return 'negative'
  return 'neutral'
})

onMounted(() => {
  calculateForecast()
})
</script>

<style scoped lang="scss">
.mood-forecast {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 24px;
  color: white;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
}

.forecast-header {
  text-align: center;
  margin-bottom: 24px;

  h3 {
    font-size: 22px;
    margin: 0 0 8px 0;
    font-weight: 600;
  }

  .forecast-subtitle {
    font-size: 13px;
    opacity: 0.9;
  }
}

.forecast-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.forecast-weather {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);

  .weather-icon-large {
    font-size: 80px;
    line-height: 1;
    margin-bottom: 12px;
    animation: float 3s ease-in-out infinite;
  }

  .weather-name {
    font-size: 20px;
    font-weight: 500;
  }
}

.forecast-trend {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;

  .trend-indicator {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 18px;
    font-weight: 500;

    .trend-arrow {
      font-size: 28px;
    }

    &.trend-up {
      .trend-arrow {
        color: #4ade80;
      }
    }

    &.trend-down {
      .trend-arrow {
        color: #f87171;
      }
    }

    &.trend-stable {
      .trend-arrow {
        color: #fbbf24;
      }
    }
  }

  .trend-score {
    font-size: 16px;

    .score-value {
      font-size: 22px;
      font-weight: 600;
      margin-left: 8px;
    }
  }
}

.forecast-analysis {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;

  .analysis-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;

    .label {
      opacity: 0.9;
    }

    .value {
      font-weight: 600;
      font-size: 15px;

      &.positive {
        color: #4ade80;
      }

      &.negative {
        color: #f87171;
      }

      &.neutral {
        color: #fbbf24;
      }
    }
  }
}

.forecast-suggestion {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;

  .el-icon {
    font-size: 20px;
    flex-shrink: 0;
  }
}

.forecast-loading,
.forecast-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
  text-align: center;

  .el-icon {
    font-size: 48px;
  }

  span {
    font-size: 15px;
    opacity: 0.9;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}
</style>
