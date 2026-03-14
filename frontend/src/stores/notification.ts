import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi, type UserNotificationDTO } from '@/api/modules/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const drawerOpen = ref(false)
  const recent = ref<UserNotificationDTO[]>([])
  const loading = ref(false)

  const bootstrap = async () => {
    try {
      const [countRes, listRes] = await Promise.all([
        notificationApi.getUnreadCount(),
        notificationApi.getList({ pageNum: 1, pageSize: 10 })
      ])
      if (countRes.code === 200) unreadCount.value = countRes.data
      if (listRes.code === 200) recent.value = listRes.data.records
    } catch {
      // silent
    }
  }

  const consumeRealtime = (n: UserNotificationDTO) => {
    const exists = recent.value.find(r => r.id === n.id)
    if (!exists) {
      recent.value.unshift(n)
      if (recent.value.length > 20) recent.value.pop()
      unreadCount.value++
    }
  }

  const markRead = async (id: number) => {
    const res = await notificationApi.markRead(id)
    if (res.code === 200) {
      const item = recent.value.find(r => r.id === id)
      if (item && item.readStatus === 0) {
        item.readStatus = 1
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    }
  }

  const markAllRead = async () => {
    const res = await notificationApi.markAllRead()
    if (res.code === 200) {
      recent.value.forEach(r => r.readStatus = 1)
      unreadCount.value = 0
    }
  }

  const loadMore = async (pageNum = 1) => {
    loading.value = true
    try {
      const res = await notificationApi.getList({ pageNum, pageSize: 20 })
      if (res.code === 200) {
        recent.value = res.data.records
      }
    } finally {
      loading.value = false
    }
  }

  return { unreadCount, drawerOpen, recent, loading, bootstrap, consumeRealtime, markRead, markAllRead, loadMore }
})
