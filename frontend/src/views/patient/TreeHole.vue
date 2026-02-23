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
import { Edit, HomeFilled, Lock } from '@element-plus/icons-vue'
import TreeHoleEditor from '@/components/MoodDiary/TreeHoleEditor.vue'
import TreeHoleCard from '@/components/MoodDiary/TreeHoleCard.vue'
import TreeHoleDetail from '@/components/MoodDiary/TreeHoleDetail.vue'
import { useTreeHole } from './tree-hole/useTreeHole'

const {
  activeTab, loading, showEditor, showDetail, selectedId,
  showArchive, activeCategories,
  activeTreeHoles, canViewArchive,
  currentPage, pageSize, total,
  totalArchiveCount, archivedCategories, stats,
  loadActiveTreeHoles, handleTabChange, handleEditorSuccess,
  handleView, handleDelete, handleArchiveDelete,
  getFireflyStyle, getLeafStyle
} = useTreeHole()

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
