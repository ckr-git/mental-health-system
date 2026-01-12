<template>
  <div class="time-capsule-page">
    <!-- 天气背景 -->
    <WeatherBackground :weather-type="currentWeather" :light-mode="lightMode" />
    
    <!-- 灯光开关 -->
    <LightSwitch :light-mode="lightMode" @toggle="toggleLight" />

    <!-- 返回首页按钮 -->
    <el-button
      class="back-home-btn"
      type="info"
      circle
      @click="$router.push('/patient/dashboard')"
      title="返回首页"
    >
      <el-icon><HomeFilled /></el-icon>
    </el-button>
    
    <!-- 主内容区 -->
    <div class="content-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">📮 时光信箱</h1>
        <p class="page-subtitle">给未来的自己寄一封信</p>
      </div>
      
      <!-- 快捷操作栏 -->
      <div class="action-bar">
        <el-button type="primary" size="large" @click="showEditorDialog = true" class="write-btn">
          ✏️ 写信给未来
        </el-button>
        
        <el-badge :value="unlockableCount" :hidden="unlockableCount === 0" class="unlock-badge">
          <el-button size="large" @click="showUnlockable" class="unlock-btn">
            🔓 可解锁信件
          </el-button>
        </el-badge>
      </div>
      
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon">📬</div>
          <div class="stat-content">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">全部信件</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">⏳</div>
          <div class="stat-content">
            <div class="stat-value">{{ lockedCount }}</div>
            <div class="stat-label">等待解锁</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📭</div>
          <div class="stat-content">
            <div class="stat-value">{{ unlockedCount }}</div>
            <div class="stat-label">已解锁</div>
          </div>
        </div>
      </div>
      
      <!-- 信件列表 -->
      <div class="capsule-list-container" v-loading="loading">
        <TimeCapsuleList
          :capsules="capsules"
          :loading="loading"
          :total="total"
          @create="showEditorDialog = true"
          @view="handleViewCapsule"
          @reply="handleReplyCapsule"
          @page-change="handlePageChange"
        />
      </div>
    </div>
    
    <!-- 写信对话框 -->
    <TimeCapsuleEditor
      v-model:visible="showEditorDialog"
      @success="handleCreateSuccess"
    />

    <!-- 回复对话框 -->
    <TimeCapsuleReplyDialog
      v-model:visible="showReplyDialog"
      :letter="currentLetter"
      @success="handleReplySuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { HomeFilled } from '@element-plus/icons-vue'
import WeatherBackground from '@/components/MoodDiary/WeatherBackground.vue'
import LightSwitch from '@/components/MoodDiary/LightSwitch.vue'
import TimeCapsuleList from '@/components/MoodDiary/TimeCapsuleList.vue'
import TimeCapsuleEditor from '@/components/MoodDiary/TimeCapsuleEditor.vue'
import TimeCapsuleReplyDialog from '@/components/MoodDiary/TimeCapsuleReplyDialog.vue'
import { capsuleApi } from '@/api'
import { playSound } from '@/utils/soundService'

// 灯光模式
const lightMode = ref<'day' | 'night'>('day')

// 当前天气（默认晴朗）
const currentWeather = ref('sunny')

// 信件列表
const capsules = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

// 写信对话框
const showEditorDialog = ref(false)

// 回复对话框
const showReplyDialog = ref(false)
const currentLetter = ref<any>(null)

// 可解锁信件数量
const unlockableCount = ref(0)

// 统计数量
const totalCount = computed(() => capsules.value.length)
const lockedCount = computed(() => capsules.value.filter(c => !c.isUnlocked).length)
const unlockedCount = computed(() => capsules.value.filter(c => c.isUnlocked).length)

// 切换灯光
const toggleLight = async () => {
  lightMode.value = lightMode.value === 'day' ? 'night' : 'day'
  const message = lightMode.value === 'day' ? '开灯啦 💡' : '关灯啦 🌙'
  ElMessage.success(message)
}

// 加载信件列表
const loadCapsules = async () => {
  try {
    loading.value = true
    const res = await capsuleApi.getList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })

    if (res.code === 200 && res.data) {
      // 将后端数据转换为前端格式
      const backendData = res.data
      if (Array.isArray(backendData) && backendData.length > 0) {
        capsules.value = backendData.map(item => ({
          id: item.id,
          letterType: item.letterType,
          title: item.title,
          content: item.content,
          unlockType: item.unlockType || 'days',
          unlockTime: item.unlockDate,
          unlockConditions: item.unlockConditions ? JSON.parse(item.unlockConditions) : [],
          isUnlocked: item.status !== 'sealed',
          replyContent: item.replyContent,
          replyTime: item.replyTime,
          createTime: item.createTime || item.writeDate
        }))
        total.value = capsules.value.length
      } else {
        // 如果后端返回空数据，使用模拟数据
        capsules.value = generateMockData()
        total.value = capsules.value.length
      }
    }
  } catch (error) {
    console.error('Failed to load capsules:', error)
    // 使用模拟数据
    capsules.value = generateMockData()
    total.value = capsules.value.length
  } finally {
    loading.value = false
  }
}

