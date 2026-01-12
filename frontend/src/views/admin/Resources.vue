<template>
  <div class="resources-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalResources || 0 }}</div>
              <div class="stat-label">总资源数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A"><View /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalViews || 0 }}</div>
              <div class="stat-label">总浏览量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C"><Star /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalLikes || 0 }}</div>
              <div class="stat-label">总点赞数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C"><Download /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalDownloads || 0 }}</div>
              <div class="stat-label">总下载量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <h2>资源管理</h2>
          <el-button type="primary" @click="showResourceDialog()">新增资源</el-button>
        </div>
      </template>

      <!-- 筛选表单 -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="资源类型">
          <el-select v-model="filters.type" placeholder="全部类型" clearable style="width: 130px">
            <el-option label="文章" value="ARTICLE" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="音频" value="AUDIO" />
            <el-option label="文档" value="DOCUMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.category" placeholder="全部分类" clearable style="width: 130px">
            <el-option label="心理健康" value="MENTAL_HEALTH" />
            <el-option label="情绪管理" value="EMOTION" />
            <el-option label="压力缓解" value="STRESS" />
            <el-option label="睡眠改善" value="SLEEP" />
            <el-option label="人际关系" value="RELATIONSHIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="标题/作者" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadResources">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 资源列表 -->
      <el-table :data="resources" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.resourceType)">{{ getTypeName(row.resourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ getCategoryName(row.category) }}</template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showResourceDialog(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button type="danger" size="small" @click="deleteResource(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadResources"
        @current-change="loadResources"
        class="pagination"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="currentResource.id ? '编辑资源' : '新增资源'" width="700px">
      <el-form :model="currentResource" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="currentResource.title" placeholder="请输入资源标题" />
        </el-form-item>
        <el-form-item label="资源类型" required>
          <el-select v-model="currentResource.resourceType" placeholder="请选择类型" style="width: 100%">
            <el-option label="文章" value="ARTICLE" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="音频" value="AUDIO" />
            <el-option label="文档" value="DOCUMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="currentResource.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="心理健康" value="MENTAL_HEALTH" />
            <el-option label="情绪管理" value="EMOTION" />
            <el-option label="压力缓解" value="STRESS" />
            <el-option label="睡眠改善" value="SLEEP" />
            <el-option label="人际关系" value="RELATIONSHIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="currentResource.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="currentResource.summary" type="textarea" :rows="2" placeholder="请输入摘要" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="currentResource.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="资源链接">
          <el-input v-model="currentResource.resourceUrl" placeholder="视频/音频/文档链接" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="currentResource.coverImage" placeholder="封面图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResource" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, View, Star, Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)

const statistics = reactive({
  totalResources: 0,
  totalViews: 0,
  totalLikes: 0,
  totalDownloads: 0
})

const filters = reactive({
  type: '',
  category: '',
  status: undefined as number | undefined,
  keyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const resources = ref<any[]>([])

const currentResource = reactive({
  id: null as number | null,
  title: '',
  resourceType: '',
  category: '',
  author: '',
  summary: '',
  content: '',
  resourceUrl: '',
  coverImage: ''
})

const loadStatistics = async () => {
  try {
    const res = await request.get('/admin/resources/statistics')
    if (res.code === 200 && res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const loadResources = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      type: filters.type || undefined,
      category: filters.category || undefined,
      status: filters.status,
      keyword: filters.keyword || undefined
    }
    const res = await request.get('/admin/resources', { params })
    if (res.code === 200 && res.data) {
      resources.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载资源列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.type = ''
  filters.category = ''
  filters.status = undefined
  filters.keyword = ''
  pagination.pageNum = 1
  loadResources()
}

const showResourceDialog = (resource?: any) => {
  if (resource) {
    Object.assign(currentResource, resource)
  } else {
    currentResource.id = null
    currentResource.title = ''
    currentResource.resourceType = ''
    currentResource.category = ''
    currentResource.author = ''
    currentResource.summary = ''
    currentResource.content = ''
    currentResource.resourceUrl = ''
    currentResource.coverImage = ''
  }
  dialogVisible.value = true
}

const saveResource = async () => {
  if (!currentResource.title || !currentResource.resourceType || !currentResource.category || !currentResource.content) {
    ElMessage.warning('请填写必填项')
    return
  }

  saving.value = true
  try {
    const res = currentResource.id
      ? await request.put(`/admin/resources/${currentResource.id}`, currentResource)
      : await request.post('/admin/resources', currentResource)

    if (res.code === 200) {
      ElMessage.success(currentResource.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadResources()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (resource: any) => {
  try {
    const newStatus = resource.status === 1 ? 0 : 1
    const res = await request.put(`/admin/resources/${resource.id}/status`, { status: newStatus })
    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '发布成功' : '下架成功')
      loadResources()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteResource = async (resource: any) => {
  try {
    await ElMessageBox.confirm('确定要删除此资源吗？', '警告', { type: 'warning' })
    const res = await request.delete(`/admin/resources/${resource.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadResources()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = {
    ARTICLE: '文章', VIDEO: '视频', AUDIO: '音频', DOCUMENT: '文档'
  }
  return map[type] || type
}

const getTypeTag = (type: string) => {
  const map: Record<string, string> = {
    ARTICLE: 'primary', VIDEO: 'success', AUDIO: 'warning', DOCUMENT: 'info'
  }
  return map[type] || ''
}

const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    MENTAL_HEALTH: '心理健康', EMOTION: '情绪管理', STRESS: '压力缓解',
    SLEEP: '睡眠改善', RELATIONSHIP: '人际关系'
  }
  return map[category] || category
}

onMounted(() => {
  loadStatistics()
  loadResources()
})
</script>

<style scoped>
.resources-container { padding: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card { cursor: pointer; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-5px); box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
.stat-content { display: flex; align-items: center; gap: 15px; }
.stat-icon { font-size: 36px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header h2 { margin: 0; color: #303133; }
.filter-form { margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px; }
.pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
