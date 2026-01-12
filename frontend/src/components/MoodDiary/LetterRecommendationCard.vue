<template>
  <div class="letter-recommendation-card" v-if="recommendation">
    <div class="card-header">
      <div class="header-icon">💡</div>
      <div class="header-content">
        <h3 class="card-title">智能推荐</h3>
        <p class="card-subtitle">{{ recommendation.reason }}</p>
      </div>
    </div>

    <div class="card-body">
      <!-- 心情趋势 -->
      <div class="mood-info">
        <div class="info-item">
          <span class="label">近期心情:</span>
          <span class="value">
            <el-tag :type="getMoodTagType(recommendation.avgMood)" size="small">
              {{ recommendation.avgMood?.toFixed(1) }} 分
            </el-tag>
          </span>
        </div>
        <div class="info-item">
          <span class="label">心情趋势:</span>
          <span class="value">
            <el-tag :type="getTrendTagType(recommendation.moodTrend)" size="small">
              {{ getTrendText(recommendation.moodTrend) }} {{ getTrendIcon(recommendation.moodTrend) }}
            </el-tag>
          </span>
        </div>
      </div>

      <!-- 推荐信件类型 -->
      <div class="recommended-type">
        <div class="type-header">
          <span class="type-icon">{{ getTypeIcon(recommendation.recommendType) }}</span>
          <div class="type-content">
            <h4 class="type-title">{{ recommendation.typeTitle }}</h4>
            <p class="type-desc">{{ recommendation.typeDescription }}</p>
          </div>
        </div>

        <!-- 模板预览 -->
        <div class="template-preview">
          <div class="preview-label">📝 推荐模板:</div>
          <div class="preview-content">{{ getTemplatePreview(recommendation.template) }}</div>
        </div>
      </div>
    </div>

    <div class="card-footer">
      <el-button @click="emit('close')">稍后再说</el-button>
      <el-button type="primary" @click="handleApply">
        使用推荐模板
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// Props
interface LetterRecommendation {
  recommendType: string
  reason: string
  moodTrend: string
  avgMood: number
  template: string
  typeTitle: string
  typeDescription: string
}

interface Props {
  recommendation: LetterRecommendation | null
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  (e: 'apply', recommendation: LetterRecommendation): void
  (e: 'close'): void
}>()

// 获取心情标签类型
const getMoodTagType = (mood: number) => {
  if (mood < 4) return 'danger'
  if (mood < 7) return 'warning'
  return 'success'
}

// 获取趋势标签类型
const getTrendTagType = (trend: string) => {
  if (trend === 'up') return 'success'
  if (trend === 'down') return 'danger'
  return 'info'
}

// 获取趋势文本
const getTrendText = (trend: string) => {
  const map: Record<string, string> = {
    up: '上升',
    down: '下降',
    stable: '平稳'
  }
  return map[trend] || '平稳'
}

// 获取趋势图标
const getTrendIcon = (trend: string) => {
  const map: Record<string, string> = {
    up: '📈',
    down: '📉',
    stable: '📊'
  }
  return map[trend] || '📊'
}

// 获取信件类型图标
const getTypeIcon = (type: string) => {
  const map: Record<string, string> = {
    praise: '🎉',
    hope: '💌',
    thanks: '🙏',
    goal: '🎯'
  }
  return map[type] || '✉️'
}

// 获取模板预览
const getTemplatePreview = (template: string) => {
  if (!template) return ''
  const lines = template.split('\n').slice(0, 3)
  return lines.join('\n') + (template.split('\n').length > 3 ? '\n...' : '')
}

// 应用推荐
const handleApply = () => {
  if (props.recommendation) {
    emit('apply', props.recommendation)
  }
}
</script>

<style scoped lang="scss">
.letter-recommendation-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.25);

  .card-header {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 20px;

    .header-icon {
      font-size: 40px;
      line-height: 1;
    }

    .header-content {
      flex: 1;

      .card-title {
        font-size: 20px;
        font-weight: 600;
        margin: 0 0 8px 0;
        color: white;
      }

      .card-subtitle {
        font-size: 14px;
        margin: 0;
        opacity: 0.9;
        line-height: 1.5;
      }
    }
  }

  .card-body {
    .mood-info {
      display: flex;
      gap: 20px;
      margin-bottom: 20px;
      padding: 16px;
      background: rgba(255, 255, 255, 0.1);
      backdrop-filter: blur(10px);
      border-radius: 12px;

      .info-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .label {
          font-size: 14px;
          opacity: 0.9;
        }

        .value {
          font-weight: 500;
        }
      }
    }

    .recommended-type {
      background: rgba(255, 255, 255, 0.15);
      backdrop-filter: blur(10px);
      border-radius: 12px;
      padding: 16px;

      .type-header {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        margin-bottom: 16px;

        .type-icon {
          font-size: 32px;
          line-height: 1;
        }

        .type-content {
          flex: 1;

          .type-title {
            font-size: 16px;
            font-weight: 600;
            margin: 0 0 6px 0;
            color: white;
          }

          .type-desc {
            font-size: 13px;
            margin: 0;
            opacity: 0.9;
          }
        }
      }

      .template-preview {
        .preview-label {
          font-size: 13px;
          font-weight: 500;
          margin-bottom: 8px;
          opacity: 0.9;
        }

        .preview-content {
          background: rgba(0, 0, 0, 0.15);
          padding: 12px;
          border-radius: 8px;
          font-size: 13px;
          line-height: 1.6;
          white-space: pre-wrap;
          font-family: inherit;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;

    :deep(.el-button) {
      &.el-button--default {
        background: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;

        &:hover {
          background: rgba(255, 255, 255, 0.3);
        }
      }

      &.el-button--primary {
        background: white;
        border: none;
        color: #667eea;
        font-weight: 600;

        &:hover {
          background: rgba(255, 255, 255, 0.9);
        }
      }
    }
  }
}
</style>
