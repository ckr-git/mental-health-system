<template>
  <div class="decoration-list">
    <el-empty
      v-if="items.length === 0"
      description="暂无装饰"
      :image-size="80"
    />

    <div v-else class="decoration-grid">
      <div
        v-for="item in items"
        :key="item.decorationType"
        class="decoration-card"
        :class="{
          'selected': selected?.decorationType === item.decorationType,
          'unlocked': item.isUnlocked === 1,
          'locked': item.isUnlocked !== 1,
          'placed': item.isActive === 1
        }"
        @click="$emit('select', item)"
      >
        <!-- 图标 -->
        <div class="card-icon">
          {{ item.decorationIcon || '🎨' }}
        </div>

        <!-- 名称 -->
        <div class="card-name">
          {{ item.decorationName }}
        </div>

        <!-- 状态标签 -->
        <div class="card-status">
          <el-tag
            v-if="item.isActive === 1"
            type="success"
            size="small"
            effect="dark"
          >
            已放置
          </el-tag>
          <el-tag
            v-else-if="item.isUnlocked === 1"
            type="primary"
            size="small"
          >
            已解锁
          </el-tag>
          <el-tag
            v-else
            type="info"
            size="small"
          >
            🔒 未解锁
          </el-tag>
        </div>

        <!-- 互动标记 -->
        <div v-if="item.canInteract" class="interact-badge">
          <el-icon><Star /></el-icon>
        </div>

        <!-- 推荐标记 -->
        <div v-if="item.isRecommended" class="recommend-badge">
          推荐
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Star } from '@element-plus/icons-vue'

interface Props {
  items: any[]
  selected?: any
}

defineProps<Props>()

defineEmits<{
  (e: 'select', item: any): void
}>()
</script>

<style scoped>
.decoration-list {
  min-height: 200px;
}

.decoration-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.decoration-card {
  position: relative;
  padding: 16px 12px;
  background: white;
  border: 2px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.decoration-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.decoration-card.selected {
  border-color: #409eff;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3f9ff 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.decoration-card.locked {
  opacity: 0.6;
  background: #f5f7fa;
}

.decoration-card.locked .card-icon {
  filter: grayscale(1);
}

.decoration-card.placed {
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8f4 100%);
  border-color: #67c23a;
}

/* 图标 */
.card-icon {
  font-size: 40px;
  line-height: 1;
  transition: transform 0.3s ease;
}

.decoration-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

/* 名称 */
.card-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  word-break: break-all;
}

/* 状态标签 */
.card-status {
  margin-top: auto;
}

/* 互动标记 */
.interact-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 推荐标记 */
.recommend-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}
</style>
