<template>
  <div
    class="room-canvas"
    :class="roomThemeClass"
    @mousedown="handleBackgroundMouseDown"
    @mouseup="handleBackgroundMouseUp"
    @mouseleave="handleBackgroundMouseUp"
  >
    <InteractionToolbar
      class="toolbar"
      @tool-selected="handleToolSelected"
    />

    <transition-group name="decoration-fade" tag="div" class="decoration-layer">
      <DecorationItem
        v-for="decoration in activeDecorations"
        :key="decoration.id"
        v-memo="[decoration.positionX, decoration.positionY, decoration.positionZ, decoration.scale, decoration.rotation, editMode, selectedTool]"
        :decoration="decoration"
        :editable="editMode"
        :selected-tool="selectedTool"
        :interaction-type="getInteractionType(selectedTool)"
        @update-position="handleUpdatePosition"
        @interact="handleInteract"
        @remove="handleRemove"
      />
    </transition-group>

    <!-- 彩蛋组件 -->
    <CloudInteraction v-show="themeEffects.clouds" ref="cloudRef" />
    <MeteorShower v-show="themeEffects.meteor" ref="meteorRef" />
    <FireworkEffect v-show="themeEffects.fireworks" ref="fireworkRef" />

    <FloatingFeedback ref="feedbackRef" />

    <el-button
      class="shop-toggle-btn"
      type="primary"
      circle
      @click="showShop = true"
      title="装饰商店"
    >
      <el-icon><Present /></el-icon>
    </el-button>

    <el-button
      class="theme-toggle-btn"
      type="success"
      circle
      @click="showThemeSelector = true"
      title="切换主题"
    >
      <el-icon><Sunny /></el-icon>
    </el-button>

    <RoomDecorationShop
      v-model:visible="showShop"
      :decorations="allDecorations"
      :configs="configs"
      @add="handleAddDecoration"
      @check-unlock="handleCheckUnlock"
    />

    <ThemeSelector
      v-model:visible="showThemeSelector"
      :current-theme="currentTheme"
      @theme-changed="handleThemeChanged"
    />

    <el-dialog
      v-model="showUnlockDialog"
      title="🎉 解锁新装饰"
      width="360px"
      center
    >
      <div class="unlock-content">
        <p class="unlock-message">恭喜你获得新的装饰！</p>
        <ul class="unlock-list">
          <li v-for="item in newlyUnlocked" :key="item">{{ item }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button type="primary" @click="showUnlockDialog = false">好的</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, defineAsyncComponent } from 'vue'
import { ElMessage } from 'element-plus'
import { Present, Sunny } from '@element-plus/icons-vue'
import DecorationItem from './DecorationItem.vue'
import RoomDecorationShop from './RoomDecorationShop.vue'
import InteractionToolbar from './InteractionToolbar.vue'
import FloatingFeedback from './FloatingFeedback.vue'
import ThemeSelector from './ThemeSelector.vue'
// Lazy load easter egg components to improve initial load time
const CloudInteraction = defineAsyncComponent(() => import('./CloudInteraction.vue'))
const MeteorShower = defineAsyncComponent(() => import('./MeteorShower.vue'))
const FireworkEffect = defineAsyncComponent(() => import('./FireworkEffect.vue'))
import { roomApi, themeApi } from '@/api'
import { playSound } from '@/utils/soundService'
import { hapticFeedback } from '@/utils/hapticService'
import { debounce, rafThrottle } from '@/utils/performance'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const allDecorations = ref<any[]>([])
const configs = ref<any[]>([])
const editMode = ref(false)
const selectedTool = ref('hand')
const showShop = ref(false)
const showThemeSelector = ref(false)
const showUnlockDialog = ref(false)
const newlyUnlocked = ref<string[]>([])
const feedbackRef = ref<InstanceType<typeof FloatingFeedback> | null>(null)
const cloudRef = ref<InstanceType<typeof CloudInteraction> | null>(null)
const meteorRef = ref<InstanceType<typeof MeteorShower> | null>(null)
const fireworkRef = ref<InstanceType<typeof FireworkEffect> | null>(null)
const currentTheme = ref('default_day')

// 主题效果状态
const themeEffects = ref({
  clouds: true,
  meteor: false,
  fireworks: false
})

// 长按计时器
const longPressTimer = ref<number | null>(null)
const LONG_PRESS_DURATION = 1500 // 1.5秒触发流星雨

const roomThemeClass = computed(() => `room-theme-${currentTheme.value}`)

const activeDecorations = computed(() =>
  allDecorations.value.filter((item) => item.isActive === 1)
)

