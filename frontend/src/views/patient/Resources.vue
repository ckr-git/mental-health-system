<template>
  <div class="resources-container">
    <el-card class="header-card">
      <div class="header-row">
        <h2>心理健康资源库</h2>
        <el-input v-model="keyword" placeholder="搜索资源" clearable style="width: 240px" @keyup.enter="search">
          <template #append><el-button @click="search">搜索</el-button></template>
        </el-input>
      </div>
    </el-card>

    <el-tabs v-model="activeType" @tab-change="search" style="margin-top: 16px">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="文章" name="ARTICLE" />
      <el-tab-pane label="视频" name="VIDEO" />
      <el-tab-pane label="音频" name="AUDIO" />
      <el-tab-pane label="文档" name="DOCUMENT" />
    </el-tabs>

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="item in list" :key="item.id" style="margin-bottom: 16px">
        <el-card shadow="hover" class="resource-card" @click="showDetail(item.id)">
          <div class="card-top">
            <el-tag size="small" :type="typeTag(item.type)">{{ typeLabel(item.type) }}</el-tag>
            <span class="view-count">👁 {{ item.viewCount }}</span>
          </div>
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-desc">{{ item.content?.slice(0, 80) }}...</p>
          <div class="card-meta" v-if="item.author">{{ item.author }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && list.length === 0" description="暂无资源" />

    <el-pagination v-if="total > 0" v-model:current-page="page" :page-size="pageSize" :total="total"
      layout="total, prev, pager, next" @current-change="loadList" style="margin-top: 16px; text-align: center" />

    <el-dialog v-model="dialogVisible" :title="detail?.title" width="640px" destroy-on-close>
      <div v-if="detail">
        <el-tag size="small" :type="typeTag(detail.type)">{{ typeLabel(detail.type) }}</el-tag>
        <span style="margin-left: 12px; color: #909399; font-size: 13px">👁 {{ detail.viewCount }} · {{ detail.author || '佚名' }}</span>
        <el-divider />
        <div class="detail-content">{{ detail.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { resourceApi } from '@/api'
import type { MentalResource } from '@/types'

const list = ref<MentalResource[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(9)
const total = ref(0)
const activeType = ref('')
const keyword = ref('')
const dialogVisible = ref(false)
const detail = ref<MentalResource | null>(null)

const typeLabel = (t: string) => ({ ARTICLE: '文章', VIDEO: '视频', AUDIO: '音频', DOCUMENT: '文档' }[t] || t)
const typeTag = (t: string) => ({ ARTICLE: 'primary', VIDEO: 'success', AUDIO: 'warning', DOCUMENT: 'info' }[t] as any || '')

const loadList = async () => {
  loading.value = true
  try {
    const { data } = await resourceApi.getList({
      pageNum: page.value, pageSize: pageSize.value,
      type: activeType.value || undefined, keyword: keyword.value || undefined
    })
    list.value = data.records
    total.value = data.total
  } catch { ElMessage.error('加载资源失败') }
  finally { loading.value = false }
}

const search = () => { page.value = 1; loadList() }

const showDetail = async (id: number) => {
  try {
    const { data } = await resourceApi.getDetail(id)
    detail.value = data
    dialogVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.resources-container { padding: 20px; }
.header-card { margin-bottom: 0; }
.header-row { display: flex; justify-content: space-between; align-items: center;
  h2 { margin: 0; color: #303133; }
}
.resource-card { cursor: pointer; transition: transform 0.2s; &:hover { transform: translateY(-4px); } }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.view-count { font-size: 12px; color: #909399; }
.card-title { margin: 0 0 8px; font-size: 16px; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-desc { margin: 0; font-size: 13px; color: #909399; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { margin-top: 8px; font-size: 12px; color: #C0C4CC; }
.detail-content { line-height: 1.8; color: #606266; white-space: pre-wrap; word-break: break-word; }
</style>
