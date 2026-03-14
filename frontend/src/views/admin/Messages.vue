<template>
  <div class="messages-container">
    <!-- Statistics Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#409EFF"><Bell /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalNotifications || 0 }}</div>
              <div class="stat-label">系统通知</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#E6A23C"><ChatLineSquare /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalFeedback || 0 }}</div>
              <div class="stat-label">用户反馈</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#F56C6C"><Warning /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.unhandledAlerts || 0 }}</div>
              <div class="stat-label">待处理警报</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" color="#67C23A"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.publishedAnnouncements || 0 }}</div>
              <div class="stat-label">已发布公告</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Main Content Tabs -->
    <el-card class="main-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- Notifications -->
        <el-tab-pane label="📬 系统通知" name="notifications">
          <div class="tab-header">
            <el-button type="primary" @click="ntf.showDialog()">新建通知</el-button>
          </div>
          <el-table :data="ntf.list.value" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="notificationType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="H.notificationTypeTagMap[row.notificationType] || 'info'">
                  {{ H.notificationTypeMap[row.notificationType] || row.notificationType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="H.priorityTagMap[row.priority] || ''" size="small">{{ row.priority }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sendStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.sendStatus === 1 ? 'success' : 'warning'" size="small">
                  {{ row.sendStatus === 1 ? '已发送' : '待发送' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.sendStatus === 0" type="success" size="small" @click="ntf.send(row.id)">发送</el-button>
                <el-button type="primary" size="small" @click="ntf.showDialog(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="ntf.remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="ntf.page.value" v-model:page-size="pageSize"
            :total="ntf.total.value" layout="total, sizes, prev, pager, next"
            @size-change="ntf.load" @current-change="ntf.load"
            style="margin-top: 20px; justify-content: center" />
        </el-tab-pane>

        <!-- Feedback -->
        <el-tab-pane label="📮 用户反馈" name="feedback">
          <el-table :data="fb.list.value" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="feedbackType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="H.feedbackTypeTagMap[row.feedbackType] || 'info'">
                  {{ H.feedbackTypeMap[row.feedbackType] || row.feedbackType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="H.feedbackStatusTagMap[row.status] || 'info'" size="small">
                  {{ H.feedbackStatusMap[row.status] || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="fb.view(row)">查看</el-button>
                <el-button v-if="row.status < 2" type="success" size="small" @click="fb.startReply(row)">回复</el-button>
                <el-button type="danger" size="small" @click="fb.remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="fb.page.value" v-model:page-size="pageSize"
            :total="fb.total.value" layout="total, sizes, prev, pager, next"
            @size-change="fb.load" @current-change="fb.load"
            style="margin-top: 20px; justify-content: center" />
        </el-tab-pane>

        <!-- Alerts -->
        <el-tab-pane label="⚠️ 系统警报" name="alerts">
          <el-table :data="alt.list.value" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="alertType" label="类型" width="120">
              <template #default="{ row }"><el-tag>{{ row.alertType }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="H.alertLevelTagMap[row.level] || 'info'" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="H.alertStatusTagMap[row.status] || 'info'" size="small">
                  {{ H.alertStatusMap[row.status] || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" type="success" size="small" @click="alt.startHandle(row)">处理</el-button>
                <el-button type="info" size="small" @click="alt.view(row)">查看</el-button>
                <el-button type="danger" size="small" @click="alt.remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="alt.page.value" v-model:page-size="pageSize"
            :total="alt.total.value" layout="total, sizes, prev, pager, next"
            @size-change="alt.load" @current-change="alt.load"
            style="margin-top: 20px; justify-content: center" />
        </el-tab-pane>

        <!-- Announcements -->
        <el-tab-pane label="📝 公告管理" name="announcements">
          <div class="tab-header">
            <el-button type="primary" @click="ann.showDialog()">新建公告</el-button>
          </div>
          <el-table :data="ann.list.value" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="announcementType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ H.announcementTypeMap[row.announcementType] || row.announcementType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pinned" label="置顶" width="80">
              <template #default="{ row }">
                <el-tag :type="row.pinned === 1 ? 'danger' : 'info'" size="small">{{ row.pinned === 1 ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="H.announcementStatusTagMap[row.status] || 'info'" size="small">
                  {{ H.announcementStatusMap[row.status] || '未知' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" type="success" size="small" @click="ann.publish(row.id)">发布</el-button>
                <el-button v-if="row.status === 1" type="warning" size="small" @click="ann.unpublish(row.id)">下线</el-button>
                <el-button type="info" size="small" @click="ann.togglePin(row.id)">{{ row.pinned === 1 ? '取消置顶' : '置顶' }}</el-button>
                <el-button type="primary" size="small" @click="ann.showDialog(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="ann.remove(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-model:current-page="ann.page.value" v-model:page-size="pageSize"
            :total="ann.total.value" layout="total, sizes, prev, pager, next"
            @size-change="ann.load" @current-change="ann.load"
            style="margin-top: 20px; justify-content: center" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Notification Dialog -->
    <el-dialog v-model="ntf.dialogVisible.value" :title="ntf.form.id ? '编辑通知' : '新建通知'" width="600px">
      <el-form :model="ntf.form" label-width="100px">
        <el-form-item label="通知类型">
          <el-select v-model="ntf.form.notificationType" style="width: 100%">
            <el-option label="系统通知" value="SYSTEM" /><el-option label="个人通知" value="PERSONAL" /><el-option label="广播通知" value="BROADCAST" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="ntf.form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="ntf.form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="ntf.form.priority" style="width: 100%">
            <el-option label="低" value="LOW" /><el-option label="普通" value="NORMAL" /><el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ntf.dialogVisible.value = false">取消</el-button>
        <el-button type="primary" @click="ntf.save">保存</el-button>
      </template>
    </el-dialog>

    <!-- Feedback Detail Dialog -->
    <el-dialog v-model="fb.dialogVisible.value" title="反馈详情" width="700px">
      <el-descriptions :column="2" border v-if="fb.current.value">
        <el-descriptions-item label="反馈类型">{{ H.feedbackTypeMap[fb.current.value.feedbackType] || fb.current.value.feedbackType }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ H.feedbackStatusMap[fb.current.value.status] || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ fb.current.value.title }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">{{ fb.current.value.content }}</el-descriptions-item>
        <el-descriptions-item label="联系方式" :span="2">{{ fb.current.value.contactInfo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="管理员回复" :span="2" v-if="fb.current.value.adminReply">{{ fb.current.value.adminReply }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ fb.current.value.createTime }}</el-descriptions-item>
        <el-descriptions-item label="回复时间">{{ fb.current.value.replyTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- Reply Feedback Dialog -->
    <el-dialog v-model="fb.replyDialogVisible.value" title="回复反馈" width="600px">
      <el-form label-width="100px">
        <el-form-item label="回复内容">
          <el-input v-model="fb.replyContent.value" type="textarea" :rows="6" placeholder="请输入回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fb.replyDialogVisible.value = false">取消</el-button>
        <el-button type="primary" @click="fb.submitReply">提交回复</el-button>
      </template>
    </el-dialog>

    <!-- Alert Detail/Handle Dialog -->
    <el-dialog v-model="alt.dialogVisible.value" title="警报详情" width="700px">
      <el-descriptions :column="2" border v-if="alt.current.value">
        <el-descriptions-item label="警报类型">{{ alt.current.value.alertType }}</el-descriptions-item>
        <el-descriptions-item label="级别"><el-tag :type="H.alertLevelTagMap[alt.current.value.level] || 'info'">{{ alt.current.value.level }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ alt.current.value.title }}</el-descriptions-item>
        <el-descriptions-item label="消息" :span="2">{{ alt.current.value.message }}</el-descriptions-item>
        <el-descriptions-item label="来源模块">{{ alt.current.value.sourceModule || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ H.alertStatusMap[alt.current.value.status] || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ alt.current.value.createTime }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2" v-if="alt.current.value.handleNote">{{ alt.current.value.handleNote }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="alt.current.value && alt.current.value.status === 0" label-width="100px" style="margin-top: 20px">
        <el-form-item label="处理备注"><el-input v-model="alt.handleNote.value" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="alt.handleStatus.value">
            <el-radio :label="1">处理中</el-radio><el-radio :label="2">已处理</el-radio><el-radio :label="3">已忽略</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer v-if="alt.current.value && alt.current.value.status === 0">
        <el-button @click="alt.dialogVisible.value = false">取消</el-button>
        <el-button type="primary" @click="alt.submitHandle">提交处理</el-button>
      </template>
    </el-dialog>

    <!-- Announcement Dialog -->
    <el-dialog v-model="ann.dialogVisible.value" :title="ann.form.id ? '编辑公告' : '新建公告'" width="700px">
      <el-form :model="ann.form" label-width="100px">
        <el-form-item label="公告类型">
          <el-select v-model="ann.form.announcementType" style="width: 100%">
            <el-option label="系统公告" value="SYSTEM" /><el-option label="维护公告" value="MAINTENANCE" /><el-option label="新功能" value="FEATURE" /><el-option label="活动公告" value="ACTIVITY" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="ann.form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="ann.form.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="ann.form.priority" style="width: 100%">
            <el-option label="低" value="LOW" /><el-option label="普通" value="NORMAL" /><el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标角色">
          <el-checkbox-group v-model="ann.targetRoles.value">
            <el-checkbox label="PATIENT">患者</el-checkbox><el-checkbox label="DOCTOR">医生</el-checkbox><el-checkbox label="ADMIN">管理员</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ann.dialogVisible.value = false">取消</el-button>
        <el-button type="primary" @click="ann.save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Bell, ChatLineSquare, Warning, Document } from '@element-plus/icons-vue'
import { useMessages, useNotifications, useFeedback, useAlerts, useAnnouncements } from './messages/useMessages'
import * as H from './messages/helpers'

const activeTab = ref('notifications')
const { loading, pageSize, statistics, loadStatistics } = useMessages()
const ntf = useNotifications(pageSize, loading, loadStatistics)
const fb = useFeedback(pageSize, loading, loadStatistics)
const alt = useAlerts(pageSize, loading, loadStatistics)
const ann = useAnnouncements(pageSize, loading, loadStatistics)

const handleTabChange = (tab: string) => {
  const loaders: Record<string, () => void> = {
    notifications: ntf.load, feedback: fb.load, alerts: alt.load, announcements: ann.load
  }
  loaders[tab]?.()
}

onMounted(() => { loadStatistics(); ntf.load() })
</script>

<style scoped lang="scss">
.messages-container { padding: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card {
  .stat-content {
    display: flex; align-items: center; gap: 15px;
    .stat-icon { font-size: 40px; }
    .stat-info {
      flex: 1;
      .stat-value { font-size: 28px; font-weight: bold; color: #303133; line-height: 1.2; }
      .stat-label { font-size: 14px; color: #909399; margin-top: 5px; }
    }
  }
}
.main-card { .tab-header { margin-bottom: 15px; } }
</style>
