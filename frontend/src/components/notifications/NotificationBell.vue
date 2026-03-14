<template>
  <div class="notification-bell-wrapper">
    <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" :max="99">
      <el-button circle size="small" @click="toggleDrawer" class="bell-btn">
        <el-icon :size="18"><Bell /></el-icon>
      </el-button>
    </el-badge>

    <el-drawer
      v-model="notificationStore.drawerOpen"
      title="通知中心"
      direction="rtl"
      size="360px"
    >
      <template #header>
        <div class="drawer-header">
          <span>通知中心</span>
          <el-button text size="small" @click="handleMarkAllRead" v-if="notificationStore.unreadCount > 0">
            全部已读
          </el-button>
        </div>
      </template>

      <div class="notification-list" v-loading="notificationStore.loading">
        <div
          v-for="n in notificationStore.recent"
          :key="n.id"
          class="notification-item"
          :class="{ unread: n.readStatus === 0 }"
          @click="handleClick(n)"
        >
          <div class="notif-icon" :class="'cat-' + n.category.toLowerCase()">
            {{ getCategoryIcon(n.category) }}
          </div>
          <div class="notif-content">
            <div class="notif-title">{{ n.title }}</div>
            <div class="notif-body">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createTime) }}</div>
          </div>
          <div class="notif-dot" v-if="n.readStatus === 0" />
        </div>
        <el-empty v-if="notificationStore.recent.length === 0" description="暂无通知" :image-size="60" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import type { UserNotificationDTO } from '@/api/modules/notification'

const notificationStore = useNotificationStore()
const router = useRouter()

onMounted(() => {
  notificationStore.bootstrap()
})

const toggleDrawer = () => {
  notificationStore.drawerOpen = !notificationStore.drawerOpen
  if (notificationStore.drawerOpen) {
    notificationStore.loadMore()
  }
}

const handleClick = async (n: UserNotificationDTO) => {
  if (n.readStatus === 0) {
    await notificationStore.markRead(n.id)
  }
  if (n.actionType === 'ROUTE' && n.actionPayload) {
    try {
      const payload = JSON.parse(n.actionPayload)
      if (payload.route) {
        notificationStore.drawerOpen = false
        router.push(payload.route)
      }
    } catch { /* ignore */ }
  }
}

const handleMarkAllRead = () => {
  notificationStore.markAllRead()
}

const getCategoryIcon = (cat: string) => {
  const map: Record<string, string> = {
    CRISIS: '🚨', APPOINTMENT: '📅', ASSESSMENT: '📋', SYSTEM: '🔔'
  }
  return map[cat] || '🔔'
}

const formatTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.bell-btn {
  border: none;
  background: transparent;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.notification-item:hover {
  background: #F8F9FA;
}

.notification-item.unread {
  background: #FFF5F5;
}

.notif-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F0F0F0;
}

.cat-crisis .notif-icon, .notif-icon.cat-crisis { background: #FADBD8; }
.cat-appointment .notif-icon { background: #D5F5E3; }
.cat-assessment .notif-icon { background: #D6EAF8; }

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 14px;
  font-weight: 500;
  color: #2D3436;
  margin-bottom: 4px;
}

.notif-body {
  font-size: 12px;
  color: #636E72;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-time {
  font-size: 11px;
  color: #B2BEC3;
  margin-top: 4px;
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #FF6B6B;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
