import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

export function useAppointments() {
  const loading = ref(false)
  const saving = ref(false)
  const appointmentDialogVisible = ref(false)
  const cancelDialogVisible = ref(false)

  const statistics = reactive({
    totalAppointments: 0, pendingCount: 0, confirmedCount: 0,
    completedCount: 0, cancelledCount: 0, expiredCount: 0
  })

  const filters = reactive({
    doctorId: '', patientId: '',
    status: undefined as number | undefined, appointmentType: ''
  })

  const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
  const appointmentList = ref<any[]>([])

  const currentAppointment = reactive({
    id: null as number | null, patientId: '', doctorId: '',
    appointmentType: '', appointmentTime: '', duration: 60,
    symptoms: '', notes: ''
  })

  const cancelReason = ref('')
  const currentCancelId = ref<number | null>(null)

  const loadStatistics = async () => {
    try {
      const res = await adminApi.getAppointmentStatistics()
      if (res.code === 200 && res.data) Object.assign(statistics, res.data)
    } catch (error) {
      console.error('Failed to load statistics:', error)
    }
  }

  const loadAppointments = async () => {
    loading.value = true
    try {
      const params = {
        pageNum: pagination.pageNum, pageSize: pagination.pageSize,
        doctorId: filters.doctorId ? Number(filters.doctorId) : undefined,
        patientId: filters.patientId ? Number(filters.patientId) : undefined,
        status: filters.status, appointmentType: filters.appointmentType || undefined
      }
      const res = await adminApi.getAppointments(params)
      if (res.code === 200 && res.data) {
        appointmentList.value = res.data.records
        pagination.total = res.data.total
      }
    } catch (error) {
      ElMessage.error('加载预约列表失败')
    } finally {
      loading.value = false
    }
  }

  const resetFilters = () => {
    filters.doctorId = ''; filters.patientId = ''
    filters.status = undefined; filters.appointmentType = ''
    pagination.pageNum = 1; loadAppointments()
  }

  const showAppointmentDialog = (appointment?: any) => {
    if (appointment) Object.assign(currentAppointment, appointment, { symptoms: appointment.symptoms || '', notes: appointment.notes || '' })
    appointmentDialogVisible.value = true
  }

  const resetAppointmentForm = () => {
    Object.assign(currentAppointment, {
      id: null, patientId: '', doctorId: '', appointmentType: '',
      appointmentTime: '', duration: 60, symptoms: '', notes: ''
    })
  }

  const saveAppointment = async () => {
    if (!currentAppointment.patientId || !currentAppointment.doctorId ||
        !currentAppointment.appointmentType || !currentAppointment.appointmentTime) {
      ElMessage.warning('请填写必填项'); return
    }
    saving.value = true
    try {
      const data = {
        patientId: Number(currentAppointment.patientId), doctorId: Number(currentAppointment.doctorId),
        appointmentType: currentAppointment.appointmentType, appointmentTime: currentAppointment.appointmentTime,
        duration: currentAppointment.duration, symptoms: currentAppointment.symptoms, notes: currentAppointment.notes
      }
      const res = currentAppointment.id
        ? await adminApi.updateAppointment(currentAppointment.id, data)
        : await adminApi.createAppointment(data)
      if (res.code === 200) {
        ElMessage.success(currentAppointment.id ? '更新成功' : '创建成功')
        appointmentDialogVisible.value = false; loadAppointments(); loadStatistics()
      }
    } catch (error) {
      ElMessage.error('操作失败')
    } finally {
      saving.value = false
    }
  }

  const confirmAppointment = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要确认此预约吗？', '提示', { type: 'warning' })
      const res = await adminApi.confirmAppointment(id)
      if (res.code === 200) { ElMessage.success('确认成功'); loadAppointments(); loadStatistics() }
    } catch (error) { if (error !== 'cancel') ElMessage.error('确认失败') }
  }

  const showCancelDialog = (appointment: any) => {
    currentCancelId.value = appointment.id; cancelReason.value = ''; cancelDialogVisible.value = true
  }

  const submitCancel = async () => {
    if (!cancelReason.value.trim()) { ElMessage.warning('请输入取消原因'); return }
    saving.value = true
    try {
      const res = await adminApi.cancelAppointment(currentCancelId.value!, cancelReason.value)
      if (res.code === 200) { ElMessage.success('取消成功'); cancelDialogVisible.value = false; loadAppointments(); loadStatistics() }
    } catch (error) { ElMessage.error('取消失败') } finally { saving.value = false }
  }

  const completeAppointment = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要将此预约标记为已完成吗？', '提示', { type: 'warning' })
      const res = await adminApi.completeAppointment(id)
      if (res.code === 200) { ElMessage.success('操作成功'); loadAppointments(); loadStatistics() }
    } catch (error) { if (error !== 'cancel') ElMessage.error('操作失败') }
  }

  const deleteAppointment = async (id: number) => {
    try {
      await ElMessageBox.confirm('确定要删除此预约吗？此操作不可恢复。', '警告', { type: 'warning' })
      const res = await adminApi.deleteAppointment(id)
      if (res.code === 200) { ElMessage.success('删除成功'); loadAppointments(); loadStatistics() }
    } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
  }

  return {
    loading, saving, appointmentDialogVisible, cancelDialogVisible,
    statistics, filters, pagination, appointmentList,
    currentAppointment, cancelReason, currentCancelId,
    loadStatistics, loadAppointments, resetFilters,
    showAppointmentDialog, resetAppointmentForm, saveAppointment,
    confirmAppointment, showCancelDialog, submitCancel,
    completeAppointment, deleteAppointment
  }
}
