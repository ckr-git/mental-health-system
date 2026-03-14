<template>
  <div class="statistics-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>User Growth Trend</template>
          <div id="userChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>Resource Views</template>
          <div id="resourceChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>Consultation Statistics</template>
          <div id="consultationChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>System Activity</template>
          <div id="activityChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'

onMounted(() => {
  ElMessage.info('Advanced statistics feature coming soon')
})
</script>

<style scoped>
.statistics-container {
  padding: 20px;
}
</style>
