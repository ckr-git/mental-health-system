<template>
  <div class="communication-container">
    <el-card class="header-card">
      <h2>💬 医患沟通</h2>
      <p style="color: #909399; margin-top: 10px;">选择医生进行在线咨询，获得专业的心理支持</p>
    </el-card>

    <!-- 我的医生卡片 -->
    <el-card class="my-doctor-card" v-loading="myDoctorLoading">
      <template #header>
        <div class="card-header">
          <span>👨‍⚕️ 我的医生</span>
        </div>
      </template>

      <div v-if="myDoctor" class="my-doctor-info">
        <el-avatar :src="myDoctor.avatar || '/default-avatar.png'" :size="60" />
        <div class="doctor-details">
          <div class="doctor-name-main">{{ myDoctor.nickname || myDoctor.username }}</div>
          <div class="doctor-specialization">{{ myDoctor.specialization || '心理咨询师' }}</div>
          <div class="doctor-contact">
            <el-tag v-if="myDoctor.phone" size="small" type="info">{{ myDoctor.phone }}</el-tag>
            <el-tag v-if="myDoctor.email" size="small" type="info" style="margin-left: 10px">{{ myDoctor.email }}</el-tag>
          </div>
        </div>
        <div class="doctor-actions">
          <el-button type="primary" @click="selectDoctor(myDoctor)">开始咨询</el-button>
        </div>
      </div>

      <el-empty v-else description="您还没有分配的专属医生，可以从下方医生列表中选择咨询" :image-size="80" />
    </el-card>

    <el-row :gutter="20">
      <!-- 左侧：医生列表 -->
      <el-col :span="8">
        <el-card class="doctor-list-card">
          <template #header>
            <div class="card-header">
              <span>👨‍⚕️ 医生列表</span>
              <el-input
                v-model="searchKeyword"
                placeholder="搜索医生"
                :prefix-icon="Search"
                size="small"
                style="width: 150px"
              />
            </div>
          </template>

          <div class="doctor-list">
            <div
              v-for="doctor in filteredDoctors"
              :key="doctor.id"
              :class="['doctor-item', { active: selectedDoctor?.id === doctor.id }]"
              @click="selectDoctor(doctor)"
            >
              <el-avatar :src="doctor.avatar || '/default-avatar.png'" :size="50" />
              <div class="doctor-info">
                <div class="doctor-name">{{ doctor.nickname || doctor.username }}</div>
                <div class="doctor-title">{{ doctor.specialization || '心理咨询师' }}</div>
                <div class="doctor-status">
                  <el-tag :type="doctor.status === 1 ? 'success' : 'info'" size="small">
                    {{ doctor.status === 1 ? '在线' : '离线' }}
                  </el-tag>
                </div>
              </div>
            </div>

            <el-empty v-if="filteredDoctors.length === 0" description="暂无医生" :image-size="80" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：聊天区域 -->
      <el-col :span="16">
        <el-card class="chat-card" v-if="selectedDoctor">
          <template #header>
            <div class="chat-header">
              <el-avatar :src="selectedDoctor.avatar || '/default-avatar.png'" :size="40" />
              <div class="chat-header-info">
                <div class="doctor-name">{{ selectedDoctor.nickname || selectedDoctor.username }}</div>
                <div class="doctor-title">{{ selectedDoctor.specialization || '心理咨询师' }}</div>
              </div>
            </div>
          </template>

          <div class="chat-content">
            <div class="messages-area" ref="messagesArea">
              <div
                v-for="msg in messages"
                :key="msg.id"
                :class="['message-item', msg.fromSelf ? 'self' : 'other']"
              >
                <el-avatar :src="msg.fromSelf ? currentUserAvatar : selectedDoctor.avatar" :size="36" />
                <div class="message-content">
                  <div class="message-text">{{ msg.content }}</div>
                  <div class="message-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </div>

              <el-empty v-if="messages.length === 0" description="暂无消息，开始对话吧" :image-size="100" />
            </div>

            <div class="input-area">
              <el-input
                v-model="newMessage"
                type="textarea"
                :rows="3"
                placeholder="输入消息..."
                @keydown.enter.ctrl="sendMessage"
              />
              <div class="input-actions">
                <span class="tip">Ctrl + Enter 发送</span>
                <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="empty-chat-card" v-else>
          <el-empty description="请选择医生开始咨询" :image-size="150" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { userApi, chatApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { UserInfo } from '@/types'

const userStore = useUserStore()

const searchKeyword = ref('')
const doctors = ref<UserInfo[]>([])
const myDoctor = ref<UserInfo | null>(null)
const myDoctorLoading = ref(false)
const selectedDoctor = ref<UserInfo | null>(null)
const messages = ref<any[]>([])
const newMessage = ref('')
const sending = ref(false)
const messagesArea = ref<HTMLElement>()

const currentUserAvatar = computed(() => userStore.userInfo?.avatar || '/default-avatar.png')

const filteredDoctors = computed(() => {
  if (!searchKeyword.value) return doctors.value
  return doctors.value.filter(d =>
    d.nickname?.includes(searchKeyword.value) ||
    d.username?.includes(searchKeyword.value)
  )
})

const loadDoctors = async () => {
  try {
    const { data } = await userApi.getDoctors({ pageNum: 1, pageSize: 100 })
    doctors.value = data.records
  } catch (error) {
    ElMessage.error('加载医生列表失败')
  }
}

const loadMyDoctor = async () => {
  myDoctorLoading.value = true
  try {
    const res = await userApi.getMyDoctor()
    if (res.code === 200 && res.data) {
      myDoctor.value = res.data
    }
  } catch (error) {
    // No assigned doctor yet, that's ok
    console.log('No assigned doctor')
  } finally {
    myDoctorLoading.value = false
  }
}

const selectDoctor = async (doctor: UserInfo) => {
  selectedDoctor.value = doctor
  await loadMessages()
}

const loadMessages = async () => {
  if (!selectedDoctor.value) return

  try {
    const { data } = await chatApi.getMessages(selectedDoctor.value.id!, { pageNum: 1, pageSize: 50 })
    messages.value = data.records.reverse()
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载消息失败')
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim() || !selectedDoctor.value) return

  sending.value = true
  try {
    await chatApi.sendMessage({
      targetUserId: selectedDoctor.value.id!,
      content: newMessage.value,
      type: 'text'
    })

    newMessage.value = ''
    await loadMessages()
    ElMessage.success('发送成功')
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  if (messagesArea.value) {
    messagesArea.value.scrollTop = messagesArea.value.scrollHeight
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`

  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadMyDoctor()
  loadDoctors()
})
</script>

<style scoped>
.communication-container {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
}

.my-doctor-card {
  margin-bottom: 20px;
}

.my-doctor-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.doctor-details {
  flex: 1;
}

.doctor-name-main {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.doctor-specialization {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.doctor-contact {
  margin-top: 8px;
}

.doctor-actions {
  display: flex;
  gap: 10px;
}

.doctor-list-card {
  height: calc(100vh - 240px);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doctor-list {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.doctor-item {
  display: flex;
  align-items: center;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;
}

.doctor-item:hover {
  background-color: #f5f7fa;
  border-color: #409eff;
}

.doctor-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.doctor-info {
  flex: 1;
  margin-left: 15px;
}

.doctor-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.doctor-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 5px;
}

.chat-card,
.empty-chat-card {
  height: calc(100vh - 240px);
}

.chat-header {
  display: flex;
  align-items: center;
}

.chat-header-info {
  margin-left: 15px;
}

.chat-header-info .doctor-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.chat-header-info .doctor-title {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.chat-content {
  height: calc(100vh - 360px);
  display: flex;
  flex-direction: column;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.self {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 60%;
  margin: 0 10px;
}

.message-item.self .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-text {
  padding: 10px 15px;
  border-radius: 8px;
  background-color: #fff;
  word-wrap: break-word;
  line-height: 1.6;
}

.message-item.self .message-text {
  background-color: #409eff;
  color: #fff;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.input-area {
  border-top: 1px solid #e4e7ed;
  padding-top: 15px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.tip {
  font-size: 12px;
  color: #909399;
}

.empty-chat-card {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
