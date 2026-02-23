import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

export function useMessages() {
  const loading = ref(false)
  const pageSize = ref(10)

  const statistics = reactive({
    totalNotifications: 0,
    totalFeedback: 0,
    unhandledAlerts: 0,
    publishedAnnouncements: 0
  })

  const loadStatistics = async () => {
    try {
      const res = await adminApi.getMessageStatistics()
      if (res.code === 200 && res.data) {
        Object.assign(statistics, res.data)
      }
    } catch (error) {
      console.error('Failed to load statistics:', error)
    }
  }

  return { loading, pageSize, statistics, loadStatistics }
}

export function useNotifications(pageSize: ReturnType<typeof ref<number>>, loading: ReturnType<typeof ref<boolean>>, loadStatistics: () => Promise<void>) {
  const list = ref<any[]>([])
  const page = ref(1)
  const total = ref(0)
  const dialogVisible = ref(false)
  const form = reactive<any>({
    id: null, notificationType: 'SYSTEM', title: '', content: '', priority: 'NORMAL'
  })

  const load = async () => {
    loading.value = true
    try {
      const res = await adminApi.getNotifications({ pageNum: page.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('加载通知列表失败')
    } finally {
      loading.value = false
    }
  }

  const showDialog = (notification?: any) => {
    if (notification) {
      Object.assign(form, notification)
    } else {
      Object.assign(form, { id: null, notificationType: 'SYSTEM', title: '', content: '', priority: 'NORMAL' })
    }
    dialogVisible.value = true
  }

  const save = async () => {
    try {
      const res = form.id
        ? await adminApi.updateNotification(form.id, form)
        : await adminApi.createNotification(form)
      if (res.code === 200) {
        ElMessage.success(res.data || '操作成功')
        dialogVisible.value = false
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }

  const send = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要发送此通知吗？', '提示', { type: 'warning' })
      const res = await adminApi.sendNotification(id)
      if (res.code === 200) {
        ElMessage.success('发送成功')
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '发送失败')
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('发送失败')
    }
  }

  const remove = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除此通知吗？', '警告', { type: 'error' })
      const res = await adminApi.deleteNotification(id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败')
    }
  }

  return { list, page, total, dialogVisible, form, load, showDialog, save, send, remove }
}

export function useFeedback(pageSize: ReturnType<typeof ref<number>>, loading: ReturnType<typeof ref<boolean>>, loadStatistics: () => Promise<void>) {
  const list = ref<any[]>([])
  const page = ref(1)
  const total = ref(0)
  const dialogVisible = ref(false)
  const current = ref<any>(null)
  const replyDialogVisible = ref(false)
  const replyContent = ref('')
  const currentReplyId = ref<number | null>(null)

  const load = async () => {
    loading.value = true
    try {
      const res = await adminApi.getFeedback({ pageNum: page.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('加载反馈列表失败')
    } finally {
      loading.value = false
    }
  }

  const view = async (feedback: any) => {
    try {
      const res = await adminApi.getFeedbackDetail(feedback.id)
      if (res.code === 200 && res.data) {
        current.value = res.data
        dialogVisible.value = true
      }
    } catch (error) {
      ElMessage.error('加载反馈详情失败')
    }
  }

  const startReply = (feedback: any) => {
    currentReplyId.value = feedback.id
    replyContent.value = ''
    replyDialogVisible.value = true
  }

  const submitReply = async () => {
    if (!replyContent.value.trim()) { ElMessage.warning('请输入回复内容'); return }
    try {
      const res = await adminApi.replyFeedback(currentReplyId.value!, replyContent.value)
      if (res.code === 200) {
        ElMessage.success('回复成功')
        replyDialogVisible.value = false
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '回复失败')
      }
    } catch (error) {
      ElMessage.error('回复失败')
    }
  }

  const remove = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除此反馈吗？', '警告', { type: 'error' })
      const res = await adminApi.deleteFeedback(id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败')
    }
  }

  return { list, page, total, dialogVisible, current, replyDialogVisible, replyContent, load, view, startReply, submitReply, remove }
}

export function useAlerts(pageSize: ReturnType<typeof ref<number>>, loading: ReturnType<typeof ref<boolean>>, loadStatistics: () => Promise<void>) {
  const list = ref<any[]>([])
  const page = ref(1)
  const total = ref(0)
  const dialogVisible = ref(false)
  const current = ref<any>(null)
  const handleNote = ref('')
  const handleStatus = ref(2)

  const load = async () => {
    loading.value = true
    try {
      const res = await adminApi.getAlerts({ pageNum: page.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('加载警报列表失败')
    } finally {
      loading.value = false
    }
  }

  const view = async (alert: any) => {
    try {
      const res = await adminApi.getAlertDetail(alert.id)
      if (res.code === 200 && res.data) {
        current.value = res.data
        dialogVisible.value = true
      }
    } catch (error) {
      ElMessage.error('加载警报详情失败')
    }
  }

  const startHandle = (alert: any) => {
    current.value = alert
    handleNote.value = ''
    handleStatus.value = 2
    dialogVisible.value = true
  }

  const submitHandle = async () => {
    if (!handleNote.value.trim()) { ElMessage.warning('请输入处理备注'); return }
    try {
      const res = await adminApi.handleAlert(current.value.id, handleNote.value, handleStatus.value)
      if (res.code === 200) {
        ElMessage.success('处理成功')
        dialogVisible.value = false
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '处理失败')
      }
    } catch (error) {
      ElMessage.error('处理失败')
    }
  }

  const remove = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除此警报吗？', '警告', { type: 'error' })
      const res = await adminApi.deleteAlert(id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('删除失败')
    }
  }

  return { list, page, total, dialogVisible, current, handleNote, handleStatus, load, view, startHandle, submitHandle, remove }
}

export function useAnnouncements(pageSize: ReturnType<typeof ref<number>>, loading: ReturnType<typeof ref<boolean>>, loadStatistics: () => Promise<void>) {
  const list = ref<any[]>([])
  const page = ref(1)
  const total = ref(0)
  const dialogVisible = ref(false)
  const form = reactive<any>({
    id: null, announcementType: 'SYSTEM', title: '', content: '', priority: 'NORMAL', targetRoles: ''
  })
  const targetRoles = ref<string[]>([])

  const load = async () => {
    loading.value = true
    try {
      const res = await adminApi.getAnnouncements({ pageNum: page.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('加载公告列表失败')
    } finally {
      loading.value = false
    }
  }

  const showDialog = (announcement?: any) => {
    if (announcement) {
      Object.assign(form, announcement)
      targetRoles.value = announcement.targetRoles ? announcement.targetRoles.split(',') : []
    } else {
      Object.assign(form, { id: null, announcementType: 'SYSTEM', title: '', content: '', priority: 'NORMAL' })
      targetRoles.value = []
    }
    dialogVisible.value = true
  }

  const save = async () => {
    form.targetRoles = targetRoles.value.join(',')
    try {
      const res = form.id
        ? await adminApi.updateAnnouncement(form.id, form)
        : await adminApi.createAnnouncement(form)
      if (res.code === 200) {
        ElMessage.success(res.data || '操作成功')
        dialogVisible.value = false
        load()
        loadStatistics()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }

  const publish = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要发布此公告吗？', '提示', { type: 'warning' })
      const res = await adminApi.publishAnnouncement(id)
      if (res.code === 200) { ElMessage.success('发布成功'); load(); loadStatistics() }
      else ElMessage.error(res.message || '发布失败')
    } catch (error) { if (error !== 'cancel') ElMessage.error('发布失败') }
  }

  const unpublish = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要下线此公告吗？', '提示', { type: 'warning' })
      const res = await adminApi.unpublishAnnouncement(id)
      if (res.code === 200) { ElMessage.success('下线成功'); load(); loadStatistics() }
      else ElMessage.error(res.message || '下线失败')
    } catch (error) { if (error !== 'cancel') ElMessage.error('下线失败') }
  }

  const togglePin = async (id: number) => {
    try {
      const res = await adminApi.toggleAnnouncementPin(id)
      if (res.code === 200) { ElMessage.success('操作成功'); load() }
      else ElMessage.error(res.message || '操作失败')
    } catch (error) { ElMessage.error('操作失败') }
  }

  const remove = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除此公告吗？', '警告', { type: 'error' })
      const res = await adminApi.deleteAnnouncement(id)
      if (res.code === 200) { ElMessage.success('删除成功'); load(); loadStatistics() }
      else ElMessage.error(res.message || '删除失败')
    } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
  }

  return { list, page, total, dialogVisible, form, targetRoles, load, showDialog, save, publish, unpublish, togglePin, remove }
}
