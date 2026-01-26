<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">🛡️</div>
        <h3>管理后台</h3>
      </div>

      <el-menu :default-active="activeMenu" router class="sidebar-menu">
        <el-menu-item index="/admin/dashboard">
          <div class="menu-icon">📊</div>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <div class="menu-icon">👥</div>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/doctors">
          <div class="menu-icon">👨‍⚕️</div>
          <span>医生管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/resources">
          <div class="menu-icon">📚</div>
          <span>资源管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/appointments">
          <div class="menu-icon">📅</div>
          <span>预约管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/messages">
          <div class="menu-icon">🔔</div>
          <span>消息中心</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <div class="menu-icon">⚙️</div>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>

      <!-- 底部装饰 -->
      <div class="sidebar-footer">
        <div class="status-tip">系统运行正常</div>
      </div>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <span class="greeting">{{ getGreeting() }}，{{ userStore.userInfo?.nickname || '管理员' }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar
                :size="36"
                :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
              />
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>系统设置
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '夜深了'
}

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
  } else if (command === 'settings') {
    router.push('/admin/settings')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #FAFAFA;
}

/* 侧边栏 */
.sidebar {
  background: linear-gradient(180deg, #EEF2F7 0%, #FFFFFF 100%);
  border-right: 1px solid #E4E7ED;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, #5B8DEF 0%, #3D5AFE 100%);
}

.logo-icon {
  font-size: 28px;
}

.logo h3 {
  margin: 0;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
}

/* 菜单样式 */
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 12px 8px;
}

:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 4px 0;
  border-radius: 12px;
  color: #636E72;
  transition: all 0.25s ease;
}

:deep(.el-menu-item:hover) {
  background: #EEF2F7 !important;
  color: #5B8DEF !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #5B8DEF 0%, #3D5AFE 100%) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(91, 141, 239, 0.3);
}

.menu-icon {
  font-size: 18px;
  margin-right: 12px;
  width: 24px;
  text-align: center;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 20px;
  text-align: center;
}

.status-tip {
  padding: 12px 16px;
  background: linear-gradient(135deg, #5B8DEF20 0%, #3D5AFE10 100%);
  border-radius: 12px;
  color: #5B8DEF;
  font-size: 13px;
  font-weight: 500;
}

/* 头部 */
.header {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
}

.greeting {
  font-size: 16px;
  color: #2D3436;
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.25s ease;
}

.user-info:hover {
  background: #EEF2F7;
}

.dropdown-icon {
  color: #B2BEC3;
  font-size: 12px;
}

/* 主内容区 */
.main-content {
  background: #FAFAFA;
  padding: 24px;
  overflow-y: auto;
}
</style>
