<template>
  <div class="room-decoration-page">
    <!-- 背景装饰 -->
    <div class="background-decoration">
      <div class="decoration-pattern"></div>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <el-button
        class="back-btn"
        type="info"
        circle
        @click="$router.back()"
        title="返回"
      >
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="page-title">🏠 装饰我的房间</h1>
      <p class="page-subtitle">收集装饰，打造专属空间</p>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 装饰画布容器 -->
      <div class="canvas-container">
        <RoomCanvas v-if="isReady" ref="roomCanvasRef" />
        <div v-else class="loading-container">
          <el-icon class="is-loading" :size="40"><Loading /></el-icon>
          <p>正在加载装饰系统...</p>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="tips-section">
        <el-alert
          title="温馨提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <ul class="tips-list">
              <li>💎 通过写日记、发送时光信等行为解锁新装饰</li>
              <li>🎁 点击右下角礼物图标打开装饰商店</li>
              <li>🛠️ 点击工具栏选择工具：手套可拖动装饰位置，其他工具用于与装饰互动</li>
              <li>✨ 不同装饰需要对应的工具才能互动（浇水壶💧→植物，打火机🔥→蜡烛等）</li>
              <li>🎮 互动时会触发炫酷的粒子特效和动画</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import RoomCanvas from '@/components/MoodDiary/RoomCanvas.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const roomCanvasRef = ref<InstanceType<typeof RoomCanvas> | null>(null)
const isReady = ref(false)

// 检查登录状态
onMounted(async () => {
  // 检查token是否存在
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 延迟100ms确保路由守卫和store初始化完成
  await new Promise(resolve => setTimeout(resolve, 100))

  // 再次检查token(防止在此期间被清除)
  if (userStore.token) {
    isReady.value = true
  } else {
    ElMessage.warning('登录已过期,请重新登录')
    router.push('/login')
  }
})
</script>

<style scoped>
.room-decoration-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
}

/* 背景装饰 */
.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.1;
  pointer-events: none;
}

.decoration-pattern {
  width: 100%;
  height: 100%;
  background-image:
    repeating-linear-gradient(45deg, transparent, transparent 35px, rgba(255,255,255,.1) 35px, rgba(255,255,255,.1) 70px);
}

/* 页面头部 */
.page-header {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 40px 20px 30px;
  color: white;
}

.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  width: 48px;
  height: 48px;
  font-size: 24px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: none;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

/* 主内容区 */
.content-wrapper {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 40px;
}

/* 画布容器 */
.canvas-container {
  position: relative;
  width: 100%;
  height: 600px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* 提示信息 */
.tips-section {
  max-width: 800px;
  margin: 0 auto;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
}

.tips-list li {
  margin: 8px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-title {
    font-size: 28px;
  }

  .canvas-container {
    height: 400px;
  }
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 16px;
}

.loading-container p {
  margin: 0;
  font-size: 14px;
}
</style>
