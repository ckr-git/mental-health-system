import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { soundService, setSoundEnabled, setVolume } from '@/utils/soundService'
import type { User } from '@/types'

export function useProfile() {
  const userStore = useUserStore()
  const userInfo = ref<User>({} as User)
  const stats = ref({ symptomsCount: 0, reportsCount: 0, daysCount: 0 })
  const editing = ref(false)
  const formData = reactive<Partial<User>>({})

  const showPasswordDialog = ref(false)
  const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const passwordFormRef = ref()
  const passwordLoading = ref(false)

  const showPhoneDialog = ref(false)
  const phoneForm = reactive({ newPhone: '', code: '' })
  const phoneFormRef = ref()
  const phoneLoading = ref(false)
  const countdown = ref(0)

  const soundSettings = reactive({
    ambientEnabled: soundService.isAmbientEnabled(),
    interactionEnabled: soundService.isInteractionEnabled(),
    volume: Math.round(soundService.getVolume() * 100)
  })

  const passwordRules = {
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请再次输入密码', trigger: 'blur' },
      {
        validator: (_: any, value: string, callback: Function) => {
          callback(value !== passwordForm.newPassword ? new Error('两次输入密码不一致') : undefined)
        },
        trigger: 'blur'
      }
    ]
  }

  const loadProfile = async () => {
    try {
      const { data } = await userApi.getProfile()
      userInfo.value = data
      Object.assign(formData, data)
    } catch { ElMessage.error('加载个人信息失败') }
  }

  const loadStats = async () => {
    try { const { data } = await userApi.getStats(); stats.value = data }
    catch { /* ignore */ }
  }

  const saveProfile = async () => {
    try {
      await userApi.updateProfile(formData)
      ElMessage.success('保存成功'); editing.value = false; loadProfile()
    } catch { ElMessage.error('保存失败') }
  }

  const cancelEdit = () => { editing.value = false; Object.assign(formData, userInfo.value) }

  const handleAvatarUpload = async (file: File) => {
    try {
      const fd = new FormData(); fd.append('file', file)
      const { data } = await userApi.uploadAvatar(fd)
      userInfo.value.avatar = data.url; ElMessage.success('头像上传成功')
    } catch { ElMessage.error('头像上传失败') }
    return false
  }

  const changePassword = async () => {
    if (!passwordFormRef.value) return
    await passwordFormRef.value.validate(async (valid: boolean) => {
      if (!valid) return
      passwordLoading.value = true
      try {
        await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
        ElMessage.success('密码修改成功，请重新登录')
        showPasswordDialog.value = false
        setTimeout(() => { userStore.logout() }, 1500)
      } catch { ElMessage.error('密码修改失败') }
      finally { passwordLoading.value = false }
    })
  }

  const sendCode = async () => {
    if (!phoneForm.newPhone) { ElMessage.warning('请输入手机号'); return }
    try {
      await authApi.sendCode(phoneForm.newPhone)
      ElMessage.success('验证码已发送'); countdown.value = 60
      const timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000)
    } catch { ElMessage.error('发送验证码失败') }
  }

  const changePhone = async () => {
    if (!phoneForm.newPhone || !phoneForm.code) { ElMessage.warning('请填写完整信息'); return }
    phoneLoading.value = true
    try {
      await authApi.changePhone({ newPhone: phoneForm.newPhone, code: phoneForm.code })
      ElMessage.success('手机号修改成功'); showPhoneDialog.value = false; loadProfile()
    } catch { ElMessage.error('手机号修改失败') }
    finally { phoneLoading.value = false }
  }

  const handleSoundSettingChange = () => {
    setSoundEnabled(soundSettings.ambientEnabled, soundSettings.interactionEnabled)
    ElMessage.success('音效设置已保存')
  }

  const handleVolumeChange = (value: number) => { setVolume(value / 100) }

  return {
    userStore, userInfo, stats, editing, formData,
    showPasswordDialog, passwordForm, passwordFormRef, passwordLoading,
    showPhoneDialog, phoneForm, phoneFormRef, phoneLoading, countdown,
    soundSettings, passwordRules,
    loadProfile, loadStats, saveProfile, cancelEdit,
    handleAvatarUpload, changePassword, sendCode, changePhone,
    handleSoundSettingChange, handleVolumeChange
  }
}
