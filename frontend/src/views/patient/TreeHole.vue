<template>
  <div class="tree-hole-page">
    <!-- 萤火虫动画 -->
    <div class="fireflies">
      <div class="firefly" v-for="i in 5" :key="i" :style="getFireflyStyle(i)"></div>
    </div>

    <!-- 飘落树叶 -->
    <div class="falling-leaves">
      <div class="leaf" v-for="i in 6" :key="i" :style="getLeafStyle(i)"></div>
    </div>

    <!-- 返回首页按钮 -->
    <el-button
      class="back-home-btn"
      type="info"
      circle
      @click="$router.push('/patient/dashboard')"
      title="返回首页"
    >
      <el-icon><HomeFilled /></el-icon>
    </el-button>

    <!-- 主内容区 -->
    <div class="content-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">🌳 心情树洞</h1>
        <p class="page-subtitle">在这里安全地倾诉你的心情、想法和秘密</p>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button
          type="primary"
          size="large"
          @click="showEditor = true"
          class="add-btn"
        >
          ✏️ 开始倾诉
        </el-button>

        <el-button
          v-if="canViewArchive"
          type="success"
          size="large"
          @click="showArchive = !showArchive"
          class="archive-btn"
          plain
        >
          📂 {{ showArchive ? '返回树洞' : '档案馆' }}
        </el-button>

        <el-tooltip v-else content="心情极低(<3分)或极高(>8分)时可解锁档案馆" placement="top">
          <el-button
            type="info"
            size="large"
            class="archive-btn"
            disabled
            plain
          >
            🔒 档案馆
          </el-button>
        </el-tooltip>
      </div>

      <!-- 树洞列表 -->
      <div class="tree-hole-list" v-if="!showArchive" v-loading="loading">
        <el-empty v-if="activeTreeHoles.length === 0 && !loading" description="还没有倾诉记录,开始倾诉吧">
          <el-button type="primary" @click="showEditor = true">开始第一次倾诉</el-button>
        </el-empty>

        <div class="tree-hole-grid" v-else>
          <TreeHoleCard
            v-for="hole in activeTreeHoles"
            :key="hole.id"
            :tree-hole="hole"
            @click="handleView"
            @delete="handleDelete"
          />
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadActiveTreeHoles"
          />
        </div>
      </div>

      <!-- 档案馆区域 -->
      <div class="archive-section" v-else v-loading="loading">
        <div class="archive-header">
          <h2 class="archive-title">📂 树洞档案馆</h2>
          <p class="archive-subtitle">所有倾诉记录按对象分类保存</p>
          <div class="archive-stats">
            <el-tag type="success">共 {{ totalArchiveCount }} 条倾诉</el-tag>
            <el-tag type="info">{{ archivedCategories.length }} 个对象</el-tag>
          </div>
        </div>

        <el-empty v-if="archivedCategories.length === 0 && !loading" description="档案馆还是空的">
          <el-button type="primary" @click="showArchive = false">去倾诉</el-button>
        </el-empty>

        <!-- 分类折叠面板 -->
        <el-collapse v-else v-model="activeCategories" class="archive-collapse">
          <el-collapse-item
            v-for="category in archivedCategories"
            :key="category.key"
            :name="category.key"
          >
            <template #title>
              <div class="category-header">
                <span class="category-icon">{{ category.icon }}</span>
                <span class="category-name">{{ category.displayName }}</span>
                <el-tag size="small" type="info">{{ category.count }} 条</el-tag>
              </div>
            </template>

            <!-- 该分类下的树洞卡片 -->
            <div class="archive-grid">
              <TreeHoleCard
                v-for="hole in category.holes"
                :key="hole.id"
                :tree-hole="hole"
                :is-archive="true"
                @click="handleView"
                @delete="handleArchiveDelete"
              />
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </div>

    <!-- 编辑器对话框 -->
    <TreeHoleEditor
      v-model="showEditor"
      @success="handleEditorSuccess"
    />

    <!-- 详情对话框 -->
    <TreeHoleDetail
      v-model="showDetail"
      :id="selectedId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, HomeFilled, Lock } from '@element-plus/icons-vue'
import TreeHoleEditor from '@/components/MoodDiary/TreeHoleEditor.vue'
import TreeHoleCard from '@/components/MoodDiary/TreeHoleCard.vue'
import TreeHoleDetail from '@/components/MoodDiary/TreeHoleDetail.vue'
import request from '@/utils/request'

// 状态
const activeTab = ref('active')
const loading = ref(false)
const showEditor = ref(false)
const showDetail = ref(false)
const selectedId = ref<number>()
const showArchive = ref(false)  // 是否显示档案馆
const activeCategories = ref<string[]>([])  // 展开的分类

// 数据
const activeTreeHoles = ref<any[]>([])
const archiveTreeHoles = ref<any[]>([])
const archiveData = ref<Record<string, any[]>>({})  // 原始档案数据
const canViewArchive = ref(false)
const statsData = ref<Record<string, number>>({})

// 分页
const currentPage = ref(1)
const pageSize = ref(9)
const total = ref(0)

