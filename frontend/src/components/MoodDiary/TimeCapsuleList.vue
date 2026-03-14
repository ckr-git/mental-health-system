<template>
  <div class="capsule-list">
    <!-- 空状态 -->
    <el-empty 
      v-if="capsules.length === 0 && !loading"
      description="还没有寄出任何信件"
      :image-size="140"
    >
      <el-button type="primary" @click="emit('create')">
        📮 写第一封信
      </el-button>
    </el-empty>
    
    <!-- 信件列表 -->
    <div v-else class="capsules-container" v-loading="loading">
      <div class="capsules-header">
        <h3 class="header-title">📬 我的时光信箱</h3>
        <p class="header-subtitle">{{ statusText }}</p>
      </div>
      
      <!-- 筛选标签 -->
      <div class="filter-tabs">
        <el-radio-group v-model="filterStatus" size="small">
          <el-radio-button label="all">全部 ({{ total }})</el-radio-button>
          <el-radio-button label="locked">待解锁 ({{ lockedCount }})</el-radio-button>
          <el-radio-button label="unlocked">已解锁 ({{ unlockedCount }})</el-radio-button>
          <el-radio-button label="replied">已回复 ({{ repliedCount }})</el-radio-button>
        </el-radio-group>
      </div>
      
      <!-- 信件卡片网格 -->
      <div class="capsules-grid">
        <div
          v-for="capsule in filteredCapsules"
          :key="capsule.id"
          class="capsule-card"
          :class="{
            'is-locked': !capsule.isUnlocked,
            'is-unlocked': capsule.isUnlocked && !capsule.replyContent,
            'is-replied': capsule.replyContent,
            [`type-${capsule.letterType}`]: true
          }"
          @click="handleCardClick(capsule)"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="header-left">
              <span class="letter-icon">{{ getLetterIcon(capsule.letterType) }}</span>
              <span class="letter-type">{{ getLetterTypeName(capsule.letterType) }}</span>
            </div>
            <div class="header-right">
              <el-tag
                :type="capsule.replyContent ? 'success' : capsule.isUnlocked ? 'success' : 'info'"
                size="small"
              >
                {{ capsule.replyContent ? '💬 已回复' : capsule.isUnlocked ? '📭 已送达' : '📬 寄送中' }}
              </el-tag>
            </div>
          </div>
          
          <!-- 卡片内容 -->
          <div class="card-body">
            <!-- 未解锁状态 -->
            <div v-if="!capsule.isUnlocked" class="locked-content">
              <div class="lock-icon">🔒</div>
              <div class="lock-title">{{ capsule.title || '给未来的自己' }}</div>
              <div class="lock-time">
                <span class="time-label">解锁倒计时</span>
                <div class="countdown">
                  <span class="countdown-text">{{ getCountdown(capsule.unlockTime) }}</span>
                </div>
              </div>
              <div class="lock-date">
                {{ formatUnlockDate(capsule.unlockTime) }}
              </div>
              
              <!-- 特殊条件提示 -->
              <div v-if="capsule.unlockConditions && capsule.unlockConditions.length > 0" class="unlock-conditions">
                <span class="condition-label">或满足条件时解锁：</span>
                <div class="conditions-list">
                  <el-tag 
                    v-for="condition in capsule.unlockConditions" 
                    :key="condition"
                    size="small"
                    type="warning"
                  >
                    {{ getConditionText(condition) }}
                  </el-tag>
                </div>
              </div>
            </div>
            
            <!-- 已解锁状态 -->
            <div v-else class="unlocked-content">
              <div class="unlock-badge">✨ 已解锁</div>
              <div class="letter-title">{{ capsule.title || '给未来的自己' }}</div>
              <div class="letter-preview">
                {{ getPreview(capsule.content) }}
              </div>
              <div class="letter-meta">
                <span class="meta-item">
                  <i class="el-icon-time"></i>
                  寄出：{{ formatDate(capsule.createTime) }}
                </span>
                <span class="meta-item">
                  <i class="el-icon-unlock"></i>
                  解锁：{{ formatDate(capsule.unlockTime) }}
                </span>
              </div>
            </div>
          </div>
          
          <!-- 卡片底部 -->
          <div class="card-footer">
            <el-button 
              v-if="!capsule.isUnlocked" 
              text 
              size="small"
              disabled
            >
              <span class="footer-text">🕐 等待解锁中...</span>
            </el-button>
            <el-button 
              v-else
              type="primary"
              text 
              size="small"
            >
              <span class="footer-text">📖 查看完整信件</span>
            </el-button>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="emit('page-change', currentPage)"
        />
      </div>
    </div>
    
    <!-- 查看信件对话框 -->
    <el-dialog
      v-model="showLetterDialog"
      :title="currentLetter?.title || '给未来的自己'"
      width="600px"
      class="letter-dialog"
    >
      <div class="letter-content" v-if="currentLetter">
        <div class="letter-envelope">
          <div class="envelope-header">
            <span class="envelope-icon">{{ getLetterIcon(currentLetter.letterType) }}</span>
            <span class="envelope-type">{{ getLetterTypeName(currentLetter.letterType) }}</span>
          </div>
          
          <div class="letter-body">
            <div class="letter-greeting">{{ getGreeting(currentLetter.letterType) }}</div>
            <div class="letter-text">{{ currentLetter.content }}</div>
            <div class="letter-signature">
              {{ formatDate(currentLetter.createTime) }} 的我
            </div>
          </div>
          
          <div class="letter-meta-info">
            <div class="meta-row">
              <span class="meta-label">寄出时间：</span>
              <span class="meta-value">{{ formatDateTime(currentLetter.createTime) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">解锁时间：</span>
              <span class="meta-value">{{ formatDateTime(currentLetter.unlockTime) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">穿越时光：</span>
              <span class="meta-value">{{ getDaysSpan(currentLetter.createTime, currentLetter.unlockTime) }} 天</span>
            </div>
          </div>
        </div>

        <!-- 回复内容 -->
        <div v-if="currentLetter.replyContent" class="letter-reply">
          <div class="reply-divider">
            <span class="divider-text">💬 来自未来的回复</span>
          </div>
          <div class="reply-content">
            <div class="reply-greeting">现在的我想说：</div>
            <div class="reply-text">{{ currentLetter.replyContent }}</div>
            <div class="reply-signature">
              {{ formatDate(currentLetter.replyTime) }} 的我
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showLetterDialog = false">关闭</el-button>
        <el-button
          type="primary"
          @click="handleReply"
          v-if="!currentLetter.replyContent"
        >
          💬 回复这封信
        </el-button>
        <el-button
          type="warning"
          @click="handleReply"
          v-else
        >
          ✏️ 修改回复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(duration)
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

// Props
const props = defineProps<{
  capsules: any[]
  loading?: boolean
  total?: number
}>()

// Emits
const emit = defineEmits<{
  'create': []
  'view': [capsule: any]
  'reply': [capsule: any]
  'page-change': [page: number]
}>()

// 筛选状态
const filterStatus = ref('all')
const currentPage = ref(1)
const pageSize = ref(9)

// 查看信件对话框
const showLetterDialog = ref(false)
const currentLetter = ref<any>(null)

// 信件类型配置
const letterTypeMap = {
  praise: { name: '表扬信', icon: '🎉', greeting: '未来的我，你好！' },
  hope: { name: '期望信', icon: '💌', greeting: '致未来更好的我：' },
  thanks: { name: '感谢信', icon: '✉️', greeting: '未来度过难关的我：' }
}

// 筛选后的信件列表
const filteredCapsules = computed(() => {
  if (filterStatus.value === 'all') return props.capsules
  if (filterStatus.value === 'locked') return props.capsules.filter(c => !c.isUnlocked)
  if (filterStatus.value === 'unlocked') return props.capsules.filter(c => c.isUnlocked && !c.replyContent)
  if (filterStatus.value === 'replied') return props.capsules.filter(c => c.replyContent)
  return props.capsules
})

// 统计数量
const lockedCount = computed(() => props.capsules.filter(c => !c.isUnlocked).length)
const unlockedCount = computed(() => props.capsules.filter(c => c.isUnlocked && !c.replyContent).length)
const repliedCount = computed(() => props.capsules.filter(c => c.replyContent).length)

// 状态文本
const statusText = computed(() => {
  const locked = lockedCount.value
  const unlocked = unlockedCount.value
  if (locked === 0 && unlocked === 0) return '还没有信件'
  if (locked === 0) return `所有信件已解锁`
  if (unlocked === 0) return `${locked} 封信件等待解锁`
  return `${unlocked} 封已解锁，${locked} 封等待中`
})

// 获取信件类型名称
const getLetterTypeName = (type: string) => {
  return letterTypeMap[type as keyof typeof letterTypeMap]?.name || type
}

// 获取信件图标
const getLetterIcon = (type: string) => {
  return letterTypeMap[type as keyof typeof letterTypeMap]?.icon || '✉️'
}

// 获取问候语
const getGreeting = (type: string) => {
  return letterTypeMap[type as keyof typeof letterTypeMap]?.greeting || '亲爱的未来的我：'
}

// 获取倒计时
const getCountdown = (unlockTime: string) => {
  if (!unlockTime) return '等待中...'
  
  const now = dayjs()
  const target = dayjs(unlockTime)
  const diff = target.diff(now, 'second')
  
  if (diff <= 0) return '已到期'
  
  const days = Math.floor(diff / 86400)
  const hours = Math.floor((diff % 86400) / 3600)
  const minutes = Math.floor((diff % 3600) / 60)
  
  if (days > 0) return `${days}天 ${hours}小时`
  if (hours > 0) return `${hours}小时 ${minutes}分钟`
  return `${minutes}分钟`
}

// 格式化解锁日期
const formatUnlockDate = (unlockTime: string) => {
  if (!unlockTime) return ''
  return dayjs(unlockTime).format('YYYY年MM月DD日 HH:mm')
}

// 格式化日期
const formatDate = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD')
}

// 格式化日期时间
const formatDateTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('YYYY年MM月DD日 HH:mm')
}

// 获取天数跨度
const getDaysSpan = (start: string, end: string) => {
  if (!start || !end) return 0
  return dayjs(end).diff(dayjs(start), 'day')
}

// 获取预览文本
const getPreview = (content: string) => {
  if (!content) return '暂无内容'
  return content.length > 100 ? content.substring(0, 100) + '...' : content
}

// 获取条件文本
const getConditionText = (condition: string) => {
  const map: Record<string, string> = {
    mood_low: '情绪低落',
    mood_high: '情绪高涨',
    days_30: '30天后'
  }
  return map[condition] || condition
}

// 点击卡片
const handleCardClick = (capsule: any) => {
  if (!capsule.isUnlocked) {
    ElMessage.info('信件还未解锁，请耐心等待~')
    return
  }
  
  currentLetter.value = capsule
  showLetterDialog.value = true
  emit('view', capsule)
}

// 回复信件
const handleReply = () => {
  if (!currentLetter.value) return
  showLetterDialog.value = false
  emit('reply', currentLetter.value)
}
</script>

<style scoped>
.capsule-list {
  width: 100%;
}

.capsules-container {
  padding: 0;
}

.capsules-header {
  text-align: center;
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
  border-radius: 12px;
}

.header-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
}

.header-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

/* 筛选标签 */
.filter-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

/* 信件卡片网格 */
.capsules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.capsule-card {
  background: white;
  border-radius: 16px;
  border: 2px solid #e4e7ed;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardEnter 0.5s ease-out backwards;
  position: relative;
}

/* 入场动画 */
@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 为每个卡片设置不同的延迟 */
.capsule-card:nth-child(1) { animation-delay: 0.05s; }
.capsule-card:nth-child(2) { animation-delay: 0.1s; }
.capsule-card:nth-child(3) { animation-delay: 0.15s; }
.capsule-card:nth-child(4) { animation-delay: 0.2s; }
.capsule-card:nth-child(5) { animation-delay: 0.25s; }
.capsule-card:nth-child(6) { animation-delay: 0.3s; }

.capsule-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.capsule-card.is-locked {
  border-color: #d9d9d9;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
}

.capsule-card.is-unlocked {
  border-color: #52c41a;
  background: linear-gradient(135deg, #f6ffed 0%, #fcffe6 100%);
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.1);
}

.capsule-card.is-replied {
  border-color: #1890ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  position: relative;
}

/* 已回复卡片右上角标记 */
.capsule-card.is-replied::before {
  content: '💬';
  position: absolute;
  top: -4px;
  right: -4px;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
  z-index: 10;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.9;
  }
}

