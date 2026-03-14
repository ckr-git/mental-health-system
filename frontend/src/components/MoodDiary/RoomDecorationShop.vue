<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="handleVisibleChange"
    title="装饰房间"
    direction="rtl"
    size="400px"
    @close="handleClose"
  >
    <div class="shop-content">
      <!-- 解锁进度 -->
      <div class="unlock-progress">
        <div class="progress-header">
          <span class="progress-title">解锁进度</span>
          <el-button
            type="primary"
            size="small"
            @click="$emit('check-unlock')"
          >
            🔓 检查解锁
          </el-button>
        </div>
        <el-progress
          :percentage="unlockPercentage"
          :color="progressColor"
          :stroke-width="12"
        />
        <p class="progress-text">
          已解锁 {{ unlockedCount }} / {{ totalCount }} 个装饰
        </p>
      </div>

      <!-- 分类标签 -->
      <el-tabs v-model="activeTab" class="decoration-tabs">
        <el-tab-pane label="全部" name="all">
          <DecorationList
            :items="filteredDecorations"
            :selected="selectedDecoration"
            @select="handleSelect"
          />
        </el-tab-pane>
        <el-tab-pane label="植物" name="plant">
          <DecorationList
            :items="getCategoryItems('plant')"
            :selected="selectedDecoration"
            @select="handleSelect"
          />
        </el-tab-pane>
        <el-tab-pane label="家具" name="furniture">
          <DecorationList
            :items="getCategoryItems('furniture')"
            :selected="selectedDecoration"
            @select="handleSelect"
          />
        </el-tab-pane>
        <el-tab-pane label="装饰品" name="decoration">
          <DecorationList
            :items="getCategoryItems('decoration')"
            :selected="selectedDecoration"
            @select="handleSelect"
          />
        </el-tab-pane>
        <el-tab-pane label="特殊" name="special">
          <DecorationList
            :items="getCategoryItems('special')"
            :selected="selectedDecoration"
            @select="handleSelect"
          />
        </el-tab-pane>
      </el-tabs>

      <!-- 选中装饰的详情 -->
      <div v-if="selectedDecoration" class="selected-detail">
        <el-divider />
        <div class="detail-header">
          <span class="detail-icon">{{ selectedConfig?.decorationIcon }}</span>
          <div class="detail-info">
            <h3 class="detail-name">{{ selectedConfig?.decorationName }}</h3>
            <p class="detail-category">{{ selectedConfig?.category }}</p>
          </div>
        </div>

        <div v-if="selectedConfig?.unlockCondition" class="unlock-condition">
          <p class="condition-label">解锁条件：</p>
          <p class="condition-text">{{ selectedConfig.unlockCondition }}</p>
        </div>

        <div v-if="selectedConfig?.canInteract" class="interaction-info">
          <el-tag type="success" size="small">可互动</el-tag>
          <p class="interaction-text">{{ selectedConfig.interactionEffect }}</p>
        </div>

        <!-- 放置按钮 -->
        <el-button
          v-if="isUnlocked(selectedDecoration)"
          type="primary"
          size="large"
          class="place-btn"
          :disabled="isPlaced(selectedDecoration)"
          @click="handlePlace"
        >
          {{ isPlaced(selectedDecoration) ? '已放置' : '放置到房间' }}
        </el-button>
        <el-button
          v-else
          type="info"
          size="large"
          class="place-btn"
          disabled
        >
          🔒 未解锁
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import DecorationList from './DecorationList.vue'

interface Props {
  visible: boolean
  decorations: any[]
  configs: any[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'add', decorationType: string, position: { x: number; y: number }): void
  (e: 'check-unlock'): void
}>()

const activeTab = ref('all')
const selectedDecoration = ref<any>(null)

// 合并装饰数据（用户实例 + 配置）
const mergedDecorations = computed(() => {
  return props.configs.map(config => {
    const userDecoration = props.decorations.find(
      d => d.decorationType === config.decorationType
    )

    // 如果找到用户装饰，合并；否则使用config并设置默认解锁状态
    if (userDecoration) {
      return {
        ...config,
        ...userDecoration,
        configId: config.id,
        userDecorationId: userDecoration?.id
      }
    } else {
      // 用户还没有这个装饰的记录，设置默认值
      return {
        ...config,
        configId: config.id,
        userDecorationId: null,
        isUnlocked: 0,  // 默认未解锁
        isActive: 0      // 默认未放置
      }
    }
  })
})

// 解锁统计
const unlockedCount = computed(() => {
  return mergedDecorations.value.filter(d => d.isUnlocked === 1).length
})

const totalCount = computed(() => {
  return mergedDecorations.value.length
})

const unlockPercentage = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((unlockedCount.value / totalCount.value) * 100)
})

const progressColor = computed(() => {
  const percentage = unlockPercentage.value
  if (percentage < 30) return '#f56c6c'
  if (percentage < 70) return '#e6a23c'
  return '#67c23a'
})

// 过滤装饰（根据分类）
const filteredDecorations = computed(() => {
  return mergedDecorations.value
})

// 获取指定分类的装饰
const getCategoryItems = (category: string) => {
  return mergedDecorations.value.filter(d => d.category === category)
}

// 选中的装饰配置
const selectedConfig = computed(() => {
  if (!selectedDecoration.value) return null
  return props.configs.find(
    c => c.decorationType === selectedDecoration.value.decorationType
  )
})

// 是否已解锁
const isUnlocked = (decoration: any) => {
  return decoration && decoration.isUnlocked === 1
}

// 是否已放置
const isPlaced = (decoration: any) => {
  return decoration && decoration.isActive === 1
}

// 选择装饰
const handleSelect = (decoration: any) => {
  selectedDecoration.value = decoration
}

// 放置装饰
const handlePlace = () => {
  if (!selectedDecoration.value) return

  // 默认放置在中心位置（可以后续优化为点击位置）
  const position = {
    x: Math.round(50 + Math.random() * 10 - 5),
    y: Math.round(50 + Math.random() * 10 - 5)
  }

  emit('add', selectedDecoration.value.decorationType, position)
}

// 处理可见性变化
const handleVisibleChange = (value: boolean) => {
  emit('update:visible', value)
}

// 关闭抽屉
const handleClose = () => {
  emit('update:visible', false)
}

// 暴露内部状态
defineExpose({
  selectedDecoration
})
</script>

<style scoped>
.shop-content {
  padding: 0 4px;
}

/* 解锁进度 */
.unlock-progress {
  margin-bottom: 24px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-title {
  font-size: 16px;
  font-weight: 600;
}

.progress-text {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.9;
  text-align: center;
}

/* 分类标签 */
.decoration-tabs {
  margin-bottom: 20px;
}

/* 选中装饰详情 */
.selected-detail {
  margin-top: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-icon {
  font-size: 48px;
}

.detail-info {
  flex: 1;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}

.detail-category {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 解锁条件 */
.unlock-condition {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
}

.condition-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin: 0 0 4px 0;
}

.condition-text {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* 互动信息 */
.interaction-info {
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
  margin-bottom: 16px;
}

.interaction-text {
  font-size: 13px;
  color: #606266;
  margin: 8px 0 0 0;
}

/* 放置按钮 */
.place-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