// 主题效果映射表
const themeEffectMap: Record<string, typeof themeEffects.value> = {
  default_day: { clouds: true, meteor: false, fireworks: false },
  christmas: { clouds: false, meteor: false, fireworks: true },
  newyear: { clouds: false, meteor: false, fireworks: true },
  halloween: { clouds: false, meteor: true, fireworks: false },
  cherry_blossom: { clouds: true, meteor: false, fireworks: false },
  seaside: { clouds: true, meteor: false, fireworks: false },
  mountain: { clouds: true, meteor: true, fireworks: false },
  starry: { clouds: false, meteor: true, fireworks: false }
}

// 应用主题效果
const applyThemeEffects = (theme: string) => {
  themeEffects.value = themeEffectMap[theme] || themeEffectMap.default_day
}

// 初始化主题效果
applyThemeEffects(currentTheme.value)

// 监听主题变化
watch(currentTheme, (newTheme) => {
  applyThemeEffects(newTheme)
})

const loadDecorations = async () => {
  if (!userStore.token) return
  try {
    const [decorationsRes, configsRes] = await Promise.all([
      roomApi.getDecorations(),
      roomApi.getConfigs()
    ])

    if (decorationsRes.code === 200) {
      allDecorations.value = decorationsRes.data || []
    }
    if (configsRes.code === 200) {
      configs.value = configsRes.data || []
    }
  } catch (error) {
    console.error('Failed to load decorations:', error)
    ElMessage.error('加载装饰数据失败')
  }
}

const handleCheckUnlock = async () => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    const res = await roomApi.checkUnlock()
    if (res.code === 200) {
      const { newlyUnlocked: unlocked } = res.data
      if (unlocked && unlocked.length > 0) {
        newlyUnlocked.value = unlocked
        showUnlockDialog.value = true
        await loadDecorations()
      } else {
        ElMessage.info('暂无新的装饰可解锁')
      }
    }
  } catch (error) {
    console.error('Failed to check unlock:', error)
    ElMessage.error('检查解锁失败')
  }
}

const handleAddDecoration = async (decorationType: string, position: { x: number; y: number }) => {
  try {
    const res = await roomApi.addToRoom({
      decorationType,
      x: Math.round(position.x),
      y: Math.round(position.y)
    })
    if (res.code === 200) {
      ElMessage.success('装饰已放置')
      playSound('place')
      await loadDecorations()
      showShop.value = false
    }
  } catch (error) {
    console.error('Failed to add decoration:', error)
    ElMessage.error('放置装饰失败')
  }
}

// Debounced position update to reduce API calls during drag operations
const updatePositionDebounced = debounce(async (decorationId: number, x: number, y: number) => {
  try {
    const res = await roomApi.updatePosition(decorationId, {
      x: Math.round(x),
      y: Math.round(y)
    })
    if (res.code !== 200) {
      ElMessage.error('更新位置失败')
    }
  } catch (error) {
    console.error('Failed to update position:', error)
    ElMessage.error('更新位置失败')
  }
}, 300) // Wait 300ms after dragging stops before saving

const handleUpdatePosition = (decoration: any, position: { x: number; y: number }) => {
  // Update UI immediately for smooth dragging experience
  decoration.positionX = Math.round(position.x)
  decoration.positionY = Math.round(position.y)

  // Debounce the API call to avoid excessive requests
  updatePositionDebounced(decoration.id, position.x, position.y)
}

const handleInteract = async (decoration: any) => {
  try {
    const res = await roomApi.interact(decoration.id)
    if (res.code === 200) {
      playSound('interact')
      if (feedbackRef.value) {
        feedbackRef.value.showFeedback(
          decoration.decorationType,
          decoration.positionX ?? 50,
          decoration.positionY ?? 50,
          res.data.interactionCount
        )
      }
    }
  } catch (error) {
    console.error('Failed to interact:', error)
    ElMessage.error('互动失败')
  }
}

const handleRemove = async (decoration: any) => {
  try {
    const res = await roomApi.removeDecoration(decoration.id)
    if (res.code === 200) {
      ElMessage.success('装饰已移除')
      await loadDecorations()
    }
  } catch (error) {
    console.error('Failed to remove decoration:', error)
    ElMessage.error('移除失败')
  }
}

const toggleEditMode = () => {
  editMode.value = !editMode.value
  if (editMode.value) {
    selectedTool.value = 'hand'
  }
  ElMessage.info(editMode.value ? '进入编辑模式' : '退出编辑模式')
}

