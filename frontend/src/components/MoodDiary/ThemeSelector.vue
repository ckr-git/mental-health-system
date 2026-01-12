<template>
  <el-drawer
    v-model="visible"
    title="主题选择"
    direction="btt"
    size="70%"
  >
    <div class="theme-selector">
      <!-- 当前主题 -->
      <div class="current-theme-section">
        <h3>当前主题</h3>
        <div class="theme-card current" :class="`theme-${currentTheme}`">
          <div class="theme-preview">
            {{ getThemeInfo(currentTheme).icon }}
          </div>
          <div class="theme-info">
            <h4>{{ getThemeInfo(currentTheme).name }}</h4>
            <p>{{ getThemeInfo(currentTheme).description }}</p>
          </div>
        </div>
      </div>

      <!-- 已解锁主题 -->
      <div class="unlocked-themes-section">
        <h3>已解锁主题 ({{ unlockedThemes.length }}/{{ allThemes.length }})</h3>
        <div class="themes-grid">
          <div
            v-for="theme in unlockedThemes"
            :key="theme"
            class="theme-card"
            :class="{ active: currentTheme === theme, [`theme-${theme}`]: true }"
            @click="handleSwitchTheme(theme)"
          >
            <div class="theme-preview">
              {{ getThemeInfo(theme).icon }}
            </div>
            <div class="theme-info">
              <h4>{{ getThemeInfo(theme).name }}</h4>
              <p>{{ getThemeInfo(theme).description }}</p>
            </div>
            <div v-if="currentTheme === theme" class="active-badge">使用中</div>
          </div>
        </div>
      </div>

      <!-- 未解锁主题 -->
      <div class="locked-themes-section">
        <h3>未解锁主题</h3>
        <div class="themes-grid">
          <div
            v-for="theme in lockedThemes"
            :key="theme"
            class="theme-card locked"
          >
            <div class="theme-preview locked-overlay">
              <el-icon class="lock-icon"><Lock /></el-icon>
              {{ getThemeInfo(theme).icon }}
            </div>
            <div class="theme-info">
              <h4>{{ getThemeInfo(theme).name }}</h4>
              <p class="unlock-condition">{{ getThemeInfo(theme).unlockCondition }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 检查解锁按钮 -->
      <div class="check-unlock-section">
        <el-button
          type="primary"
          size="large"
          :loading="checking"
          @click="handleCheckUnlock"
        >
          <el-icon><Present /></el-icon>
          检查可解锁主题
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Present } from '@element-plus/icons-vue'
import { themeApi } from '@/api'
import { playSound } from '@/utils/soundService'
import { hapticFeedback } from '@/utils/hapticService'

