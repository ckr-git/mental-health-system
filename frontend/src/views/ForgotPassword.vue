<template>
  <div class="forgot-container">
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
    </div>
    <div class="forgot-box">
      <div class="logo-section">
        <div class="logo-icon">🔑</div>
        <h1 class="title">找回密码</h1>
      </div>

      <el-steps :active="step" align-center style="margin-bottom: 24px">
        <el-step title="账号验证" />
        <el-step title="输入验证码" />
        <el-step title="设置新密码" />
      </el-steps>

      <!-- Step 0: Username -->
      <el-form v-if="step === 0" :model="form" ref="step0Ref" :rules="rules0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="action-btn" :loading="loading" @click="sendCode">
          发送验证码
        </el-button>
      </el-form>

      <!-- Step 1: Code -->
      <el-form v-if="step === 1" :model="form" ref="step1Ref" :rules="rules1">
        <el-form-item prop="code">
          <el-input v-model="form.code" placeholder="请输入验证码" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="action-btn" @click="verifyCode">下一步</el-button>
      </el-form>

      <!-- Step 2: New Password -->
      <el-form v-if="step === 2" :model="form" ref="step2Ref" :rules="rules2">
        <el-form-item prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="action-btn" :loading="loading" @click="resetPassword">
          重置密码
        </el-button>
      </el-form>

      <div class="links">
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const step0Ref = ref<FormInstance>()
const step1Ref = ref<FormInstance>()
const step2Ref = ref<FormInstance>()

const form = reactive({
  username: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const rules0: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}
const rules1: FormRules = {
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}
const rules2: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_r: any, v: string, cb: any) => v === form.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

const sendCode = async () => {
  await step0Ref.value?.validate()
  loading.value = true
  try {
    await authApi.sendResetCode(form.username)
    ElMessage.success('如果账号存在，验证码已发送')
    step.value = 1
  } catch { /* handled by interceptor */ } finally { loading.value = false }
}

const verifyCode = async () => {
  await step1Ref.value?.validate()
  step.value = 2
}

const resetPassword = async () => {
  await step2Ref.value?.validate()
  loading.value = true
  try {
    await authApi.resetPassword({ username: form.username, code: form.code, newPassword: form.newPassword })
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
  } catch { /* handled by interceptor */ } finally { loading.value = false }
}
</script>

<style scoped>
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #FFF5F5 0%, #FFF0E5 50%, #E8F5E9 100%);
  position: relative;
  overflow: hidden;
}
.bg-decoration { position: absolute; inset: 0; pointer-events: none; }
.circle { position: absolute; border-radius: 50%; opacity: 0.6; }
.circle-1 { width: 400px; height: 400px; background: linear-gradient(135deg, #FF6B6B40, #FFE66D40); top: -100px; right: -100px; }
.circle-2 { width: 300px; height: 300px; background: linear-gradient(135deg, #4ECDC440, #44A08D40); bottom: -50px; left: -50px; }
.forgot-box {
  width: 440px;
  padding: 48px 40px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(255,107,107,0.15);
  position: relative;
  z-index: 1;
}
.logo-section { text-align: center; margin-bottom: 24px; }
.logo-icon { font-size: 48px; margin-bottom: 8px; }
.title { font-size: 24px; font-weight: 700; color: #2D3436; margin: 0; }
.action-btn {
  width: 100%; height: 48px; font-size: 16px; font-weight: 600;
  border-radius: 12px; background: linear-gradient(135deg, #FF6B6B, #FF8E8E); border: none;
}
.action-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(255,107,107,0.4); }
.links { text-align: center; margin-top: 16px; }
.links a { color: #FF6B6B; text-decoration: none; font-size: 14px; }
.links a:hover { color: #FF8E8E; }
:deep(.el-input__wrapper) { border-radius: 12px; padding: 4px 16px; }
</style>