const handleToolSelected = (toolType: string) => {
  selectedTool.value = toolType
  if (toolType === 'hand' && !editMode.value) {
    editMode.value = true
  }
  if (toolType !== 'hand' && editMode.value) {
    editMode.value = false
  }
}

const getInteractionType = (toolType: string) => {
  const map: Record<string, string> = {
    hand: 'move',
    watering_can: 'water',
    lighter: 'light',
    magnifier: 'view',
    music_note: 'play_music',
    switch: 'toggle',
    wand: 'blessing',
    heart: 'pet'
  }
  return map[toolType] || ''
}

const loadThemeConfig = async () => {
  if (!userStore.token) return
  try {
    const res = await themeApi.getUnlockedThemes()
    if (res.code === 200) {
      currentTheme.value = res.data.currentTheme || 'default_day'
    }
  } catch (error) {
    console.error('Failed to load theme config:', error)
  }
}

const handleThemeChanged = (theme: string) => {
  currentTheme.value = theme
  playSound('place')
  hapticFeedback.success()
  ElMessage.success('主题切换成功')
}

// 长按触发流星雨
const handleBackgroundMouseDown = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.classList.contains('room-canvas')) {
    longPressTimer.value = window.setTimeout(() => {
      if (meteorRef.value) {
        meteorRef.value.createMeteorShower(50)
        ElMessage({
          message: '🌠 流星雨出现了！许个愿吧',
          type: 'success',
          duration: 3000
        })
        playSound('unlock')
        hapticFeedback.specialEffect()
      }
    }, LONG_PRESS_DURATION)
  }
}

const handleBackgroundMouseUp = () => {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

// 检查特殊日期触发烟花
const checkSpecialDateFireworks = () => {
  const today = new Date()
  const month = today.getMonth() + 1
  const date = today.getDate()

  // 新年
  if (month === 1 && date === 1) {
    setTimeout(() => {
      if (fireworkRef.value) {
        fireworkRef.value.startFireworks(10000) // 10秒烟花
        ElMessage({
          message: '🎊 新年快乐！',
          type: 'success',
          duration: 5000
        })
      }
    }, 1000)
  }

  // 圣诞节
  if (month === 12 && date === 25) {
    setTimeout(() => {
      if (fireworkRef.value) {
        fireworkRef.value.startFireworks(8000) // 8秒烟花
        ElMessage({
          message: '🎄 圣诞快乐！',
          type: 'success',
          duration: 5000
        })
      }
    }, 1000)
  }
}

onMounted(async () => {
  await Promise.all([loadDecorations(), loadThemeConfig()])
  checkSpecialDateFireworks()
})

defineExpose({
  loadDecorations,
  checkUnlock: handleCheckUnlock
})
</script>

<style scoped>
.room-canvas {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 460px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(180deg, #fefefe 0%, #eef4ff 100%);
  transition: background 0.5s ease;
}

/* 主题样式 */
.room-theme-default_day {
  background: linear-gradient(180deg, #fefefe 0%, #eef4ff 100%);
}

.room-theme-christmas {
  background: linear-gradient(180deg, #fff5f5 0%, #ffcdd2 100%);
}

.room-theme-newyear {
  background: linear-gradient(180deg, #fffbf0 0%, #ffe0b2 100%);
}

.room-theme-halloween {
  background: linear-gradient(180deg, #fff3e0 0%, #ffe0b2 100%);
}

.room-theme-cherry_blossom {
  background: linear-gradient(180deg, #fff0f6 0%, #ffd6e7 100%);
}

.room-theme-seaside {
  background: linear-gradient(180deg, #e0f7fa 0%, #b2ebf2 100%);
}

.room-theme-mountain {
  background: linear-gradient(180deg, #f1f8e9 0%, #dcedc8 100%);
}

.room-theme-starry {
  background: radial-gradient(circle at 20% 20%, rgba(95, 133, 255, 0.5), transparent 60%), #0d1333;
  color: #e8ecff;
}

.decoration-layer {
  position: absolute;
  inset: 0;
}

.decoration-fade-enter-active,
.decoration-fade-leave-active {
  transition: all 0.35s ease;
}

.decoration-fade-enter-from,
.decoration-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.shop-toggle-btn,
.theme-toggle-btn {
  position: absolute;
  right: 20px;
  width: 50px;
  height: 50px;
  z-index: 20;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.shop-toggle-btn {
  bottom: 20px;
}

.theme-toggle-btn {
  bottom: 80px;
}

.unlock-content {
  text-align: center;
}

.unlock-message {
  margin-bottom: 12px;
  color: #606266;
}

.unlock-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.unlock-list li {
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(102, 126, 234, 0.12);
  color: #4a4a6a;
}
</style>
