<template>
  <div class="chat-container">
    <el-row :gutter="20" style="height: calc(100vh - 100px)">
      <!-- 患者列表 -->
      <el-col :span="6">
        <el-card style="height: 100%">
          <template #header>
            <span>患者列表</span>
          </template>
          <el-input
            v-model="searchText"
            placeholder="搜索患者..."
            style="margin-bottom: 10px"
            clearable
          />
          <el-scrollbar height="calc(100% - 80px)">
            <div
              v-for="chat in filteredChatList"
              :key="chat.sessionId"
              class="patient-item"
              :class="{ active: selectedChat?.sessionId === chat.sessionId }"
              @click="selectChat(chat)"
            >
              <el-badge :value="chat.unreadCount" :hidden="!chat.unreadCount">
                <el-avatar :src="chat.avatar || defaultAvatar" :size="45">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </el-badge>
              <div class="patient-info">
                <div class="name">{{ chat.name }}</div>
                <div class="last-message">{{ chat.lastMessage || '暂无消息' }}</div>
              </div>
              <div class="chat-time" v-if="chat.lastTime">
                {{ formatTime(chat.lastTime) }}
              </div>
            </div>
            <el-empty v-if="filteredChatList.length === 0" description="暂无聊天" />
          </el-scrollbar>
        </el-card>
      </el-col>

      <!-- 聊天区域 -->
      <el-col :span="18">
        <el-card style="height: 100%" v-if="selectedChat">
          <template #header>
            <div class="chat-header">
              <el-avatar :src="selectedChat.avatar || defaultAvatar" :size="40">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span style="margin-left: 10px; font-weight: 600;">{{ selectedChat.name }}</span>
              <el-tag
                :type="selectedChat.sessionStatus === 'ongoing' ? 'success' : 'info'"
                size="small"
                style="margin-left: 10px"
              >
                {{ selectedChat.sessionStatus === 'ongoing' ? '进行中' : '已结束' }}
              </el-tag>
            </div>
          </template>

          <el-scrollbar height="calc(100% - 180px)" ref="scrollbarRef">
            <div class="message-list" ref="messageListRef">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="message-item"
                :class="msg.isSelf ? 'self' : 'other'"
              >
                <el-avatar :src="msg.avatar || defaultAvatar" :size="40">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <div class="message-content">
                  <div class="message-name">{{ msg.senderName }}</div>
                  <div class="message-bubble">
                    <span v-if="msg.type === 'text'">{{ msg.content }}</span>
                    <el-image
                      v-else-if="msg.type === 'image'"
                      :src="msg.content"
                      fit="cover"
                      style="max-width: 200px; border-radius: 8px;"
                      :preview-src-list="[msg.content]"
                    />
                  </div>
                  <div class="message-time">{{ formatDateTime(msg.createTime) }}</div>
                </div>
              </div>
              <el-empty v-if="messages.length === 0" description="暂无消息" />
            </div>
          </el-scrollbar>

          <div class="chat-input">
            <el-input
              v-model="messageText"
              type="textarea"
              :rows="3"
              placeholder="输入消息... (Ctrl+Enter发送)"
              @keydown.ctrl.enter="sendMessage"
              :disabled="selectedChat.sessionStatus !== 'ongoing'"
            />
            <div class="input-actions">
              <el-upload
                action="#"
                :show-file-list="false"
                :before-upload="handleImageUpload"
                :disabled="selectedChat.sessionStatus !== 'ongoing'"
              >
                <el-button size="small" :disabled="selectedChat.sessionStatus !== 'ongoing'">
                  <el-icon><Picture /></el-icon> 图片
                </el-button>
              </el-upload>
              <el-button
                type="primary"
                @click="sendMessage"
                :loading="sending"
                :disabled="selectedChat.sessionStatus !== 'ongoing'"
              >
                发送
              </el-button>
            </div>
          </div>
        </el-card>
        <el-card v-else style="height: 100%; display: flex; align-items: center; justify-content: center;">
          <el-empty description="请选择一个患者开始聊天" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'

