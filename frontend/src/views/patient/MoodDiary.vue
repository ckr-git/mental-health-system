<template>
  <div class="mood-diary-page">
    <!-- 天气背景 -->
    <WeatherBackground :weather-type="currentWeather" :light-mode="lightMode" />

    <!-- 灯光开关 -->
    <LightSwitch :light-mode="lightMode" @toggle="toggleLight" />

    <!-- 房间装饰画布 - 条件渲染,仅登录时显示 -->
    <!-- 暂时禁用，等待稳定方案 -->
    <!-- <RoomCanvas v-if="userStore.token && isPageReady" /> -->

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
        <h1 class="page-title">我的情绪日记</h1>
        <p class="page-subtitle">记录每一天的心情，看见情绪的天气</p>
      </div>

      <!-- 心情预测 -->
      <div class="forecast-section">
        <MoodForecast />
      </div>

      <!-- 添加日记按钮 -->
      <div class="action-bar">
        <el-button type="primary" size="large" @click="showAddDialog = true" class="add-btn">
          ✏️ 写今天的日记
        </el-button>
        <el-button type="success" size="large" @click="$router.push('/patient/room-decoration')" class="room-btn">
          🏠 我的心灵小屋
        </el-button>
      </div>
      
      <!-- 日记列表 -->
      <div class="diary-list" v-loading="loading">
        <el-empty v-if="diaries.length === 0 && !loading" description="还没有日记，开始记录吧">
          <el-button type="primary" @click="showAddDialog = true">写第一篇日记</el-button>
        </el-empty>
        
        <div class="diary-grid" v-else>
          <DiaryCard 
            v-for="diary in diaries" 
            :key="diary.id" 
            :diary="diary"
            @click="viewDiary"
          />
        </div>
        
        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadDiaries"
          />
        </div>
      </div>
    </div>
    
    <!-- 添加日记对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="写日记"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="newDiary" label-position="top">
        <!-- 心情评分 -->
        <el-form-item label="今天心情">
          <div class="mood-selector">
            <div class="mood-emojis">
              <span 
                v-for="(emoji, score) in moodEmojis" 
                :key="score"
                class="emoji-option"
                :class="{ active: newDiary.moodScore === score }"
                @click="selectMood(score, emoji)"
              >
                {{ emoji }}
              </span>
            </div>
            <el-slider 
              v-model="newDiary.moodScore" 
              :min="1" 
              :max="10" 
              :marks="{ 1: '😢', 5: '😐', 10: '😄' }"
              @change="updateMoodEmoji"
            />
          </div>
        </el-form-item>
        
        <!-- 标题 -->
        <el-form-item label="一句话概括">
          <el-input 
            v-model="newDiary.title" 
            placeholder="今天发生了什么..." 
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <!-- 内容 -->
        <el-form-item label="详细记录">
          <el-input 
            v-model="newDiary.content" 
            type="textarea" 
            :rows="6"
            placeholder="把想说的都写下来吧..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        
        <!-- 多维度评分 -->
        <el-form-item label="其他维度（选填）">
          <div class="multi-dimensions">
            <div class="dimension-item">
              <span>精力：</span>
              <el-rate v-model="newDiary.energyLevel" :max="10" />
            </div>
            <div class="dimension-item">
              <span>睡眠：</span>
              <el-rate v-model="newDiary.sleepQuality" :max="10" />
            </div>
            <div class="dimension-item">
              <span>压力：</span>
              <el-rate v-model="newDiary.stressLevel" :max="10" />
            </div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddDiary" :loading="submitting">
          保存日记
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 日记详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="currentDiary?.title || '日记详情'"
      width="900px"
      class="diary-detail-dialog"
    >
      <div class="diary-detail" v-if="currentDiary" v-loading="detailLoading">
        <!-- 日记头部 -->
        <div class="detail-header">
          <div class="header-left">
            <div class="mood-display">
              <span class="mood-emoji">{{ currentDiary.moodEmoji }}</span>
              <span class="mood-score">心情指数: {{ currentDiary.moodScore }}/10</span>
            </div>
            <div class="weather-tag">
              <span class="weather-icon">{{ getWeatherIcon(currentDiary.weatherType) }}</span>
              <span>{{ getWeatherLabel(currentDiary.weatherType) }}</span>
            </div>
          </div>
          <div class="header-right">
            <div class="status-controls">
              <span class="status-label">状态：</span>
              <el-select
                v-model="currentDiary.status"
                placeholder="选择状态"
                size="small"
                @change="handleStatusChange"
                style="width: 140px"
              >
                <el-option label="进行中" value="ongoing" />
                <el-option label="已好转" value="better" />
                <el-option label="完全度过" value="overcome" />
                <el-option label="我战胜了它" value="proud" />
              </el-select>
            </div>
          </div>
        </div>
        
        <!-- 日记内容 -->
        <div class="detail-content">
          <div class="content-section">
            <h3 class="section-title">📝 日记正文</h3>
            <p class="content-text">{{ currentDiary.content || '暂无内容' }}</p>
          </div>
          
          <!-- 多维度评分 -->
          <div class="dimensions-section" v-if="hasDimensions">
            <h3 class="section-title">📊 其他维度</h3>
            <div class="dimensions-grid">
              <div class="dimension-card" v-if="currentDiary.energyLevel">
                <span class="dimension-label">精力</span>
                <el-rate v-model="currentDiary.energyLevel" disabled :max="10" size="small" />
                <span class="dimension-value">{{ currentDiary.energyLevel }}/10</span>
              </div>
              <div class="dimension-card" v-if="currentDiary.sleepQuality">
                <span class="dimension-label">睡眠</span>
                <el-rate v-model="currentDiary.sleepQuality" disabled :max="10" size="small" />
                <span class="dimension-value">{{ currentDiary.sleepQuality }}/10</span>
              </div>
              <div class="dimension-card" v-if="currentDiary.stressLevel">
                <span class="dimension-label">压力</span>
                <el-rate v-model="currentDiary.stressLevel" disabled :max="10" size="small" />
                <span class="dimension-value">{{ currentDiary.stressLevel }}/10</span>
              </div>
            </div>
          </div>
          
          <!-- 记录时间 -->
          <div class="meta-info">
            <span class="meta-item">
              <i class="el-icon-time"></i>
              创建时间: {{ formatDateTime(currentDiary.createTime) }}
            </span>
            <span class="meta-item" v-if="currentDiary.updateTime !== currentDiary.createTime">
              <i class="el-icon-edit"></i>
              更新时间: {{ formatDateTime(currentDiary.updateTime) }}
            </span>
          </div>
        </div>
        
        <!-- 分割线 -->
        <el-divider />
        
        <!-- 心情留言板 -->
        <div class="comments-section">
          <div class="comments-header">
            <h3 class="section-title">💭 心情留言</h3>
            <el-button type="primary" size="small" @click="showCommentDialog = true">
              ✏️ 写留言
            </el-button>
          </div>
          
          <CommentTimeline
            :comments="comments"
            :loading="commentsLoading"
            :hasMore="hasMoreComments"
            @add="showCommentDialog = true"
            @like="handleLikeComment"
            @reply="handleReplyComment"
            @delete="handleDeleteComment"
            @load-more="loadMoreComments"
          />
        </div>
      </div>
    </el-dialog>
    
    <!-- 写留言对话框 -->
    <MoodCommentDialog
      v-model:visible="showCommentDialog"
      :diary-id="currentDiary?.id || 0"
      @success="handleCommentSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { HomeFilled } from '@element-plus/icons-vue'
