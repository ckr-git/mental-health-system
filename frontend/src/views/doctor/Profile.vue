<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <h2>⚙️ 个人中心</h2>
      </template>

      <el-form :model="form" label-width="100px">
        <el-form-item label="头像">
          <el-avatar :src="form.avatar || '/default-avatar.png'" :size="80" />
          <el-button size="small" style="margin-left: 20px" disabled>更换头像（待开发）</el-button>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="专业领域">
          <el-input v-model="form.specialization" placeholder="请输入专业领域" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
          <el-button @click="loadProfile">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />

      <h3>修改密码</h3>
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="changingPassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const form = ref({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  specialization: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const saving = ref(false)
const changingPassword = ref(false)

const loadProfile = async () => {
  try {
    const { data } = await userApi.getProfile()
    form.value = { ...data }
  } catch (error) {
    ElMessage.error('加载个人信息失败')
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await userApi.updateProfile(form.value)
    ElMessage.success('保存成功')
    userStore.setUserInfo(form.value)
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }

  changingPassword.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    ElMessage.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

h2 {
  margin: 0;
  color: #303133;
}

h3 {
  color: #606266;
  margin-bottom: 20px;
}
</style>
