<template>
  <div class="comment-timeline">
    <!-- 空状态 -->
    <el-empty 
      v-if="comments.length === 0 && !loading"
      description="还没有留言，写下第一条留言吧"
      :image-size="120"
    >
      <el-button type="primary" size="small" @click="emit('add')">
        写留言
      </el-button>
    </el-empty>
    
    <!-- 时间线 -->
    <div v-else class="timeline-container" v-loading="loading">
      <div class="timeline-header">
        <h3 class="timeline-title">💭 心情留言板</h3>
        <p class="timeline-subtitle">与过去的自己对话</p>
      </div>
      
      <el-timeline class="timeline-list">
        <el-timeline-item
          v-for="comment in comments"
          :key="comment.id"
          :timestamp="formatTime(comment.createTime)"
          placement="top"
          :color="getTypeColor(comment.commentType)"
        >
          <div class="comment-card" :class="`type-${comment.commentType}`">
            <!-- 卡片头部 -->
            <div class="comment-header">
              <div class="interaction-badge">
                <span class="badge-icon">{{ getTypeIcon(comment.commentType) }}</span>
                <span class="badge-text">{{ getTypeLabel(comment.commentType) }}</span>
              </div>
              
              <!-- 互动统计 -->
              <div class="interaction-stats" v-if="comment.interactionCount > 0">
                <el-tooltip content="互动次数" placement="top">
                  <span class="stat-item">
                    <i class="el-icon-chat-dot-round"></i>
                    {{ comment.interactionCount }}
                  </span>
                </el-tooltip>
              </div>
            </div>
            
            <!-- 留言内容 -->
            <div class="comment-content">
              {{ comment.content }}
            </div>
            
            <!-- 卡片底部 -->
            <div class="comment-footer">
              <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
              
              <!-- 操作按钮 -->
              <div class="comment-actions">
                <el-button 
                  text 
                  size="small"
                  @click="handleLike(comment)"
                >
                  <span class="action-icon">{{ comment.liked ? '❤️' : '🤍' }}</span>
                  <span>{{ comment.liked ? '已点赞' : '点赞' }}</span>
                </el-button>
                
                <el-button 
                  text 
                  size="small"
                  @click="handleReply(comment)"
                >
                  <span class="action-icon">💬</span>
                  <span>回复</span>
                </el-button>
                
                <el-popconfirm
                  title="确定要删除这条留言吗？"
                  @confirm="handleDelete(comment)"
                >
                  <template #reference>
                    <el-button text size="small" type="danger">
                      <span class="action-icon">🗑️</span>
                      <span>删除</span>
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
            
            <!-- 回复列表 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
              <div 
                v-for="reply in comment.replies" 
                :key="reply.id"
                class="reply-item"
              >
                <div class="reply-header">
                  <span class="reply-badge">💬 回复</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      
      <!-- 加载更多 -->
      <div v-if="hasMore" class="load-more">
        <el-button @click="emit('load-more')" :loading="loading">
          加载更多
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'

// Props
const props = defineProps<{
  comments: any[]
  loading?: boolean
  hasMore?: boolean
}>()

// Emits
const emit = defineEmits<{
  'add': []
  'like': [comment: any]
  'reply': [comment: any]
  'delete': [comment: any]
  'load-more': []
}>()

// 互动类型配置
const typeConfig = {
  agree: { label: '赞同', icon: '👍', color: '#52c41a' },
  disagree: { label: '不赞同', icon: '🤔', color: '#faad14' },
  heartache: { label: '心疼', icon: '💔', color: '#f5222d' },
  encourage: { label: '鼓励', icon: '💪', color: '#1890ff' },
  relief: { label: '释然', icon: '🌈', color: '#722ed1' }
}

// 获取类型标签
const getTypeLabel = (type: string) => {
  return typeConfig[type as keyof typeof typeConfig]?.label || type
}

// 获取类型图标
const getTypeIcon = (type: string) => {
  return typeConfig[type as keyof typeof typeConfig]?.icon || '💭'
}

// 获取类型颜色
const getTypeColor = (type: string) => {
  return typeConfig[type as keyof typeof typeConfig]?.color || '#909399'
}

// 格式化时间
const formatTime = (time: string) => {
  const now = dayjs()
  const target = dayjs(time)
  const diff = now.diff(target, 'minute')
  
  if (diff < 1) return '刚刚'
  if (diff < 60) return `${diff}分钟前`
  if (diff < 1440) return `${Math.floor(diff / 60)}小时前`
  if (diff < 43200) return `${Math.floor(diff / 1440)}天前`
  
  return target.format('YYYY-MM-DD HH:mm')
}

// 格式化完整时间
const formatDateTime = (time: string) => {
  return dayjs(time).format('YYYY年MM月DD日 HH:mm')
}

// 点赞
const handleLike = (comment: any) => {
  emit('like', comment)
}

// 回复
const handleReply = (comment: any) => {
  emit('reply', comment)
}

// 删除
const handleDelete = (comment: any) => {
  emit('delete', comment)
}
</script>

<style scoped>
.comment-timeline {
  width: 100%;
}

.timeline-container {
  padding: 0;
}

.timeline-header {
  text-align: center;
  margin-bottom: 32px;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
  border-radius: 12px;
}

.timeline-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
}

.timeline-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.timeline-list {
  padding: 20px 0;
}

/* 留言卡片 */
.comment-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border-left: 4px solid #e4e7ed;
}

.comment-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateX(4px);
}

/* 不同类型的卡片边框色 */
.comment-card.type-agree { border-left-color: #52c41a; }
.comment-card.type-disagree { border-left-color: #faad14; }
.comment-card.type-heartache { border-left-color: #f5222d; }
.comment-card.type-encourage { border-left-color: #1890ff; }
.comment-card.type-relief { border-left-color: #722ed1; }

/* 卡片头部 */
.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.interaction-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.badge-icon {
  font-size: 16px;
}

.badge-text {
  color: #303133;
}

.interaction-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 留言内容 */
.comment-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 卡片底部 */
.comment-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.comment-actions :deep(.el-button) {
  padding: 4px 8px;
  font-size: 13px;
}

.action-icon {
  margin-right: 4px;
  font-size: 14px;
}

/* 回复列表 */
.replies-list {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}

.reply-item {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 8px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.reply-badge {
  font-size: 12px;
  color: #1890ff;
  font-weight: 600;
}

.reply-time {
  font-size: 11px;
  color: #909399;
}

.reply-content {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 24px 0;
}

/* 时间线样式覆盖 */
:deep(.el-timeline-item__timestamp) {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

:deep(.el-timeline-item__node) {
  width: 16px;
  height: 16px;
}

:deep(.el-timeline-item__tail) {
  border-left: 2px dashed #e4e7ed;
}
</style>
