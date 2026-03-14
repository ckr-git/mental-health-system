import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { treeHoleApi } from '@/api'

export const speakToIconMap: Record<string, string> = {
  self: '🧘', person: '👤', role: '🎭', thing: '🎈', custom: '✨'
}

export function useTreeHole() {
  const activeTab = ref('active')
  const loading = ref(false)
  const showEditor = ref(false)
  const showDetail = ref(false)
  const selectedId = ref<number>()
  const showArchive = ref(false)
  const activeCategories = ref<string[]>([])

  const activeTreeHoles = ref<any[]>([])
  const archiveTreeHoles = ref<any[]>([])
  const archiveData = ref<Record<string, any[]>>({})
  const canViewArchive = ref(false)

  const currentPage = ref(1)
  const pageSize = ref(9)
  const total = ref(0)

  const totalArchiveCount = computed(() =>
    Object.values(archiveData.value).reduce((sum, arr) => sum + arr.length, 0)
  )

  const archivedCategories = computed(() => {
    const categories: any[] = []
    for (const [key, holes] of Object.entries(archiveData.value)) {
      if (holes?.length > 0) {
        const [type, name] = key.split(':')
        categories.push({
          key, displayName: name || '未分类',
          icon: speakToIconMap[type] || '💬', count: holes.length,
          holes: holes.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
        })
      }
    }
    return categories.sort((a, b) => b.count - a.count)
  })

  const stats = computed(() => ({
    total: activeTreeHoles.value.length + archiveTreeHoles.value.length,
    active: activeTreeHoles.value.length
  }))

  const loadActiveTreeHoles = async () => {
    try {
      loading.value = true
      const res = await treeHoleApi.getActive()
      if (res.code === 200) activeTreeHoles.value = res.data || []
    } catch (error) {
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  const checkArchivePermission = async () => {
    try {
      const res = await treeHoleApi.canViewArchive()
      if (res.code === 200) canViewArchive.value = res.data
    } catch (error) { /* ignore */ }
  }

  const loadArchive = async () => {
    if (!canViewArchive.value) return
    try {
      loading.value = true
      const res = await treeHoleApi.getArchive()
      if (res.code === 200) {
        archiveData.value = res.data || {}
        archiveTreeHoles.value = Object.values(archiveData.value).flat()
      }
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '加载档案馆失败')
    } finally {
      loading.value = false
    }
  }

  const handleTabChange = (tabName: string) => {
    if (tabName === 'archive' && canViewArchive.value) loadArchive()
  }

  const handleEditorSuccess = () => { loadActiveTreeHoles(); checkArchivePermission() }

  const handleView = (id: number) => { selectedId.value = id; showDetail.value = true }

  const handleDelete = async (id: number) => {
    try {
      const res = await treeHoleApi.delete(id)
      if (res.code === 200) {
        ElMessage.success('删除成功'); loadActiveTreeHoles()
        if (showArchive.value) loadArchive()
      } else { ElMessage.error(res.message || '删除失败') }
    } catch (error: any) {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }

  const handleArchiveDelete = async (id: number) => { await handleDelete(id) }

  const getFireflyStyle = (index: number) => {
    const p = [
      { left: '20%', top: '30%', animationDelay: '0s' },
      { left: '60%', top: '50%', animationDelay: '1s' },
      { left: '80%', top: '40%', animationDelay: '2s' },
      { left: '40%', top: '70%', animationDelay: '1.5s' },
      { left: '90%', top: '60%', animationDelay: '2.5s' }
    ]
    return p[index - 1]
  }

  const getLeafStyle = (index: number) => {
    const p = [
      { left: '10%', animationDelay: '0s', animationDuration: '10s' },
      { left: '30%', animationDelay: '3s', animationDuration: '12s' },
      { left: '50%', animationDelay: '6s', animationDuration: '11s' },
      { left: '70%', animationDelay: '9s', animationDuration: '13s' },
      { left: '85%', animationDelay: '4s', animationDuration: '10s' },
      { left: '95%', animationDelay: '7s', animationDuration: '14s' }
    ]
    return p[index - 1]
  }

  watch(showArchive, (v) => { if (v && canViewArchive.value) loadArchive() })

  onMounted(() => { loadActiveTreeHoles(); checkArchivePermission() })

  return {
    activeTab, loading, showEditor, showDetail, selectedId,
    showArchive, activeCategories,
    activeTreeHoles, archiveTreeHoles, archiveData, canViewArchive,
    currentPage, pageSize, total,
    totalArchiveCount, archivedCategories, stats,
    loadActiveTreeHoles, handleTabChange, handleEditorSuccess,
    handleView, handleDelete, handleArchiveDelete,
    getFireflyStyle, getLeafStyle
  }
}
