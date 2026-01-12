<template>
  <div class="register-container">
    <div class="register-box">
      <h2 class="title">用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="formRef" class="register-form">
        <!-- 注册类型选择 -->
        <el-form-item prop="userType">
          <el-radio-group v-model="registerForm.userType" size="large" style="width: 100%">
            <el-radio-button label="PATIENT" style="width: 50%">
              <span>👤 普通注册</span>
            </el-radio-button>
            <el-radio-button label="DOCTOR" style="width: 50%">
              <span>👨‍⚕️ 医生注册</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 提示信息 -->
        <el-alert
          v-if="registerForm.userType === 'DOCTOR'"
          title="医生注册需要管理员审核，审核通过后方可登录，预计1-2天完成审核"
          type="warning"
          :closable="false"
          style="margin-bottom: 15px"
        />

        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" prefix-icon="Edit" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号（可选）" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱（可选）" prefix-icon="Message" size="large" />
        </el-form-item>

        <!-- 医生专业领域（仅医生注册时显示） -->
        <el-form-item prop="specialization" v-if="registerForm.userType === 'DOCTOR'">
          <el-input v-model="registerForm.specialization" placeholder="请输入专业领域（如：临床心理学、认知行为疗法等）" prefix-icon="Briefcase" size="large" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleRegister">
            {{ registerForm.userType === 'DOCTOR' ? '提交审核' : '注册' }}
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
  width: 450px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.register-form {
  margin-top: 20px;
}

.links {
  text-align: center;
  margin-top: 10px;
}

.links a {
  color: #667eea;
  text-decoration: none;
}

.links a:hover {
  text-decoration: underline;
}
</style>
