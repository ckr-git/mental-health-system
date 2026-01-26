<template>
  <div class="register-container">
    <!-- 装饰背景 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="register-box">
      <!-- Logo区域 -->
      <div class="logo-section">
        <div class="logo-icon">🌸</div>
        <h1 class="title">加入心灵花园</h1>
        <p class="subtitle">开启你的心理健康之旅</p>
      </div>

      <el-form :model="registerForm" :rules="rules" ref="formRef" class="register-form">
        <!-- 注册类型选择 -->
        <el-form-item prop="userType">
          <div class="type-selector">
            <div
              class="type-option"
              :class="{ active: registerForm.userType === 'PATIENT' }"
              @click="registerForm.userType = 'PATIENT'"
            >
              <span class="type-icon">👤</span>
              <span class="type-label">普通用户</span>
            </div>
            <div
              class="type-option"
              :class="{ active: registerForm.userType === 'DOCTOR' }"
              @click="registerForm.userType = 'DOCTOR'"
            >
              <span class="type-icon">👨‍⚕️</span>
              <span class="type-label">医生入驻</span>
            </div>
          </div>
        </el-form-item>

        <!-- 医生提示 -->
        <el-alert
          v-if="registerForm.userType === 'DOCTOR'"
          title="医生注册需管理员审核，预计1-2天完成"
          type="warning"
          :closable="false"
          class="doctor-alert"
        />

        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="昵称" prefix-icon="Edit" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="手机号（可选）" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱（可选）" prefix-icon="Message" size="large" />
        </el-form-item>

        <!-- 医生专业领域 -->
        <el-form-item prop="specialization" v-if="registerForm.userType === 'DOCTOR'">
          <el-input v-model="registerForm.specialization" placeholder="专业领域（如：临床心理学）" prefix-icon="Briefcase" size="large" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="register-btn" :loading="loading" @click="handleRegister">
            {{ registerForm.userType === 'DOCTOR' ? '提交审核' : '立即注册' }}
          </el-button>
        </el-form-item>
        <div class="links">
          <router-link to="/login">已有账号？立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api'
import type { RegisterForm } from '@/types'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
  userType: 'PATIENT',
  specialization: ''
})

const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码长度不能少于6位'))
  } else {
    callback()
  }
}

const validateConfirmPass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const validateSpecialization = (rule: any, value: any, callback: any) => {
  if (registerForm.userType === 'DOCTOR' && !value) {
    callback(new Error('请输入专业领域'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  userType: [{ required: true, message: '请选择注册类型', trigger: 'change' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, validator: validatePass, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPass, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  specialization: [{ validator: validateSpecialization, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 构建注册数据，将userType映射为role
        const registerData = {
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname,
          phone: registerForm.phone,
          email: registerForm.email,
          role: registerForm.userType, // userType -> role
          specialization: registerForm.specialization
        }

        await authApi.register(registerData)
        if (registerForm.userType === 'DOCTOR') {
          ElMessage.success('提交成功！管理员审核通过后将以短信或邮件通知您，预计1-2天完成')
        } else {
          ElMessage.success('注册成功，请登录')
        }
        router.push('/login')
      } catch (error) {
        console.error('Register failed:', error)
        ElMessage.error('注册失败，请检查网络或重试')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请填写完整信息')
    }
  })
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #FFF5F5 0%, #FFF0E5 50%, #E8F5E9 100%);
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FF6B6B40 0%, #FFE66D40 100%);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #4ECDC440 0%, #44A08D40 100%);
  bottom: -50px;
  left: -50px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #FFE66D40 0%, #FF6B6B40 100%);
  top: 50%;
  left: 10%;
}

.register-box {
  width: 440px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(255, 107, 107, 0.15);
  position: relative;
  z-index: 1;
}

.logo-section {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  font-size: 42px;
  margin-bottom: 8px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #2D3436;
  margin: 0 0 6px 0;
}

.subtitle {
  font-size: 13px;
  color: #636E72;
  margin: 0;
}

.type-selector {
  display: flex;
  gap: 12px;
  width: 100%;
}

.type-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px;
  border: 2px solid #E8E8E8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.type-option:hover {
  border-color: #FFB5B5;
}

.type-option.active {
  border-color: #FF6B6B;
  background: #FFF5F5;
}

.type-icon {
  font-size: 24px;
}

.type-label {
  font-size: 13px;
  color: #636E72;
}

.type-option.active .type-label {
  color: #FF6B6B;
  font-weight: 500;
}

.doctor-alert {
  margin-bottom: 16px;
  border-radius: 8px;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%);
  border: none;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 107, 107, 0.4);
}

.links {
  text-align: center;
  margin-top: 16px;
}

.links a {
  color: #FF6B6B;
  text-decoration: none;
  font-size: 14px;
}

.links a:hover {
  color: #FF8E8E;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 16px;
}
</style>
