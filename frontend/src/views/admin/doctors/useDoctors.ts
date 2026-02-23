import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api'
import type { UserInfo } from '@/types'

export function useDoctors() {
  const loading = ref(false)
  const saveLoading = ref(false)
  const statsLoading = ref(false)
  const assignmentLoading = ref(false)
  const activeTab = ref('all')
  const searchKeyword = ref('')
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const doctorList = ref<UserInfo[]>([])
  const assignmentRequests = ref<any[]>([])

  const editDialogVisible = ref(false)
  const statsDialogVisible = ref(false)
  const editFormRef = ref<FormInstance>()
  const editForm = reactive<Partial<UserInfo>>({
    id: undefined, nickname: '', specialization: '', phone: '', email: '', gender: 0, age: undefined
  })
  const editRules: FormRules = {
    nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
  }

  const currentStats = reactive<Record<string, any>>({
    doctorId: null, doctorName: '', specialization: '', status: null,
    patientCount: 0, consultationCount: 0, reportCount: 0, averageRating: 0
  })

  const statistics = computed(() => ({
    all: doctorList.value.length,
    pending: doctorList.value.filter(d => d.status === 0).length,
    approved: doctorList.value.filter(d => d.status === 1).length,
    rejected: doctorList.value.filter(d => d.status === 2).length
  }))

  const fetchDoctorList = async () => {
    loading.value = true
    try {
      const params: any = { pageNum: pageNum.value, pageSize: pageSize.value, role: 'DOCTOR' }
      if (searchKeyword.value) params.keyword = searchKeyword.value
      const res = await adminApi.getUsers(params)
      if (res.code === 200 && res.data) {
        const allDoctors = res.data.records || []
        const filterMap: Record<string, number> = { pending: 0, approved: 1, rejected: 2 }
        doctorList.value = activeTab.value in filterMap
          ? allDoctors.filter(d => d.status === filterMap[activeTab.value])
          : allDoctors
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('获取医生列表失败')
    } finally {
      loading.value = false
    }
  }

  const handleTabChange = () => {
    pageNum.value = 1
    activeTab.value === 'patientAssignments' ? fetchAssignmentRequests() : fetchDoctorList()
  }

  const handleSearch = () => { pageNum.value = 1; fetchDoctorList() }

  const handleApprove = async (doctor: UserInfo, approve: boolean) => {
    const action = approve ? '通过' : '拒绝'
    try {
      await ElMessageBox.confirm(
        approve
          ? `确定通过医生 "${doctor.nickname}" 的审核吗？通过后该医生将可以正常登录使用系统。`
          : `确定拒绝医生 "${doctor.nickname}" 的审核吗？拒绝后该医生将无法登录系统。`,
        `${action}审核`, { type: approve ? 'success' : 'warning' }
      )
      const res = await adminApi.approveDoctor(doctor.id!, approve)
      if (res.code === 200) { ElMessage.success(res.data || `${action}成功`); fetchDoctorList() }
      else ElMessage.error(res.message || `${action}失败`)
    } catch (error) {
      if (error !== 'cancel') ElMessage.error(`${action}失败`)
    }
  }

  const handleToggleStatus = async (doctor: UserInfo) => {
    const isActive = doctor.status === 1
    const action = isActive ? '禁用' : '启用'
    try {
      await ElMessageBox.confirm(
        isActive
          ? `确定要禁用医生 "${doctor.nickname}" 吗？禁用后该医生将无法登录系统。`
          : `确定要启用医生 "${doctor.nickname}" 吗？启用后该医生将可以正常登录使用系统。`,
        `${action}账户`, { type: 'warning' }
      )
      const res = await adminApi.toggleUserStatus(doctor.id!)
      if (res.code === 200) { ElMessage.success(`${action}成功`); fetchDoctorList() }
      else ElMessage.error(res.message || `${action}失败`)
    } catch (error) {
      if (error !== 'cancel') ElMessage.error(`${action}失败`)
    }
  }

  const handleEdit = (doctor: UserInfo) => {
    Object.assign(editForm, {
      id: doctor.id, nickname: doctor.nickname, specialization: doctor.specialization || '',
      phone: doctor.phone || '', email: doctor.email || '', gender: doctor.gender || 0, age: doctor.age || undefined
    })
    editDialogVisible.value = true
  }

  const handleSaveEdit = async () => {
    if (!editFormRef.value) return
    await editFormRef.value.validate(async (valid) => {
      if (!valid) return
      saveLoading.value = true
      try {
        const res = await adminApi.updateDoctor(editForm.id!, editForm)
        if (res.code === 200) { ElMessage.success('更新成功'); editDialogVisible.value = false; fetchDoctorList() }
        else ElMessage.error(res.message || '更新失败')
      } catch (error) {
        ElMessage.error('更新失败')
      } finally {
        saveLoading.value = false
      }
    })
  }

  const handleViewStatistics = async (doctor: UserInfo) => {
    statsDialogVisible.value = true
    statsLoading.value = true
    try {
      const res = await adminApi.getDoctorStatistics(doctor.id!)
      if (res.code === 200 && res.data) Object.assign(currentStats, res.data)
    } catch (error) {
      ElMessage.error('获取统计数据失败')
    } finally {
      statsLoading.value = false
    }
  }

  const fetchAssignmentRequests = async () => {
    assignmentLoading.value = true
    try {
      const res = await adminApi.getPendingAssignments({ pageNum: pageNum.value, pageSize: pageSize.value })
      if (res.code === 200 && res.data) {
        assignmentRequests.value = res.data.records || []
        total.value = res.data.total || 0
      }
    } catch (error) {
      ElMessage.error('获取患者分配请求失败')
    } finally {
      assignmentLoading.value = false
    }
  }

  const handleApproveAssignment = async (request: any) => {
    const op = request.operationType === 'claim' ? '认领' : '释放'
    try {
      await ElMessageBox.confirm(`确定通过此${op}申请吗？`, '审核通过', { type: 'success' })
      const res = await adminApi.approveAssignment(request.id)
      if (res.code === 200) { ElMessage.success('审核通过'); fetchAssignmentRequests() }
      else ElMessage.error(res.message || '审核失败')
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('审核失败')
    }
  }

  const handleRejectAssignment = async (request: any) => {
    const op = request.operationType === 'claim' ? '认领' : '释放'
    try {
      const { value: adminNote } = await ElMessageBox.prompt(
        `请输入拒绝理由（${op}申请）`, '审核拒绝',
        { inputType: 'textarea', inputPlaceholder: '请输入拒绝理由', inputValidator: (v) => (!v?.trim() ? '请输入拒绝理由' : true) }
      )
      const res = await adminApi.rejectAssignment(request.id, { adminNote })
      if (res.code === 200) { ElMessage.success('已拒绝申请'); fetchAssignmentRequests() }
      else ElMessage.error(res.message || '拒绝失败')
    } catch (error) {
      if (error !== 'cancel') ElMessage.error('拒绝失败')
    }
  }

  const formatDateTime = (dateTime: string) => dateTime ? dateTime.replace('T', ' ').substring(0, 16) : '暂无'

  return {
    loading, saveLoading, statsLoading, assignmentLoading,
    activeTab, searchKeyword, pageNum, pageSize, total,
    doctorList, assignmentRequests,
    editDialogVisible, statsDialogVisible, editFormRef, editForm, editRules,
    currentStats, statistics,
    fetchDoctorList, handleTabChange, handleSearch,
    handleApprove, handleToggleStatus, handleEdit, handleSaveEdit,
    handleViewStatistics, fetchAssignmentRequests,
    handleApproveAssignment, handleRejectAssignment, formatDateTime
  }
}