/* 不同类型的边框色 */
.capsule-card.type-praise.is-unlocked { border-color: #faad14; }
.capsule-card.type-hope.is-unlocked { border-color: #1890ff; }
.capsule-card.type-thanks.is-unlocked { border-color: #eb2f96; }

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.letter-icon {
  font-size: 24px;
}

.letter-type {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

/* 卡片内容 */
.card-body {
  padding: 20px;
  min-height: 200px;
}

/* 未解锁内容 */
.locked-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.lock-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.lock-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.lock-time {
  width: 100%;
  margin-bottom: 16px;
}

.time-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.countdown {
  padding: 12px 20px;
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border-radius: 24px;
  display: inline-block;
  animation: countdownGlow 2s ease-in-out infinite;
}

@keyframes countdownGlow {
  0%, 100% {
    box-shadow: 0 0 10px rgba(24, 144, 255, 0.3);
  }
  50% {
    box-shadow: 0 0 20px rgba(24, 144, 255, 0.6);
  }
}

.countdown-text {
  font-size: 18px;
  font-weight: 700;
  color: #1890ff;
  font-family: 'Courier New', monospace;
  animation: numberFlicker 1s ease-in-out infinite;
}

@keyframes numberFlicker {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.lock-date {
  font-size: 13px;
  color: #606266;
}

/* 解锁条件 */
.unlock-conditions {
  margin-top: 16px;
  padding: 12px;
  background: #fffbe6;
  border-radius: 8px;
  width: 100%;
}

.condition-label {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.conditions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

/* 已解锁内容 */
.unlocked-content {
  position: relative;
}

.unlock-badge {
  position: absolute;
  top: -10px;
  right: -10px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.letter-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.letter-preview {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.letter-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 卡片底部 */
.card-footer {
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #e4e7ed;
  text-align: center;
}

.footer-text {
  font-size: 13px;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 信件对话框 */
.letter-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.letter-envelope {
  background: linear-gradient(135deg, #fffbf0 0%, #fff9e6 100%);
  border: 2px solid #ffd700;
  border-radius: 12px;
  padding: 24px;
}

.envelope-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px dashed #ffd700;
}

.envelope-icon {
  font-size: 32px;
}

.envelope-type {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.letter-body {
  margin-bottom: 24px;
}

.letter-greeting {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.letter-text {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 20px;
}

.letter-signature {
  text-align: right;
  font-size: 14px;
  color: #909399;
  font-style: italic;
}

.letter-meta-info {
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
}

.meta-row:not(:last-child) {
  border-bottom: 1px dashed #e4e7ed;
}

.meta-label {
  color: #909399;
}

.meta-value {
  color: #303133;
  font-weight: 500;
}

/* 回复内容样式 */
.letter-reply {
  margin-top: 24px;
}

.reply-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 20px 0;
  position: relative;

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: linear-gradient(90deg, transparent, #d4d7dc, transparent);
  }

  .divider-text {
    padding: 0 16px;
    font-size: 14px;
    font-weight: 600;
    color: #67c23a;
    white-space: nowrap;
  }
}

.reply-content {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-left: 4px solid #67c23a;
  border-radius: 12px;
  padding: 20px;

  .reply-greeting {
    font-size: 15px;
    font-weight: 600;
    color: #409eff;
    margin-bottom: 12px;
  }

  .reply-text {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-word;
    margin-bottom: 16px;
  }

  .reply-signature {
    text-align: right;
    font-size: 13px;
    color: #67c23a;
    font-style: italic;
    font-weight: 500;
  }
}
</style>