import WeatherBackground from '@/components/MoodDiary/WeatherBackground.vue'
import LightSwitch from '@/components/MoodDiary/LightSwitch.vue'
import DiaryCard from '@/components/MoodDiary/DiaryCard.vue'
import MoodCommentDialog from '@/components/MoodDiary/MoodCommentDialog.vue'
import CommentTimeline from '@/components/MoodDiary/CommentTimeline.vue'
import MoodForecast from '@/components/MoodDiary/MoodForecast.vue'
import { symptomApi, commentApi } from '@/api'
import { playSound } from '@/utils/soundService'
import { useUserStore } from '@/stores/user'

// 用户状态
const userStore = useUserStore()

// 灯光模式
const lightMode = ref<'day' | 'night'>('day')

// 天气类型（根据最新日记或当前编辑的心情自动设置）
const currentWeather = computed(() => {
  // 如果正在写日记,使用新日记的心情
  if (showAddDialog.value && newDiary.value.moodScore) {
    const score = newDiary.value.moodScore
    if (score <= 2) return 'storm'
    if (score <= 4) return 'rain'
    if (score <= 6) return 'cloudy'
    if (score <= 8) return 'sunny'
    return 'clear'
  }

  // 否则使用最新日记的天气
  if (diaries.value.length > 0) {
    return diaries.value[0].weatherType || 'sunny'
  }

  // 默认晴朗
  return 'sunny'
})

// 心情表情映射
const moodEmojis: Record<number, string> = {
  1: '😭', 2: '😢', 3: '😔', 4: '😕', 5: '😐',
  6: '🙂', 7: '😊', 8: '😄', 9: '😁', 10: '🤩'
}

