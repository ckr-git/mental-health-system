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
        <!-- System Notifications Tab -->
        <el-tab-pane label="📬 系统通知" name="notifications">
          <div class="tab-header">
            <el-button type="primary" @click="showNotificationDialog()">新建通知</el-button>
          </div>

          <el-table :data="notificationList" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="notificationType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getNotificationTypeTag(row.notificationType)">
                  {{ getNotificationTypeName(row.notificationType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="{ row }">
                <el-tag :type="getPriorityTag(row.priority)" size="small">
                  {{ row.priority }}
                </el-tag>
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
                <el-button v-if="row.sendStatus === 0" type="success" size="small" @click="sendNotification(row.id)">
                  发送
                </el-button>
                <el-button type="primary" size="small" @click="showNotificationDialog(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteNotification(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="notificationPage"
            v-model:page-size="pageSize"
            :total="notificationTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadNotifications"
            @current-change="loadNotifications"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>

        <!-- User Feedback Tab -->
        <el-tab-pane label="📮 用户反馈" name="feedback">
          <el-table :data="feedbackList" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="feedbackType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getFeedbackTypeTag(row.feedbackType)">
                  {{ getFeedbackTypeName(row.feedbackType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getFeedbackStatusTag(row.status)" size="small">
                  {{ getFeedbackStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewFeedback(row)">查看</el-button>
                <el-button v-if="row.status < 2" type="success" size="small" @click="replyFeedback(row)">
                  回复
                </el-button>
                <el-button type="danger" size="small" @click="deleteFeedback(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="feedbackPage"
            v-model:page-size="pageSize"
            :total="feedbackTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadFeedback"
            @current-change="loadFeedback"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>

        <!-- System Alerts Tab -->
        <el-tab-pane label="⚠️ 系统警报" name="alerts">
          <el-table :data="alertList" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="alertType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.alertType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="getAlertLevelTag(row.level)" size="small">
                  {{ row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAlertStatusTag(row.status)" size="small">
                  {{ getAlertStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" type="success" size="small" @click="handleAlert(row)">
                  处理
                </el-button>
                <el-button type="info" size="small" @click="viewAlert(row)">查看</el-button>
                <el-button type="danger" size="small" @click="deleteAlert(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="alertPage"
            v-model:page-size="pageSize"
            :total="alertTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadAlerts"
            @current-change="loadAlerts"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>

        <!-- Announcements Tab -->
        <el-tab-pane label="📝 公告管理" name="announcements">
          <div class="tab-header">
            <el-button type="primary" @click="showAnnouncementDialog()">新建公告</el-button>
          </div>

          <el-table :data="announcementList" v-loading="loading" border stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="announcementType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ getAnnouncementTypeName(row.announcementType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="pinned" label="置顶" width="80">
              <template #default="{ row }">
                <el-tag :type="row.pinned === 1 ? 'danger' : 'info'" size="small">
                  {{ row.pinned === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAnnouncementStatusTag(row.status)" size="small">
                  {{ getAnnouncementStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" type="success" size="small" @click="publishAnnouncement(row.id)">
                  发布
                </el-button>
                <el-button v-if="row.status === 1" type="warning" size="small" @click="unpublishAnnouncement(row.id)">
                  下线
                </el-button>
                <el-button type="info" size="small" @click="togglePin(row.id)">
                  {{ row.pinned === 1 ? '取消置顶' : '置顶' }}
                </el-button>
                <el-button type="primary" size="small" @click="showAnnouncementDialog(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteAnnouncement(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="announcementPage"
            v-model:page-size="pageSize"
            :total="announcementTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="loadAnnouncements"
            @current-change="loadAnnouncements"
            style="margin-top: 20px; justify-content: center"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Notification Dialog -->
    <el-dialog v-model="notificationDialogVisible" :title="notificationForm.id ? '编辑通知' : '新建通知'" width="600px">
      <el-form :model="notificationForm" label-width="100px">
        <el-form-item label="通知类型">
          <el-select v-model="notificationForm.notificationType" style="width: 100%">
            <el-option label="系统通知" value="SYSTEM" />
            <el-option label="个人通知" value="PERSONAL" />
            <el-option label="广播通知" value="BROADCAST" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="notificationForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="notificationForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="notificationForm.priority" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notificationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNotification">保存</el-button>
      </template>
    </el-dialog>

    <!-- Feedback Detail Dialog -->
    <el-dialog v-model="feedbackDialogVisible" title="反馈详情" width="700px">
      <el-descriptions :column="2" border v-if="currentFeedback">
        <el-descriptions-item label="反馈类型">
          {{ getFeedbackTypeName(currentFeedback.feedbackType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getFeedbackStatusName(currentFeedback.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">
          {{ currentFeedback.title }}
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          {{ currentFeedback.content }}
        </el-descriptions-item>
        <el-descriptions-item label="联系方式" :span="2">
          {{ currentFeedback.contactInfo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="管理员回复" :span="2" v-if="currentFeedback.adminReply">
          {{ currentFeedback.adminReply }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ currentFeedback.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="回复时间">
          {{ currentFeedback.replyTime || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- Reply Feedback Dialog -->
    <el-dialog v-model="replyDialogVisible" title="回复反馈" width="600px">
      <el-form label-width="100px">
        <el-form-item label="回复内容">
          <el-input v-model="replyContent" type="textarea" :rows="6" placeholder="请输入回复内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>

    <!-- Alert Detail/Handle Dialog -->
    <el-dialog v-model="alertDialogVisible" title="警报详情" width="700px">
      <el-descriptions :column="2" border v-if="currentAlert">
        <el-descriptions-item label="警报类型">{{ currentAlert.alertType }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="getAlertLevelTag(currentAlert.level)">{{ currentAlert.level }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ currentAlert.title }}</el-descriptions-item>
        <el-descriptions-item label="消息" :span="2">{{ currentAlert.message }}</el-descriptions-item>
        <el-descriptions-item label="来源模块">{{ currentAlert.sourceModule || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getAlertStatusName(currentAlert.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentAlert.createTime }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2" v-if="currentAlert.handleNote">
          {{ currentAlert.handleNote }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form v-if="currentAlert && currentAlert.status === 0" label-width="100px" style="margin-top: 20px">
        <el-form-item label="处理备注">
          <el-input v-model="alertHandleNote" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="alertHandleStatus">
            <el-radio :label="1">处理中</el-radio>
            <el-radio :label="2">已处理</el-radio>
            <el-radio :label="3">已忽略</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer v-if="currentAlert && currentAlert.status === 0">
        <el-button @click="alertDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAlertHandle">提交处理</el-button>
      </template>
    </el-dialog>

    <!-- Announcement Dialog -->
    <el-dialog v-model="announcementDialogVisible" :title="announcementForm.id ? '编辑公告' : '新建公告'" width="700px">
      <el-form :model="announcementForm" label-width="100px">
        <el-form-item label="公告类型">
          <el-select v-model="announcementForm.announcementType" style="width: 100%">
            <el-option label="系统公告" value="SYSTEM" />
            <el-option label="维护公告" value="MAINTENANCE" />
            <el-option label="新功能" value="FEATURE" />
            <el-option label="活动公告" value="ACTIVITY" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="announcementForm.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="announcementForm.priority" style="width: 100%">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标角色">
          <el-checkbox-group v-model="announcementTargetRoles">
            <el-checkbox label="PATIENT">患者</el-checkbox>
            <el-checkbox label="DOCTOR">医生</el-checkbox>
            <el-checkbox label="ADMIN">管理员</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, ChatLineSquare, Warning, Document } from '@element-plus/icons-vue'
import { adminApi } from '@/api'

// State
const activeTab = ref('notifications')
const loading = ref(false)
const pageSize = ref(10)

// Statistics
const statistics = reactive({
  totalNotifications: 0,
  totalFeedback: 0,
  unhandledAlerts: 0,
  publishedAnnouncements: 0
})

// Notifications
const notificationList = ref<any[]>([])
const notificationPage = ref(1)
const notificationTotal = ref(0)
const notificationDialogVisible = ref(false)
const notificationForm = reactive<any>({
  id: null,
  notificationType: 'SYSTEM',
  title: '',
  content: '',
  priority: 'NORMAL'
})

// Feedback
const feedbackList = ref<any[]>([])
const feedbackPage = ref(1)
const feedbackTotal = ref(0)
const feedbackDialogVisible = ref(false)
const currentFeedback = ref<any>(null)
const replyDialogVisible = ref(false)
const replyContent = ref('')
const currentReplyFeedbackId = ref<number | null>(null)

// Alerts
const alertList = ref<any[]>([])
const alertPage = ref(1)
const alertTotal = ref(0)
const alertDialogVisible = ref(false)
const currentAlert = ref<any>(null)
const alertHandleNote = ref('')
const alertHandleStatus = ref(2)

// Announcements
const announcementList = ref<any[]>([])
const announcementPage = ref(1)
const announcementTotal = ref(0)
const announcementDialogVisible = ref(false)
const announcementForm = reactive<any>({
  id: null,
  announcementType: 'SYSTEM',
  title: '',
  content: '',
  priority: 'NORMAL',
  targetRoles: ''
})
const announcementTargetRoles = ref<string[]>([])

// Load statistics
const loadStatistics = async () => {
  try {
    const res = await adminApi.getMessageStatistics()
    if (res.code === 200 && res.data) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

// Notifications
const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await adminApi.getNotifications({
      pageNum: notificationPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200 && res.data) {
      notificationList.value = res.data.records || []
      notificationTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load notifications:', error)
    ElMessage.error('加载通知列表失败')
  } finally {
    loading.value = false
  }
}

const showNotificationDialog = (notification?: any) => {
  if (notification) {
    Object.assign(notificationForm, notification)
  } else {
    notificationForm.id = null
    notificationForm.notificationType = 'SYSTEM'
    notificationForm.title = ''
    notificationForm.content = ''
    notificationForm.priority = 'NORMAL'
  }
  notificationDialogVisible.value = true
}

const saveNotification = async () => {
  try {
    const res = notificationForm.id
      ? await adminApi.updateNotification(notificationForm.id, notificationForm)
      : await adminApi.createNotification(notificationForm)

    if (res.code === 200) {
      ElMessage.success(res.data || '操作成功')
      notificationDialogVisible.value = false
      loadNotifications()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('Failed to save notification:', error)
    ElMessage.error('操作失败')
  }
}

const sendNotification = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要发送此通知吗？', '提示', { type: 'warning' })
    const res = await adminApi.sendNotification(id)
    if (res.code === 200) {
      ElMessage.success('发送成功')
      loadNotifications()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to send notification:', error)
      ElMessage.error('发送失败')
    }
  }
}

const deleteNotification = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此通知吗？', '警告', { type: 'error' })
    const res = await adminApi.deleteNotification(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadNotifications()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete notification:', error)
      ElMessage.error('删除失败')
    }
  }
}

// Feedback
const loadFeedback = async () => {
  loading.value = true
  try {
    const res = await adminApi.getFeedback({
      pageNum: feedbackPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200 && res.data) {
      feedbackList.value = res.data.records || []
      feedbackTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load feedback:', error)
    ElMessage.error('加载反馈列表失败')
  } finally {
    loading.value = false
  }
}

const viewFeedback = async (feedback: any) => {
  try {
    const res = await adminApi.getFeedbackDetail(feedback.id)
    if (res.code === 200 && res.data) {
      currentFeedback.value = res.data
      feedbackDialogVisible.value = true
    }
  } catch (error) {
    console.error('Failed to load feedback detail:', error)
    ElMessage.error('加载反馈详情失败')
  }
}

const replyFeedback = (feedback: any) => {
  currentReplyFeedbackId.value = feedback.id
  replyContent.value = ''
  replyDialogVisible.value = true
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  try {
    const res = await adminApi.replyFeedback(currentReplyFeedbackId.value!, replyContent.value)
    if (res.code === 200) {
      ElMessage.success('回复成功')
      replyDialogVisible.value = false
      loadFeedback()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '回复失败')
    }
  } catch (error) {
    console.error('Failed to reply feedback:', error)
    ElMessage.error('回复失败')
  }
}

const deleteFeedback = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此反馈吗？', '警告', { type: 'error' })
    const res = await adminApi.deleteFeedback(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadFeedback()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete feedback:', error)
      ElMessage.error('删除失败')
    }
  }
}

// Alerts
const loadAlerts = async () => {
  loading.value = true
  try {
    const res = await adminApi.getAlerts({
      pageNum: alertPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200 && res.data) {
      alertList.value = res.data.records || []
      alertTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load alerts:', error)
    ElMessage.error('加载警报列表失败')
  } finally {
    loading.value = false
  }
}

const viewAlert = async (alert: any) => {
  try {
    const res = await adminApi.getAlertDetail(alert.id)
    if (res.code === 200 && res.data) {
      currentAlert.value = res.data
      alertDialogVisible.value = true
    }
  } catch (error) {
    console.error('Failed to load alert detail:', error)
    ElMessage.error('加载警报详情失败')
  }
}

const handleAlert = async (alert: any) => {
  currentAlert.value = alert
  alertHandleNote.value = ''
  alertHandleStatus.value = 2
  alertDialogVisible.value = true
}

const submitAlertHandle = async () => {
  if (!alertHandleNote.value.trim()) {
    ElMessage.warning('请输入处理备注')
    return
  }

  try {
    const res = await adminApi.handleAlert(
      currentAlert.value.id,
      alertHandleNote.value,
      alertHandleStatus.value
    )
    if (res.code === 200) {
      ElMessage.success('处理成功')
      alertDialogVisible.value = false
      loadAlerts()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '处理失败')
    }
  } catch (error) {
    console.error('Failed to handle alert:', error)
    ElMessage.error('处理失败')
  }
}

const deleteAlert = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此警报吗？', '警告', { type: 'error' })
    const res = await adminApi.deleteAlert(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAlerts()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete alert:', error)
      ElMessage.error('删除失败')
    }
  }
}

// Announcements
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const res = await adminApi.getAnnouncements({
      pageNum: announcementPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200 && res.data) {
      announcementList.value = res.data.records || []
      announcementTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('Failed to load announcements:', error)
    ElMessage.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

const showAnnouncementDialog = (announcement?: any) => {
  if (announcement) {
    Object.assign(announcementForm, announcement)
    announcementTargetRoles.value = announcement.targetRoles ? announcement.targetRoles.split(',') : []
  } else {
    announcementForm.id = null
    announcementForm.announcementType = 'SYSTEM'
    announcementForm.title = ''
    announcementForm.content = ''
    announcementForm.priority = 'NORMAL'
    announcementTargetRoles.value = []
  }
  announcementDialogVisible.value = true
}

const saveAnnouncement = async () => {
  announcementForm.targetRoles = announcementTargetRoles.value.join(',')

  try {
    const res = announcementForm.id
      ? await adminApi.updateAnnouncement(announcementForm.id, announcementForm)
      : await adminApi.createAnnouncement(announcementForm)

    if (res.code === 200) {
      ElMessage.success(res.data || '操作成功')
      announcementDialogVisible.value = false
      loadAnnouncements()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('Failed to save announcement:', error)
    ElMessage.error('操作失败')
  }
}

const publishAnnouncement = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要发布此公告吗？', '提示', { type: 'warning' })
    const res = await adminApi.publishAnnouncement(id)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      loadAnnouncements()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to publish announcement:', error)
      ElMessage.error('发布失败')
    }
  }
}

const unpublishAnnouncement = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要下线此公告吗？', '提示', { type: 'warning' })
    const res = await adminApi.unpublishAnnouncement(id)
    if (res.code === 200) {
      ElMessage.success('下线成功')
      loadAnnouncements()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '下线失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to unpublish announcement:', error)
      ElMessage.error('下线失败')
    }
  }
}

const togglePin = async (id: number) => {
  try {
    const res = await adminApi.toggleAnnouncementPin(id)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadAnnouncements()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('Failed to toggle pin:', error)
    ElMessage.error('操作失败')
  }
}

const deleteAnnouncement = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除此公告吗？', '警告', { type: 'error' })
    const res = await adminApi.deleteAnnouncement(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAnnouncements()
      loadStatistics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete announcement:', error)
      ElMessage.error('删除失败')
    }
  }
}

// Tab change handler
const handleTabChange = (tab: string) => {
  switch (tab) {
    case 'notifications':
      loadNotifications()
      break
    case 'feedback':
      loadFeedback()
      break
    case 'alerts':
      loadAlerts()
      break
    case 'announcements':
      loadAnnouncements()
      break
  }
}

// Helper functions
const getNotificationTypeName = (type: string) => {
  const map: Record<string, string> = {
    SYSTEM: '系统通知',
    PERSONAL: '个人通知',
    BROADCAST: '广播通知'
  }
  return map[type] || type
}

const getNotificationTypeTag = (type: string) => {
  const map: Record<string, any> = {
    SYSTEM: 'primary',
    PERSONAL: 'success',
    BROADCAST: 'warning'
  }
  return map[type] || 'info'
}

const getPriorityTag = (priority: string) => {
  const map: Record<string, any> = {
    LOW: 'info',
    NORMAL: '',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[priority] || ''
}

const getFeedbackTypeName = (type: string) => {
  const map: Record<string, string> = {
    BUG: 'Bug报告',
    FEATURE: '功能建议',
    COMPLAINT: '投诉',
    OTHER: '其他'
  }
  return map[type] || type
}

const getFeedbackTypeTag = (type: string) => {
  const map: Record<string, any> = {
    BUG: 'danger',
    FEATURE: 'success',
    COMPLAINT: 'warning',
    OTHER: 'info'
  }
  return map[type] || 'info'
}

const getFeedbackStatusName = (status: number) => {
  const map: Record<number, string> = {
    0: '待处理',
    1: '处理中',
    2: '已完成',
    3: '已关闭'
  }
  return map[status] || '未知'
}

const getFeedbackStatusTag = (status: number) => {
  const map: Record<number, any> = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return map[status] || 'info'
}

const getAlertLevelTag = (level: string) => {
  const map: Record<string, any> = {
    INFO: 'info',
    WARNING: 'warning',
    ERROR: 'danger',
    CRITICAL: 'danger'
  }
  return map[level] || 'info'
}

const getAlertStatusName = (status: number) => {
  const map: Record<number, string> = {
    0: '未处理',
    1: '处理中',
    2: '已处理',
    3: '已忽略'
  }
  return map[status] || '未知'
}

const getAlertStatusTag = (status: number) => {
  const map: Record<number, any> = {
    0: 'danger',
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return map[status] || 'info'
}

const getAnnouncementTypeName = (type: string) => {
  const map: Record<string, string> = {
    SYSTEM: '系统公告',
    MAINTENANCE: '维护公告',
    FEATURE: '新功能',
    ACTIVITY: '活动公告'
  }
  return map[type] || type
}

const getAnnouncementStatusName = (status: number) => {
  const map: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已下线'
  }
  return map[status] || '未知'
}

const getAnnouncementStatusTag = (status: number) => {
  const map: Record<number, any> = {
    0: 'info',
    1: 'success',
    2: 'warning'
  }
  return map[status] || 'info'
}

// Lifecycle
onMounted(() => {
  loadStatistics()
  loadNotifications()
})
</script>

<style scoped lang="scss">
.messages-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 15px;

    .stat-icon {
      font-size: 40px;
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 5px;
      }
    }
  }
}

.main-card {
  .tab-header {
    margin-bottom: 15px;
  }
}
</style>
