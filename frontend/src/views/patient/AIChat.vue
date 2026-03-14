<template>
  <div class="ai-chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="chat-header">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI心理助手</span>
        </div>
      </template>

      <div class="chat-content" ref="chatContentRef">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-content">
            <div class="message-avatar">
              <el-avatar v-if="msg.role === 'user'" :src="userStore.userInfo?.avatar" />
              <el-icon v-else color="#409EFF" size="30"><ChatDotRound /></el-icon>
            </div>
            <div class="message-text">{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="message-content">
            <div class="message-avatar">
              <el-icon color="#409EFF" size="30"><ChatDotRound /></el-icon>
            </div>
            <div class="message-text">正在思考...</div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          @keyup.enter="sendMessage"
          :disabled="loading"
        >
          <template #append>
            <el-button :icon="Position" @click="sendMessage" :loading="loading">发送</el-button>
          </template>
        </el-input>
      </div>

      <div class="quick-questions">
        <el-tag v-for="q in quickQuestions" :key="q" @click="askQuestion(q)" style="margin-right: 10px; cursor: pointer">
          {{ q }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const chatContentRef = ref<HTMLElement>()
const loading = ref(false)
const inputMessage = ref('')

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<Message[]>([
  {
    role: 'assistant',
    content: '您好！我是您的AI心理助手，很高兴为您服务。您可以向我咨询任何心理健康相关的问题。'
  }
])

const quickQuestions = [
  '如何缓解焦虑？',
  '睡眠不好怎么办？',
  '如何调节情绪？',
  '压力太大怎么办？'
]

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContentRef.value) {
      // 使用 smooth 滚动行为
      chatContentRef.value.scrollTo({
        top: chatContentRef.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  const userMessage = inputMessage.value
  messages.value.push({
    role: 'user',
    content: userMessage
  })

  inputMessage.value = ''
  loading.value = true
  
  // 立即滚动到底部显示用户消息
  setTimeout(() => scrollToBottom(), 100)

  try {
    const res = await aiApi.ask(userMessage)
    messages.value.push({
      role: 'assistant',
      content: res.data
    })
    // AI回复后再次滚动到底部
    setTimeout(() => scrollToBottom(), 100)
  } catch (error) {
    console.error('AI chat failed:', error)
    ElMessage.error('发送失败，请重试')
  } finally {
    loading.value = false
  }
}

const askQuestion = (question: string) => {
  inputMessage.value = question
  sendMessage()
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  padding: 20px;
  height: calc(100vh - 120px);
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
  scroll-behavior: smooth;
  max-height: calc(100vh - 380px);
  min-height: 400px;
}

.message {
  margin-bottom: 20px;
}

.message-content {
  display: flex;
  gap: 10px;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-text {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 8px;
  line-height: 1.6;
}

.message.user .message-text {
  background-color: #409EFF;
  color: white;
}

.message.assistant .message-text {
  background-color: white;
  color: #303133;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chat-input {
  margin-bottom: 15px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