// 日记列表
const diaries = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

// 新日记表单
const showAddDialog = ref(false)
const submitting = ref(false)
const newDiary = ref<{
  moodScore: number
  moodEmoji: string
  title: string
  content: string
  energyLevel: number
  sleepQuality: number
  stressLevel: number
}>({
  moodScore: 5,
  moodEmoji: '😐',
  title: '',
  content: '',
  energyLevel: 5,
  sleepQuality: 5,
  stressLevel: 5
})

// 日记详情
const showDetailDialog = ref(false)
const currentDiary = ref<any>(null)
const detailLoading = ref(false)

// 留言相关
const showCommentDialog = ref(false)
const comments = ref<any[]>([])
const commentsLoading = ref(false)
const hasMoreComments = ref(false)
const commentsPage = ref(1)
const commentsPageSize = ref(20)

// 是否有多维度评分
const hasDimensions = computed(() => {
  if (!currentDiary.value) return false
  return currentDiary.value.energyLevel || currentDiary.value.sleepQuality || currentDiary.value.stressLevel
})

// 切换灯光
const toggleLight = async () => {
  lightMode.value = lightMode.value === 'day' ? 'night' : 'day'
  const message = lightMode.value === 'day' ? '开灯啦 💡' : '关灯啦 🌙'
  ElMessage.success(message)
  
  // TODO: 调用后端API保存灯光模式
}

// 选择心情
const selectMood = (score: number, emoji: string) => {
  newDiary.value.moodScore = Number(score)
  newDiary.value.moodEmoji = emoji
}

// 更新心情表情
const updateMoodEmoji = (score: number | string) => {
  const numScore = Number(score)
  newDiary.value.moodScore = numScore
  newDiary.value.moodEmoji = moodEmojis[numScore]
}

// 加载日记列表
const loadDiaries = async () => {
  try {
    loading.value = true
    const res = await symptomApi.getList({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    
    if (res.code === 200 && res.data) {
      diaries.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load diaries:', error)
    ElMessage.error('加载日记失败')
  } finally {
    loading.value = false
  }
}

// 添加日记
const handleAddDiary = async () => {
  if (!newDiary.value.moodScore) {
    ElMessage.warning('请选择心情评分')
    return
  }

  try {
    submitting.value = true
    const res = await symptomApi.addRecord(newDiary.value)

    if (res.code === 200) {
      playSound('write')
      ElMessage.success('日记保存成功！')
      showAddDialog.value = false
      resetForm()
      loadDiaries()
    }
  } catch (error) {
    console.error('Failed to add diary:', error)
    ElMessage.error('保存日记失败')
  } finally {
    submitting.value = false
  }
}

// 查看日记详情
const viewDiary = async (diary: any) => {
  currentDiary.value = diary
  showDetailDialog.value = true
  
  // 加载留言
  await loadComments()
}

// 重置表单
const resetForm = () => {
  newDiary.value = {
    moodScore: 5,
    moodEmoji: '😐',
    title: '',
    content: '',
    energyLevel: 5,
    sleepQuality: 5,
    stressLevel: 5
  }
}

// 加载留言列表
const loadComments = async () => {
  if (!currentDiary.value?.id) return
  
  try {
    commentsLoading.value = true
    const res = await commentApi.getList(currentDiary.value.id, {
      pageNum: commentsPage.value,
      pageSize: commentsPageSize.value
    })
    
    if (res.code === 200 && res.data) {
      // 处理留言数据，添加 liked 字段
      comments.value = res.data.map((comment: any) => {
        // 解析 interactions 字段（JSON字符串）
        let interactions: string[] = []
        try {
          interactions = comment.interactions ? JSON.parse(comment.interactions) : []
        } catch (e) {
          interactions = []
        }
        
        return {
          ...comment,
          liked: interactions.includes('like'),
          interactionCount: interactions.length
        }
      })
      
      // TODO: 处理分页
      hasMoreComments.value = false
    }
  } catch (error) {
    console.error('Failed to load comments:', error)
    ElMessage.error('加载留言失败')
  } finally {
    commentsLoading.value = false
  }
}

// 留言添加成功
const handleCommentSuccess = () => {
  loadComments()
}

// 状态修改
const handleStatusChange = async (newStatus: string) => {
  if (!currentDiary.value) return

  try {
    const res = await symptomApi.updateRecord({
      ...currentDiary.value,
      status: newStatus
    })

    if (res.code === 200) {
      ElMessage.success('状态已更新！')
      // 刷新日记列表
      loadDiaries()
    }
  } catch (error) {
    console.error('Failed to update status:', error)
    ElMessage.error('状态更新失败')
    // 恢复原来的状态
    if (currentDiary.value) {
      const originalDiary = diaries.value.find(d => d.id === currentDiary.value?.id)
      if (originalDiary) {
        currentDiary.value.status = originalDiary.status
      }
    }
  }
}

// 点赞留言
const handleLikeComment = async (comment: any) => {
  try {
    // 切换点赞状态：如果已点赞则取消（传空数组），否则点赞（传['like']）
    const interactions = comment.liked ? [] : ['like']
    const res = await commentApi.like(comment.id, interactions)
    if (res.code === 200) {
      // 切换点赞状态
      comment.liked = !comment.liked
      ElMessage.success(comment.liked ? '点赞成功！' : '已取消点赞')
    }
  } catch (error) {
    console.error('Failed to like comment:', error)
    ElMessage.error('操作失败')
  }
}

// 回复留言
const handleReplyComment = (comment: any) => {
  // TODO: 实现回复功能（Phase 2）
  ElMessage.info('回复功能将在下一阶段开发')
}

// 删除留言
const handleDeleteComment = async (comment: any) => {
  try {
    const res = await commentApi.delete(comment.id)
    if (res.code === 200) {
      ElMessage.success('留言已删除')
      loadComments()
    }
  } catch (error) {
    console.error('Failed to delete comment:', error)
    ElMessage.error('删除失败')
  }
}

// 加载更多留言
const loadMoreComments = () => {
  commentsPage.value++
  loadComments()
}

// 获取天气图标
const getWeatherIcon = (weather: string) => {
  const icons: Record<string, string> = {
    sunny: '☀️',
    cloudy: '⛅',
    rain: '🌧️',
    storm: '⛈️',
    clear: '🌤️'
  }
  return icons[weather] || '☀️'
}

// 获取天气标签
const getWeatherLabel = (weather: string) => {
  const labels: Record<string, string> = {
    sunny: '晴朗',
    cloudy: '多云',
    rain: '阴雨',
    storm: '风暴',
    clear: '放晴'
  }
  return labels[weather] || weather
}

// 获取状态类型
const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    pending: 'info',
    processing: 'warning',
    completed: 'success'
  }
  return types[status] || 'info'
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成'
  }
  return labels[status] || status
}

