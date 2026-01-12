<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <h3>医生工作台</h3>
      </div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/doctor/dashboard">
          <el-icon><House /></el-icon>
          <span>📈 工作台</span>
        </el-menu-item>
        <el-menu-item index="/doctor/patients">
          <el-icon><User /></el-icon>
          <span>👥 患者管理</span>
        </el-menu-item>
        <el-menu-item index="/doctor/patient-pool">
          <el-icon><User /></el-icon>
          <span>🌊 患者公海</span>
        </el-menu-item>
        <el-menu-item index="/doctor/reports">
          <el-icon><Document /></el-icon>
          <span>📋 评估报告</span>
        </el-menu-item>
        <el-menu-item index="/doctor/chat">
          <el-icon><ChatLineRound /></el-icon>
          <span>💬 在线咨询</span>
        </el-menu-item>
        <el-menu-item index="/doctor/consultations">
          <el-icon><ChatLineRound /></el-icon>
          <span>💼 咨询管理</span>
        </el-menu-item>
        <el-menu-item index="/doctor/appointments">
          <el-icon><Calendar /></el-icon>
          <span>📅 预约管理</span>
        </el-menu-item>
        <el-menu-item index="/doctor/profile">
          <el-icon><Setting /></el-icon>
          <span>⚙️ 个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header>
        <div class="header-content">
          <span class="welcome">欢迎，{{ userStore.userInfo?.nickname }} 医生</span>
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              <el-avatar :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await authApi.logout()
      userStore.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
}

.logo h3 {
  margin: 0;
  color: #fff;
  font-size: 16px;
}

.el-menu {
  border-right: none;
  background-color: #304156;
}

:deep(.el-menu-item) {
  color: #bfcbd9;
}

:deep(.el-menu-item:hover),
:deep(.el-menu-item.is-active) {
  background-color: #263445 !important;
  color: #409eff !important;
}

.el-header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.welcome {
  color: #606266;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>
