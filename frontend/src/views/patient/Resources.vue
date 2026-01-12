<template>
  <div class="resources-container">
    <el-card class="header-card">
      <h2>心理健康资源库</h2>
      <p style="color: #909399; margin-top: 10px">功能开发中，敬请期待...</p>
    </el-card>

    <el-empty description="暂无资源" style="margin-top: 100px">
      <template #image>
        <el-icon :size="80" color="#909399">
          <Document />
        </el-icon>
      </template>
      <template #description>
        <div style="margin-top: 20px">
          <p style="font-size: 16px; color: #606266">心理健康资源正在筹备中</p>
          <p style="font-size: 14px; color: #909399; margin-top: 10px">
            将为您提供优质的文章、视频、音频和课程资源
          </p>
        </div>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

// TODO: 待实现心理资源功能
</script>

<style scoped lang="scss">
.resources-container {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;
    text-align: center;

    h2 {
      margin: 0;
      color: #303133;
    }
  }
}
</style>
