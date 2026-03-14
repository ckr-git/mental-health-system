import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { symptomApi, commentApi } from '@/api'
import { playSound } from '@/utils/soundService'
import { useUserStore } from '@/stores/user'
import { moodEmojis } from './helpers'

export function useMoodDiary() {
  const userStore = useUserStore()
  const lightMode = ref<'day' | 'night'>('day')
  const diaries = ref<any[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(9)
  const total = ref(0)

  const showAddDialog = ref(false)
  const submitting = ref(false)
  const newDiary = ref({
    moodScore: 5, moodEmoji: '😐', title: '', content: '',
    energyLevel: 5, sleepQuality: 5, stressLevel: 5
  })

  const showDetailDialog = ref(false)
  const currentDiary = ref<any>(null)

  const currentWeather = computed(() => {
    if (diaries.value.length > 0) {
      const recent = diaries.value.slice(0, 3)
      const avgScore = recent.reduce((sum: number, d: any) => sum + (d.moodScore || 5), 0) / recent.length
      if (avgScore <= 2) return 'storm'
      if (avgScore <= 4) return 'rain'
      if (avgScore <= 6) return 'cloudy'
      if (avgScore <= 8) return 'sunny'
      return 'clear'
    }
    if (diaries.value.length > 0) return diaries.value[0].weatherType || 'sunny'
    return 'sunny'
  })

  const hasDimensions = computed(() => {
    if (!currentDiary.value) return false
    return currentDiary.value.energyLevel || currentDiary.value.sleepQuality || currentDiary.value.stressLevel
  })

  const toggleLight = () => {
    lightMode.value = lightMode.value === 'day' ? 'night' : 'day'
    ElMessage.success(lightMode.value === 'day' ? '开灯啦 💡' : '关灯啦 🌙')
  }

  const selectMood = (score: number, emoji: string) => {
    newDiary.value.moodScore = Number(score)
    newDiary.value.moodEmoji = emoji
  }

  const updateMoodEmoji = (score: number | string) => {
    const numScore = Number(score)
    newDiary.value.moodScore = numScore
    newDiary.value.moodEmoji = moodEmojis[numScore]
  }

  const resetForm = () => {
    newDiary.value = {
      moodScore: 5, moodEmoji: '😐', title: '', content: '',
      energyLevel: 5, sleepQuality: 5, stressLevel: 5
    }
  }

  const loadDiaries = async () => {
    try {
      loading.value = true
      const res = await symptomApi.getList({ pageNum: currentPage.value, pageSize: pageSize.value })
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

  const handleAddDiary = async () => {
    if (!newDiary.value.moodScore) { ElMessage.warning('请选择心情评分'); return }
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

  const handleStatusChange = async (newStatus: string) => {
    if (!currentDiary.value) return
    try {
      const res = await symptomApi.updateRecord({ ...currentDiary.value, status: newStatus })
      if (res.code === 200) {
        ElMessage.success('状态已更新！')
        loadDiaries()
      }
    } catch (error) {
      console.error('Failed to update status:', error)
      ElMessage.error('状态更新失败')
      if (currentDiary.value) {
        const original = diaries.value.find(d => d.id === currentDiary.value?.id)
        if (original) currentDiary.value.status = original.status
      }
    }
  }

  return {
    userStore, lightMode, diaries, loading, currentPage, pageSize, total,
    showAddDialog, submitting, newDiary, showDetailDialog, currentDiary,
    currentWeather, hasDimensions,
    toggleLight, selectMood, updateMoodEmoji, resetForm,
    loadDiaries, handleAddDiary, handleStatusChange
  }
}