// 格式化时间
const formatDateTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadDiaries()
})
</script>

<style scoped>
.mood-diary-page {
  position: relative;
  min-height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 自定义滚动条样式 */
.mood-diary-page::-webkit-scrollbar {
  width: 10px;
}

.mood-diary-page::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
}

.mood-diary-page::-webkit-scrollbar-thumb {
  background: rgba(64, 158, 255, 0.4);
  border-radius: 10px;
  transition: background 0.3s;
}

.mood-diary-page::-webkit-scrollbar-thumb:hover {
  background: rgba(64, 158, 255, 0.6);
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
  max-width: 1200px;
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
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px 0;
}

.page-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.forecast-section {
  max-width: 500px;
  margin: 0 auto 32px;
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.add-btn,
.room-btn {
  font-size: 16px;
  padding: 12px 32px;
  border-radius: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.add-btn:hover,
.room-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.diary-list {
  min-height: 400px;
}

.diary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 对话框样式 */
.mood-selector {
  width: 100%;
}

.mood-emojis {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 8px;
}

.emoji-option {
  font-size: 32px;
  cursor: pointer;
  transition: transform 0.2s ease;
  opacity: 0.5;
}

.emoji-option:hover {
  transform: scale(1.2);
}

.emoji-option.active {
  opacity: 1;
  transform: scale(1.3);
}

.multi-dimensions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dimension-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dimension-item span {
  min-width: 60px;
  font-size: 14px;
  color: #606266;
}

/* 日记详情对话框样式 */
.diary-detail-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.diary-detail {
  min-height: 400px;
}

/* 详情头部 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
  border-radius: 12px;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.status-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 8px 12px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.status-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.mood-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mood-emoji {
  font-size: 48px;
}

.mood-score {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.weather-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border-radius: 20px;
  font-size: 14px;
  color: #606266;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.weather-icon {
  font-size: 20px;
}

/* 详情内容 */
.detail-content {
  padding: 0 8px;
}

.content-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.content-text {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin: 0;
}

/* 多维度评分 */
.dimensions-section {
  margin-bottom: 24px;
}

.dimensions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.dimension-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #fff 0%, #f9fafb 100%);
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  gap: 8px;
}

.dimension-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.dimension-value {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 元信息 */
.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-item i {
  font-size: 14px;
}

/* 留言区域 */
.comments-section {
  margin-top: 24px;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.comments-header .section-title {
  margin: 0;
}
</style>
