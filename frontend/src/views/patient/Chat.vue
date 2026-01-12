<template>
  <div class="chat-container">
    <el-row :gutter="20" style="height: 100%">
      <el-col :span="6">
        <el-card class="chat-list-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>最近聊天</span>
            </div>
          </template>
          <el-scrollbar height="calc(100vh - 200px)">
            <div
              v-for="chat in chatList"
              :key="chat.sessionId"
              class="chat-item"
              :class="{ active: currentChat?.sessionId === chat.sessionId }"
              @click="selectChat(chat)"
            >
              <el-badge :value="chat.unreadCount" :hidden="chat.unreadCount === 0">
                <el-avatar :src="chat.avatar || '/default-avatar.jpg'" :size="45">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
              </el-badge>
              <div class="chat-info">
                <div class="chat-name">{{ chat.name }}</div>
                <div class="chat-last-message">{{ formatLastMessage(chat.lastMessage) }}</div>
              </div>
              <div class="chat-time">{{ formatTime(chat.lastTime) }}</div>
            </div>
            <el-empty v-if="chatList.length === 0" description="暂无聊天记录" :image-size="80" />
          </el-scrollbar>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card class="chat-window-card" shadow="never" v-if="currentChat">
          <template #header>
            <div class="card-header">
              <div class="chat-header-info">
                <el-avatar :src="currentChat.avatar || '/default-avatar.jpg'" :size="35">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <span class="chat-header-name">{{ currentChat.name }}</span>
                <el-tag size="small" type="success" v-if="currentChat.online">在线</el-tag>
                <el-tag size="small" type="info" v-else>离线</el-tag>
              </div>
            </div>
          </template>

          <el-scrollbar ref="scrollbarRef" height="calc(100vh - 350px)">
            <div class="message-list">
              <div
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="{ 'is-self': message.isSelf }"
              >
                <el-avatar :src="message.avatar || '/default-avatar.jpg'" :size="40">
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <div class="message-content">
                  <div class="message-name">{{ message.senderName }}</div>
                  <div class="message-bubble">
                    <span v-if="message.type === 'text'">{{ message.content }}</span>
                    <el-image v-else-if="message.type === 'image'" :src="message.content" fit="cover" style="max-width: 300px" />
                  </div>
                  <div class="message-time">{{ formatDateTime(message.createTime) }}</div>
                </div>
              </div>
            </div>
          </el-scrollbar>

          <div class="chat-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息..."
              @keydown.enter.ctrl="sendMessage"
            />
            <div class="input-actions">
              <el-upload
                action="#"
                :show-file-list="false"
                :before-upload="handleImageUpload"
              >
                <el-button size="small">
                  <el-icon><Picture /></el-icon> 图片
                </el-button>
              </el-upload>
              <el-button type="primary" @click="sendMessage" :loading="sending">
                发送 (Ctrl+Enter)
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card class="chat-window-card" shadow="never" v-else>
          <el-empty description="请选择一个聊天" :image-size="150" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'

const route = useRoute()

interface ChatItem {
  sessionId: number
  userId: number
  name: string
  avatar: string
  lastMessage: string
  lastTime: string
  unreadCount: number
  sessionStatus: string
  online: boolean
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

const chatList = ref<ChatItem[]>([])
const currentChat = ref<ChatItem | null>(null)
const messages = ref<MessageItem[]>([])
const inputMessage = ref('')
const sending = ref(false)
const scrollbarRef = ref()

const loadChatList = async () => {
  try {
    const { data } = await request.get('/patient/chat/list')
    chatList.value = data || []

    const doctorId = route.query.doctorId
    if (doctorId) {
      const chat = chatList.value.find(c => c.userId === Number(doctorId))
      if (chat) {
        selectChat(chat)
      } else {
        await createChat(doctorId as string)
      }
    }
  } catch (error) {
    ElMessage.error('加载聊天列表失败')
  }
}

const createChat = async (doctorId: string) => {
  try {
    const { data } = await request.post('/patient/chat/create', { doctorId })
    chatList.value.unshift(data)
    selectChat(data)
  } catch (error) {
    ElMessage.error('创建聊天失败')
  }
}

const selectChat = async (chat: ChatItem) => {
  currentChat.value = chat
  await loadMessages()

  if (chat.unreadCount > 0) {
    markAsRead()
    chat.unreadCount = 0
  }
}

const loadMessages = async () => {
  if (!currentChat.value) return
  try {
    const { data } = await request.get(
      `/patient/chat/messages/${currentChat.value.sessionId}`,
      { params: { pageNum: 1, pageSize: 100 } }
    )
    messages.value = data?.records || []
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentChat.value) return

  sending.value = true
  try {
    const { data } = await request.post('/patient/chat/send', {
      sessionId: currentChat.value.sessionId,
      targetUserId: currentChat.value.userId,
      content: inputMessage.value,
      type: 'text'
    })
    messages.value.push(data)
    inputMessage.value = ''
    scrollToBottom()
    currentChat.value.lastMessage = data.content
    currentChat.value.lastTime = data.createTime
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const handleImageUpload = async (file: File) => {
  if (!currentChat.value) return false

  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploadRes = await request.post('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    const { data } = await request.post('/patient/chat/send', {
      sessionId: currentChat.value.sessionId,
      targetUserId: currentChat.value.userId,
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
  if (!currentChat.value) return
  try {
    await request.post(`/patient/chat/read/${currentChat.value.sessionId}`)
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
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatLastMessage = (msg: string) => {
  if (!msg) return ''
  if (msg.startsWith('/uploads/') || msg.startsWith('http')) {
    return '[图片]'
  }
  return msg.length > 20 ? msg.substring(0, 20) + '...' : msg
}

onMounted(() => {
  loadChatList()
})

watch(() => route.query.doctorId, (newId) => {
  if (newId) {
    loadChatList()
  }
})
</script>

<style scoped lang="scss">
.chat-container {
  padding: 20px;
  height: calc(100vh - 60px);
}

.chat-list-card,
.chat-window-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;

  .chat-header-info {
    display: flex;
    align-items: center;
    gap: 10px;

    .chat-header-name {
      font-size: 16px;
    }
  }
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.3s;
  position: relative;

  &:hover {
    background-color: #f5f7fa;
  }

  &.active {
    background-color: #ecf5ff;
  }

  .chat-info {
    flex: 1;
    overflow: hidden;

    .chat-name {
      font-weight: 600;
      margin-bottom: 4px;
    }

    .chat-last-message {
      font-size: 13px;
      color: #909399;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .chat-time {
    font-size: 12px;
    color: #c0c4cc;
    position: absolute;
    top: 12px;
    right: 12px;
  }
}

.message-list {
  padding: 20px;

  .message-item {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;

    &.is-self {
      flex-direction: row-reverse;

      .message-content {
        align-items: flex-end;

        .message-bubble {
          background-color: #409EFF;
          color: white;
        }
      }
    }

    .message-content {
      display: flex;
      flex-direction: column;
      max-width: 60%;

      .message-name {
        font-size: 12px;
        color: #909399;
        margin-bottom: 5px;
      }

      .message-bubble {
        background-color: #f5f7fa;
        padding: 10px 15px;
        border-radius: 8px;
        word-break: break-word;
        line-height: 1.6;
      }

      .message-time {
        font-size: 12px;
        color: #c0c4cc;
        margin-top: 5px;
      }
    }
  }
}

.chat-input {
  padding: 15px;
  border-top: 1px solid #ebeef5;

  .input-actions {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
  }
}
</style>