const defaultAvatar = '/default-avatar.jpg'

interface ChatItem {
  sessionId: number
  userId: number
  name: string
  avatar: string
  lastMessage: string
  lastTime: string
  unreadCount: number
  sessionStatus: string
}

interface MessageItem {
  id: number
  content: string
  type: string
  createTime: string
  isSelf: boolean
  senderName: string
  avatar: string
}

const searchText = ref('')
const chatList = ref<ChatItem[]>([])
const selectedChat = ref<ChatItem | null>(null)
const messages = ref<MessageItem[]>([])
const messageText = ref('')
const sending = ref(false)
const scrollbarRef = ref()

const filteredChatList = computed(() => {
  if (!searchText.value) return chatList.value
  return chatList.value.filter(c =>
    c.name.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

const loadChatList = async () => {
  try {
    const { data } = await request.get('/doctor/chat/list')
    chatList.value = data || []
  } catch (error) {
    ElMessage.error('加载聊天列表失败')
  }
}

const selectChat = async (chat: ChatItem) => {
  selectedChat.value = chat
  await loadMessages()
  if (chat.unreadCount > 0) {
    markAsRead()
    chat.unreadCount = 0
  }
}

const loadMessages = async () => {
  if (!selectedChat.value) return
  try {
    const { data } = await request.get(
      `/doctor/chat/messages/${selectedChat.value.sessionId}`,
      { params: { pageNum: 1, pageSize: 100 } }
    )
    messages.value = data?.records || []
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!messageText.value.trim() || !selectedChat.value) return
  sending.value = true
  try {
    const { data } = await request.post('/doctor/chat/send', {
      sessionId: selectedChat.value.sessionId,
      targetUserId: selectedChat.value.userId,
      content: messageText.value,
      type: 'text'
    })
    messages.value.push(data)
    messageText.value = ''
    scrollToBottom()
    selectedChat.value.lastMessage = data.content
    selectedChat.value.lastTime = data.createTime
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const handleImageUpload = async (file: File) => {
  if (!selectedChat.value) return false
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadRes = await request.post('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const { data } = await request.post('/doctor/chat/send', {
      sessionId: selectedChat.value.sessionId,
      targetUserId: selectedChat.value.userId,
      content: uploadRes.data.url,
      type: 'image'
    })
    messages.value.push(data)
    scrollToBottom()
  } catch (error) {
    ElMessage.error('图片上传失败')
  }
  return false
}

const markAsRead = async () => {
  if (!selectedChat.value) return
  try {
    await request.post(`/doctor/chat/read/${selectedChat.value.sessionId}`)
  } catch (error) {
    console.error('标记已读失败')
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollbarRef.value) {
      scrollbarRef.value.setScrollTop(99999)
    }
  })
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}

const formatDateTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

onMounted(() => {
  loadChatList()
})
</script>

<style scoped>
.chat-container {
  padding: 20px;
}

.patient-item {
  display: flex;
  align-items: center;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 8px;
  position: relative;
}

.patient-item:hover,
.patient-item.active {
  background-color: #ecf5ff;
}

.patient-info {
  flex: 1;
  margin-left: 10px;
  overflow: hidden;
}

.patient-info .name {
  font-weight: 600;
  margin-bottom: 4px;
}

.patient-info .last-message {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-time {
  font-size: 12px;
  color: #c0c4cc;
  position: absolute;
  top: 12px;
  right: 12px;
}

.chat-header {
  display: flex;
  align-items: center;
}

.message-list {
  padding: 20px;
  min-height: 300px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  gap: 10px;
}

.message-item.self {
  flex-direction: row-reverse;
}

.message-item.self .message-content {
  align-items: flex-end;
}

.message-item.self .message-bubble {
  background-color: #409eff;
  color: white;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}

.message-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.message-bubble {
  padding: 10px 15px;
  border-radius: 8px;
  background-color: #f5f7fa;
  word-break: break-word;
  line-height: 1.6;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 5px;
}

.chat-input {
  border-top: 1px solid #ebeef5;
  padding: 15px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}
</style>
