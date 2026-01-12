<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <h3>管理后台</h3>
      </div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>📊 数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>👥 用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/doctors">
          <el-icon><User /></el-icon>
          <span>👨‍⚕️ 医生管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/resources">
          <el-icon><Reading /></el-icon>
          <span>📚 资源管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/appointments">
          <el-icon><Calendar /></el-icon>
          <span>📅 预约管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/messages">
          <el-icon><ChatDotRound /></el-icon>
          <span>🔔 消息中心</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <span>⚙️ 系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header>
        <div class="header-content">
          <span class="welcome">管理员：{{ userStore.userInfo?.nickname }}</span>
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