// 档案馆统计
const totalArchiveCount = computed(() => {
  return Object.values(archiveData.value).reduce((sum, arr) => sum + arr.length, 0)
})

// 倾诉对象图标映射
const speakToIconMap: Record<string, string> = {
  'self': '🧘',
  'person': '👤',
  'role': '🎭',
  'thing': '🎈',
  'custom': '✨'
}

// 档案分类数据
const archivedCategories = computed(() => {
  const categories: any[] = []

  for (const [key, holes] of Object.entries(archiveData.value)) {
    if (holes && holes.length > 0) {
      // 解析key: "speakToType:speakToName"
      const [type, name] = key.split(':')
      categories.push({
        key,
        displayName: name || '未分类',
        icon: speakToIconMap[type] || '💬',
        count: holes.length,
        holes: holes.sort((a, b) =>
          new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
        )
      })
    }
  }

  // 按倾诉数量降序排列
  return categories.sort((a, b) => b.count - a.count)
})

// 统计数据
const stats = computed(() => {
  const total = activeTreeHoles.value.length + archiveTreeHoles.value.length
  const active = activeTreeHoles.value.length
  return { total, active }
})

// 生命周期
onMounted(() => {
  loadActiveTreeHoles()
  checkArchivePermission()
})

// 监听档案馆显示状态
watch(showArchive, (newVal) => {
  if (newVal && canViewArchive.value) {
    loadArchive()
  }
})