interface Props {
  visible: boolean
  currentTheme?: string
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'theme-changed', theme: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 主题数据 - 必须在watch之前定义
const currentTheme = ref('default_day')
const unlockedThemes = ref<string[]>(['default_day'])
const checking = ref(false)

// 监听props变化 - 现在可以安全访问currentTheme
watch(
  () => props.currentTheme,
  (theme) => {
    if (theme) {
      currentTheme.value = theme
    }
  },
  { immediate: true }
)

// 所有主题配置
const allThemes = [
  'default_day',
  'christmas',
  'newyear',
  'halloween',
  'cherry_blossom',
  'seaside',
  'mountain',
  'starry'
]

const themeConfig: Record<string, { name: string; icon: string; description: string; unlockCondition: string }> = {
  default_day: {
    name: '默认日间',
    icon: '☀️',
    description: '清新明亮的默认主题',
    unlockCondition: '默认拥有'
  },
  christmas: {
    name: '温馨圣诞屋',
    icon: '🎄',
    description: '充满节日气氛的圣诞主题',
    unlockCondition: '12月自动解锁'
  },
  newyear: {
    name: '新年烟花',
    icon: '🎆',
    description: '新年祝福与烟花',
    unlockCondition: '1月自动解锁'
  },
  halloween: {
    name: '万圣节主题',
    icon: '🎃',
    description: '神秘有趣的万圣节',
    unlockCondition: '10月自动解锁'
  },
  cherry_blossom: {
    name: '春日樱花房',
    icon: '🌸',
    description: '浪漫的樱花飘落',
    unlockCondition: '连续打卡30天解锁'
  },
  seaside: {
    name: '海边小屋',
    icon: '🌊',
    description: '宁静的海边小屋',
    unlockCondition: '完成10次时光信箱'
  },
  mountain: {
    name: '山间木屋',
    icon: '🏔️',
    description: '静谧的山间避世之地',
    unlockCondition: '度过5次低谷'
  },
  starry: {
    name: '星空露营',
    icon: '🌙',
    description: '浪漫星空下的露营',
    unlockCondition: '夜晚模式使用30次'
  }
}

// 未解锁的主题
const lockedThemes = computed(() => {
  return allThemes.filter(theme => !unlockedThemes.value.includes(theme))
})

// 获取主题信息
const getThemeInfo = (theme: string) => {
  return themeConfig[theme] || themeConfig.default_day
}

// 加载主题数据
const loadThemeData = async () => {
  try {
    const res = await themeApi.getUnlockedThemes()
    if (res.code === 200) {
      currentTheme.value = res.data.currentTheme || 'default_day'
      unlockedThemes.value = res.data.unlockedThemes || ['default_day']
    }
  } catch (error) {
    console.error('Failed to load theme data:', error)
  }
}

// 切换主题
const handleSwitchTheme = async (theme: string) => {
  if (theme === currentTheme.value) return

  try {
    const res = await themeApi.switchTheme(theme)
    if (res.code === 200) {
      currentTheme.value = theme
      ElMessage.success('主题切换成功')
      playSound('place')
      hapticFeedback.success()
      emit('theme-changed', theme)
    } else {
      ElMessage.error(res.message || '主题切换失败')
    }
  } catch (error: any) {
    console.error('Failed to switch theme:', error)
    if (error?.response?.status !== 401) {
      ElMessage.error('主题切换失败')
    }
  }
}

// 检查解锁
const handleCheckUnlock = async () => {
  checking.value = true
  try {
    const res = await themeApi.checkUnlock()
    if (res.code === 200) {
      const { newlyUnlocked, unlockedThemes: allUnlocked } = res.data

      if (newlyUnlocked && newlyUnlocked.length > 0) {
        ElMessage({
          message: `🎉 解锁了${newlyUnlocked.length}个新主题！`,
          type: 'success',
          duration: 3000
        })
        playSound('unlock')
        hapticFeedback.achievement()

        // 更新解锁列表
        unlockedThemes.value = allUnlocked
      } else {
        ElMessage.info('暂无可解锁的新主题')
      }
    }
  } catch (error: any) {
    console.error('Failed to check unlock:', error)
    if (error?.response?.status !== 401) {
      ElMessage.error('检查解锁失败')
    }
  } finally {
    checking.value = false
  }
}

// 监听visible变化，加载数据
watch(visible, (newVal) => {
  if (newVal) {
    loadThemeData()
  }
})

// 暴露方法
defineExpose({
  loadThemeData,
  checkUnlock: handleCheckUnlock
})
</script>

<style scoped>
.theme-selector {
  padding: 20px;
}

.current-theme-section,
.unlocked-themes-section,
.locked-themes-section {
  margin-bottom: 40px;
}

h3 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
}

.themes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.theme-card {
  position: relative;
  padding: 20px;
  border-radius: 12px;
  border: 2px solid #e4e7ed;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.theme-card:not(.locked):hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.theme-card.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
}

.theme-card.current {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2f1 100%);
}

.theme-card.locked {
  cursor: not-allowed;
  opacity: 0.6;
  background: #f5f7fa;
}

.theme-preview {
  font-size: 48px;
  text-align: center;
  margin-bottom: 16px;
  position: relative;
}

.locked-overlay {
  filter: grayscale(100%);
  opacity: 0.5;
}

.lock-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  color: #909399;
}

.theme-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.theme-info p {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.unlock-condition {
  color: #909399;
  font-style: italic;
}

.active-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  background: #409eff;
  color: white;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.check-unlock-section {
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #e4e7ed;
}

.check-unlock-section .el-button {
  width: 100%;
  max-width: 400px;
}

/* 主题特定样式 */
.theme-christmas {
  background: linear-gradient(135deg, #ffe5e5 0%, #ffcccc 100%);
}

.theme-newyear {
  background: linear-gradient(135deg, #fff9e6 0%, #ffe6b3 100%);
}

.theme-halloween {
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
}

.theme-cherry_blossom {
  background: linear-gradient(135deg, #fce4ec 0%, #f8bbd0 100%);
}

.theme-seaside {
  background: linear-gradient(135deg, #e1f5fe 0%, #b3e5fc 100%);
}

.theme-mountain {
  background: linear-gradient(135deg, #f1f8e9 0%, #dcedc8 100%);
}

.theme-starry {
  background: linear-gradient(135deg, #e8eaf6 0%, #c5cae9 100%);
}
</style>
