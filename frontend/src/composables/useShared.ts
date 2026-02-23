import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export function usePagedList<T = any>(fetchFn: (params: { pageNum: number; pageSize: number }) => Promise<any>, defaultPageSize = 10) {
  const list = ref<T[]>([]) as { value: T[] }
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  const load = async () => {
    loading.value = true
    try {
      const res = await fetchFn({ pageNum: page.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        list.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch {
      ElMessage.error('加载列表失败')
    } finally {
      loading.value = false
    }
  }

  return { list, loading, page, pageSize, total, load }
}

export function useConfirmAction(reloadFn: () => void) {
  return async (apiCall: () => Promise<any>, opts: { confirmMsg: string; confirmTitle?: string; successMsg?: string; type?: 'warning' | 'error' | 'success' }) => {
    try {
      await ElMessageBox.confirm(opts.confirmMsg, opts.confirmTitle || '提示', { type: opts.type || 'warning' })
      const res = await apiCall()
      if (res.code === 200) {
        ElMessage.success(opts.successMsg || res.data || '操作成功')
        reloadFn()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('操作失败')
    }
  }
}