// 加载活跃树洞
const loadActiveTreeHoles = async () => {
  try {
    loading.value = true
    const res = await request.get('/patient/tree-hole/active')

    if (res.code === 200) {
      activeTreeHoles.value = res.data || []
    }
  } catch (error: any) {
    console.error('Failed to load active tree holes:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 检查档案馆权限
const checkArchivePermission = async () => {
  try {
    const res = await request.get('/patient/tree-hole/can-view-archive')
    if (res.code === 200) {
      canViewArchive.value = res.data
    }
  } catch (error) {
    console.error('Failed to check archive permission:', error)
  }
}

// 加载档案馆
const loadArchive = async () => {
  if (!canViewArchive.value) {
    return
  }

  try {
    loading.value = true
    const res = await request.get('/patient/tree-hole/archive')

    if (res.code === 200) {
      // 保存原始分组数据（不展平）
      archiveData.value = res.data || {}

      // 也展平保存到archiveTreeHoles用于兼容
      archiveTreeHoles.value = Object.values(archiveData.value).flat()
    }
  } catch (error: any) {
    console.error('Failed to load archive:', error)
    ElMessage.error(error.response?.data?.message || '加载档案馆失败')
  } finally {
    loading.value = false
  }
}

// 切换标签页
const handleTabChange = (tabName: string) => {
  if (tabName === 'archive' && canViewArchive.value) {
    loadArchive()
  }
}

// 编辑器成功回调
const handleEditorSuccess = () => {
  loadActiveTreeHoles()
  checkArchivePermission()
}

// 查看详情
const handleView = (id: number) => {
  selectedId.value = id
  showDetail.value = true
}

// 删除记录
const handleDelete = async (id: number) => {
  try {
    const res = await request.delete(`/patient/tree-hole/delete/${id}`)

    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadActiveTreeHoles()
      if (showArchive.value) {
        loadArchive()
      }
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    console.error('Failed to delete tree hole:', error)
    ElMessage.error(error.response?.data?.message || '删除失败')
  }
}

// 删除档案记录
const handleArchiveDelete = async (id: number) => {
  await handleDelete(id)
}

// 萤火虫位置样式
const getFireflyStyle = (index: number) => {
  const positions = [
    { left: '20%', top: '30%', animationDelay: '0s' },
    { left: '60%', top: '50%', animationDelay: '1s' },
    { left: '80%', top: '40%', animationDelay: '2s' },
    { left: '40%', top: '70%', animationDelay: '1.5s' },
    { left: '90%', top: '60%', animationDelay: '2.5s' }
  ]
  return positions[index - 1]
}

// 树叶位置样式
const getLeafStyle = (index: number) => {
  const positions = [
    { left: '10%', animationDelay: '0s', animationDuration: '10s' },
    { left: '30%', animationDelay: '3s', animationDuration: '12s' },
    { left: '50%', animationDelay: '6s', animationDuration: '11s' },
    { left: '70%', animationDelay: '9s', animationDuration: '13s' },
    { left: '85%', animationDelay: '4s', animationDuration: '10s' },
    { left: '95%', animationDelay: '7s', animationDuration: '14s' }
  ]
  return positions[index - 1]
}
</script>

<style scoped lang="scss">
.tree-hole-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(
    180deg,
    #2c5530 0%,      /* 深绿色（树冠） */
    #4a7c4e 30%,     /* 绿色（树身） */
    #8b7355 70%,     /* 棕色（树干） */
    #6b5444 100%     /* 深棕色（树洞） */
  );
  z-index: 0;

  // 自定义滚动条样式
  &::-webkit-scrollbar {
    width: 10px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(139, 115, 85, 0.6);
    border-radius: 10px;
    transition: background 0.3s;

    &:hover {
      background: rgba(139, 115, 85, 0.8);
    }
  }

  // 树洞轮廓效果（伪元素）
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image:
      radial-gradient(circle at 50% 80%, rgba(139, 115, 85, 0.8) 0%, transparent 30%),
      radial-gradient(circle at 30% 60%, rgba(255, 223, 186, 0.1) 0%, transparent 20%),
      radial-gradient(circle at 70% 60%, rgba(255, 223, 186, 0.1) 0%, transparent 20%);
    pointer-events: none;
    z-index: 0;
  }

  // 树洞入口效果
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 400px;
    height: 500px;
    background: radial-gradient(
      ellipse at center,
      rgba(0, 0, 0, 0.4) 0%,
      transparent 70%
    );
    border-radius: 50% 50% 0 0;
    pointer-events: none;
    z-index: 0;
  }

  // 萤火虫动画
  .fireflies {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;

    .firefly {
      position: absolute;
      width: 4px;
      height: 4px;
      background: #ffd699;
      border-radius: 50%;
      box-shadow: 0 0 10px #ffd699, 0 0 20px #ffd699;
      animation: firefly 3s infinite ease-in-out;
      will-change: transform, opacity;

      @keyframes firefly {
        0%, 100% {
          transform: translate(0, 0);
          opacity: 0.3;
        }
        50% {
          transform: translate(20px, -20px);
          opacity: 1;
        }
      }
    }
  }

  // 飘落树叶动画
  .falling-leaves {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;

    .leaf {
      position: absolute;
      width: 20px;
      height: 20px;
      background: #8b7355;
      clip-path: polygon(50% 0%, 100% 40%, 80% 100%, 20% 100%, 0% 40%);
      animation: leaf-fall 10s infinite linear;
      opacity: 0;
      will-change: transform, opacity;

      @keyframes leaf-fall {
        0% {
          transform: translateY(-20px) rotate(0deg);
          opacity: 0;
        }
        10% {
          opacity: 0.8;
        }
        90% {
          opacity: 0.8;
        }
        100% {
          transform: translateY(100vh) rotate(360deg);
          opacity: 0;
        }
      }
    }
  }

  // 返回首页按钮
  .back-home-btn {
    position: fixed;
    top: 20px;
    left: 20px;
    z-index: 1000;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    width: 48px;
    height: 48px;
    font-size: 24px;

    &:hover {
      transform: scale(1.1);
      background: rgba(255, 255, 255, 1);
    }
  }

  // 主内容容器
  .content-container {
    position: relative;
    z-index: 1;
    max-width: 1200px;
    margin: 0 auto;
    padding: 100px 20px 40px;
  }

  // 页面标题
  .page-header {
    text-align: center;
    margin-bottom: 40px;
    color: white;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);

    .page-title {
      font-size: 36px;
      font-weight: 700;
      margin: 0 0 12px 0;
    }

    .page-subtitle {
      font-size: 16px;
      opacity: 0.9;
      margin: 0;
    }
  }

  // 操作按钮区
  .action-bar {
    display: flex;
    justify-content: center;
    gap: 16px;
    margin-bottom: 32px;

    .add-btn, .archive-btn {
      font-size: 16px;
      padding: 12px 32px;
      border-radius: 24px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }

  // 树洞列表
  .tree-hole-list {
    min-height: 400px;
  }

  // 树洞网格
  .tree-hole-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
    margin-bottom: 32px;
  }

  // 分页
  .pagination {
    display: flex;
    justify-content: center;
    padding: 20px 0;
  }

  // 档案馆区域
  .archive-section {
    min-height: 400px;

    .archive-header {
      text-align: center;
      margin-bottom: 32px;
      padding: 24px;
      background: rgba(255, 255, 255, 0.95);
      border-radius: 16px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

      .archive-title {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        margin: 0 0 12px 0;
      }

      .archive-subtitle {
        font-size: 14px;
        color: #909399;
        margin: 0 0 16px 0;
      }

      .archive-stats {
        display: flex;
        justify-content: center;
        gap: 12px;
      }
    }

    .archive-collapse {
      background: transparent;
      border: none;

      :deep(.el-collapse-item) {
        background: rgba(255, 255, 255, 0.95);
        border-radius: 16px;
        margin-bottom: 16px;
        overflow: hidden;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

        .el-collapse-item__header {
          background: linear-gradient(135deg, #f5f7fa 0%, #e8eaf0 100%);
          border: none;
          padding: 16px 24px;
          font-size: 16px;
          font-weight: 600;

          &:hover {
            background: linear-gradient(135deg, #e8eaf0 0%, #dfe1e7 100%);
          }

          &.is-active {
            background: linear-gradient(135deg, #e0f2fe 0%, #bae7ff 100%);
          }
        }

        .el-collapse-item__wrap {
          background: white;
          border: none;
        }

        .el-collapse-item__content {
          padding: 20px;
        }
      }

      .category-header {
        display: flex;
        align-items: center;
        gap: 12px;
        width: 100%;

        .category-icon {
          font-size: 24px;
        }

        .category-name {
          flex: 1;
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
      }

      .archive-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 20px;
      }
    }
  }
}
</style>
