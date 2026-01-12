<template>
  <div class="space-container">
    <el-card class="header-card">
      <h2>🎨 心灵空间</h2>
      <p style="color: #909399; margin-top: 10px;">在这里，记录心情、倾诉烦恼、装点属于你的温馨小屋</p>
    </el-card>

    <el-card class="content-card">
      <el-tabs v-model="activeTab" type="border-card" class="space-tabs">
        <el-tab-pane label="📮 时光信箱" name="capsule">
          <TimeCapsule />
        </el-tab-pane>
        <el-tab-pane label="🌳 心情树洞" name="treehole">
          <TreeHole />
        </el-tab-pane>
        <el-tab-pane label="🏠 房间装饰" name="room">
          <RoomDecoration />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import TimeCapsule from './TimeCapsule.vue'
import TreeHole from './TreeHole.vue'
import RoomDecoration from './RoomDecoration.vue'

const activeTab = ref('capsule')
</script>

<style scoped>
.space-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.content-card {
  min-height: calc(100vh - 240px);
}

.space-tabs {
  border: none;
  box-shadow: none;
}

:deep(.el-tabs__header) {
  margin: 0 0 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 10px;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 30px;
  height: 50px;
  line-height: 50px;
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>
