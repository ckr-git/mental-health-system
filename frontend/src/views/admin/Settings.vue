<template>
  <div class="settings-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>⚙️ 系统设置</h2>
          <el-button type="primary" @click="showCreateDialog">新建设置</el-button>
        </div>
      </template>

      <div v-loading="loading">
        <!-- Grouped Settings Display -->
        <el-collapse v-model="activeGroups" accordion>
          <el-collapse-item
            v-for="(items, group) in groupedSettings"
            :key="group"
            :name="group"
          >
            <template #title>
              <div class="group-header">
                <el-icon><Setting /></el-icon>
                <span class="group-name">{{ group }}</span>
                <el-tag size="small" type="info" style="margin-left: 10px">{{ items.length }} 项</el-tag>
              </div>
            </template>

            <el-table :data="items" border stripe>
              <el-table-column prop="configKey" label="配置键" min-width="200" />
              <el-table-column prop="configValue" label="配置值" min-width="200" show-overflow-tooltip />
              <el-table-column prop="valueType" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="getValueTypeTag(row.valueType)">
                    {{ row.valueType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
              <el-table-column prop="editable" label="可编辑" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.editable === 1 ? 'success' : 'info'" size="small">
                    {{ row.editable === 1 ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updateTime" label="更新时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.updateTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button
                    v-if="row.editable === 1"
                    type="primary"
                    size="small"
                    @click="showEditDialog(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-else
                    type="info"
                    size="small"
                    disabled
                  >
                    只读
                  </el-button>
                  <el-button
                    v-if="row.editable === 1"
                    type="danger"
                    size="small"
                    @click="deleteSetting(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>

        <el-empty v-if="Object.keys(groupedSettings).length === 0" description="暂无配置项" />
      </div>
    </el-card>

    <!-- Edit Setting Dialog -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑设置"
      width="600px"
      @close="resetEditForm"
    >
      <el-form :model="currentSetting" label-width="100px">
        <el-form-item label="配置键">
          <el-input v-model="currentSetting.configKey" disabled />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-if="currentSetting.valueType === 'STRING'"
            v-model="currentSetting.configValue"
            placeholder="请输入配置值"
          />
          <el-input-number
            v-else-if="currentSetting.valueType === 'NUMBER'"
            v-model="currentSetting.configValue"
            style="width: 100%"
          />
          <el-switch
            v-else-if="currentSetting.valueType === 'BOOLEAN'"
            v-model="currentSetting.configValue"
            active-value="true"
            inactive-value="false"
          />
          <el-input
            v-else-if="currentSetting.valueType === 'JSON'"
            v-model="currentSetting.configValue"
            type="textarea"
            :rows="6"
            placeholder="请输入JSON格式配置"
          />
          <el-input
            v-else
            v-model="currentSetting.configValue"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentSetting.description" disabled />
        </el-form-item>
        <el-form-item label="类型">
          <el-tag :type="getValueTypeTag(currentSetting.valueType)">
            {{ currentSetting.valueType }}
          </el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSetting" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create Setting Dialog -->
    <el-dialog
      v-model="createDialogVisible"
      title="新建设置"
      width="600px"
      @close="resetCreateForm"
    >
      <el-form :model="newSetting" label-width="100px">
        <el-form-item label="配置键" required>
          <el-input v-model="newSetting.configKey" placeholder="请输入配置键（唯一标识）" />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-if="newSetting.valueType === 'STRING'"
            v-model="newSetting.configValue"
            placeholder="请输入配置值"
          />
          <el-input-number
            v-else-if="newSetting.valueType === 'NUMBER'"
            v-model="newSetting.configValue"
            style="width: 100%"
          />
          <el-switch
            v-else-if="newSetting.valueType === 'BOOLEAN'"
            v-model="newSetting.configValue"
            active-value="true"
            inactive-value="false"
          />
          <el-input
            v-else-if="newSetting.valueType === 'JSON'"
            v-model="newSetting.configValue"
            type="textarea"
            :rows="6"
            placeholder="请输入JSON格式配置"
          />
          <el-input
            v-else
            v-model="newSetting.configValue"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="配置类型" required>
          <el-select v-model="newSetting.valueType" placeholder="请选择类型">
            <el-option label="字符串" value="STRING" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="布尔值" value="BOOLEAN" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置分组" required>
          <el-input v-model="newSetting.groupName" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newSetting.description"
            type="textarea"
            :rows="3"
            placeholder="请输入配置描述"
          />
        </el-form-item>
        <el-form-item label="可编辑">
          <el-switch v-model="newSetting.editable" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createSetting" :loading="saving">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { adminApi } from '@/api'

const loading = ref(false)
const saving = ref(false)
const editDialogVisible = ref(false)
const createDialogVisible = ref(false)
const activeGroups = ref<string[]>([])

const groupedSettings = ref<Record<string, any[]>>({})

const currentSetting = reactive({
  id: null as number | null,
  configKey: '',
  configValue: '',
  description: '',
  valueType: '',
  groupName: '',
  editable: 1
})

const newSetting = reactive({
  configKey: '',
  configValue: '',
  description: '',
  valueType: 'STRING',
  groupName: '',
  editable: 1
})

// Load grouped settings
const loadSettings = async () => {
  loading.value = true
  try {
    const res = await adminApi.getSettingsByGroup()
    if (res.code === 200 && res.data) {
      groupedSettings.value = res.data
      // Auto-expand first group
      const groups = Object.keys(res.data)
      if (groups.length > 0) {
        activeGroups.value = [groups[0]]
      }
    }
  } catch (error) {
    ElMessage.error('加载设置失败')
    console.error('Failed to load settings:', error)
  } finally {
    loading.value = false
  }
}

// Show edit dialog
const showEditDialog = (setting: any) => {
  currentSetting.id = setting.id
  currentSetting.configKey = setting.configKey
  currentSetting.configValue = String(setting.configValue)
  currentSetting.description = setting.description
  currentSetting.valueType = setting.valueType
  currentSetting.groupName = setting.groupName
  currentSetting.editable = setting.editable
  editDialogVisible.value = true
}

// Reset edit form
const resetEditForm = () => {
  currentSetting.id = null
  currentSetting.configKey = ''
  currentSetting.configValue = ''
  currentSetting.description = ''
  currentSetting.valueType = ''
  currentSetting.groupName = ''
  currentSetting.editable = 1
}

// Save setting
const saveSetting = async () => {
  if (!currentSetting.configValue) {
    ElMessage.warning('请填写配置值')
    return
  }

  // Validate JSON if type is JSON
  if (currentSetting.valueType === 'JSON') {
    try {
      JSON.parse(currentSetting.configValue)
    } catch (e) {
      ElMessage.error('配置值不是有效的JSON格式')
      return
    }
  }

  saving.value = true
  try {
    const res = await adminApi.updateSetting(currentSetting.configKey, currentSetting.configValue)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      loadSettings()
    }
  } catch (error) {
    ElMessage.error('更新失败')
    console.error('Failed to update setting:', error)
  } finally {
    saving.value = false
  }
}

// Show create dialog
const showCreateDialog = () => {
  createDialogVisible.value = true
}

// Reset create form
const resetCreateForm = () => {
  newSetting.configKey = ''
  newSetting.configValue = ''
  newSetting.description = ''
  newSetting.valueType = 'STRING'
  newSetting.groupName = ''
  newSetting.editable = 1
}

// Create setting
const createSetting = async () => {
  if (!newSetting.configKey || !newSetting.configValue || !newSetting.groupName) {
    ElMessage.warning('请填写必填项')
    return
  }

  // Validate JSON if type is JSON
  if (newSetting.valueType === 'JSON') {
    try {
      JSON.parse(newSetting.configValue as string)
    } catch (e) {
      ElMessage.error('配置值不是有效的JSON格式')
      return
    }
  }

  saving.value = true
  try {
    const data = {
      configKey: newSetting.configKey,
      configValue: String(newSetting.configValue),
      description: newSetting.description,
      valueType: newSetting.valueType,
      groupName: newSetting.groupName,
      editable: newSetting.editable
    }
    const res = await adminApi.createSetting(data)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      createDialogVisible.value = false
      loadSettings()
    }
  } catch (error) {
    ElMessage.error('创建失败')
    console.error('Failed to create setting:', error)
  } finally {
    saving.value = false
  }
}

// Delete setting
const deleteSetting = async (setting: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置项 "${setting.configKey}" 吗？此操作不可恢复。`,
      '警告',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    const res = await adminApi.deleteSetting(setting.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadSettings()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// Helper functions
const getValueTypeTag = (type: string) => {
  const tagMap: Record<string, string> = {
    'STRING': 'primary',
    'NUMBER': 'success',
    'BOOLEAN': 'warning',
    'JSON': 'info'
  }
  return tagMap[type] || ''
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.settings-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.group-name {
  flex: 1;
}

:deep(.el-collapse-item__header) {
  padding: 15px 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 10px;
}

:deep(.el-collapse-item__content) {
  padding: 15px 20px 20px;
}

:deep(.el-table) {
  margin-top: 0;
}
</style>