// 加载可解锁信件数量
const loadUnlockableCount = async () => {
  try {
    const res = await capsuleApi.getUnlockable()
    if (res.code === 200 && res.data) {
      unlockableCount.value = res.data.length
    }
  } catch (error) {
    console.error('Failed to load unlockable count:', error)
    unlockableCount.value = 0
  }
}

// 生成模拟数据
const generateMockData = () => {
  const now = new Date()
  return [
    {
      id: 1,
      letterType: 'praise',
      title: '给坚持的自己',
      content: '今天的我完成了一个重要的目标，虽然过程很艰辛，但我做到了！未来的你一定要记住这份成就感。',
      unlockType: 'days',
      unlockTime: new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      unlockConditions: [],
      isUnlocked: false,
      createTime: now.toISOString()
    },
    {
      id: 2,
      letterType: 'hope',
      title: '30天后的期待',
      content: '希望30天后的你，已经养成了早起的习惯，学会了一门新技能，交到了新朋友。',
      unlockType: 'days',
      unlockTime: new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString(),
      unlockConditions: ['days_30'],
      isUnlocked: false,
      createTime: new Date(now.getTime() - 5 * 24 * 60 * 60 * 1000).toISOString()
    },
    {
      id: 3,
      letterType: 'thanks',
      title: '谢谢你的坚持',
      content: '现在的我虽然很难，但我相信你已经走出来了。谢谢你的坚持，谢谢你没有放弃。',
      unlockType: 'condition',
      unlockTime: new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      unlockConditions: ['mood_low'],
      isUnlocked: true,
      unlockTime: new Date(now.getTime() - 1 * 24 * 60 * 60 * 1000).toISOString(),
      createTime: new Date(now.getTime() - 35 * 24 * 60 * 60 * 1000).toISOString()
    }
  ]
}

// 查看信件
const handleViewCapsule = (capsule: any) => {
  console.log('View capsule:', capsule)
}

// 回复信件
const handleReplyCapsule = (capsule: any) => {
  currentLetter.value = capsule
  showReplyDialog.value = true
}

// 回复成功
const handleReplySuccess = () => {
  loadCapsules()
  ElMessage.success('💌 回复已保存，你与过去的自己完成了一次对话！')
}

// 创建成功
const handleCreateSuccess = () => {
  playSound('send-letter')
  loadCapsules()
}

// 页码变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadCapsules()
}

// 显示可解锁信件
const showUnlockable = async () => {
  try {
    const res = await capsuleApi.getUnlockable()
    if (res.code === 200 && res.data && res.data.length > 0) {
      playSound('open-letter')
      ElMessage.success(`发现 ${res.data.length} 封可解锁的信件！`)
      // TODO: 显示可解锁信件列表
    } else {
      ElMessage.info('暂时没有可解锁的信件')
    }
  } catch (error) {
    ElMessage.info('暂时没有可解锁的信件')
  }
}

onMounted(() => {
  loadCapsules()
  loadUnlockableCount()
})
</script>

<style scoped>
.time-capsule-page {
  position: relative;
  min-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 自定义滚动条样式 */
.time-capsule-page::-webkit-scrollbar {
  width: 10px;
}

.time-capsule-page::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
}

.time-capsule-page::-webkit-scrollbar-thumb {
  background: rgba(250, 173, 20, 0.4);
  border-radius: 10px;
  transition: background 0.3s;
}

.time-capsule-page::-webkit-scrollbar-thumb:hover {
  background: rgba(250, 173, 20, 0.6);
}

/* 返回首页按钮 */
.back-home-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  width: 48px;
  height: 48px;
  font-size: 24px;
}

.back-home-btn:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 1);
}

.content-container {
  position: relative;
  z-index: 1;
  max-width: 1400px;
  margin: 0 auto;
  padding: 100px 20px 40px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.page-title {
  font-size: 40px;
  font-weight: 700;
  margin: 0 0 12px 0;
}

.page-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 40px;
}

.write-btn,
.unlock-btn {
  font-size: 16px;
  padding: 14px 36px;
  border-radius: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.unlock-badge {
  :deep(.el-badge__content) {
    background: #ff4d4f;
    border: 2px solid white;
  }
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-icon {
  font-size: 48px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 信件列表容器 */
.capsule-list-container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-title {
    font-size: 32px;
  }
  
  .page-subtitle {
    font-size: 16px;
  }
  
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .capsule-list-container {
    padding: 20px;
  }
}
</style>
