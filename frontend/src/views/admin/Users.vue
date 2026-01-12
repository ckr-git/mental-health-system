<template>
  <div class="users-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户角色">
          <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 120px">
            <el-option label="患者" value="PATIENT" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="姓名/手机号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUsers">搜索</el-button>
          <el-button type="success" @click="showAddDialog">新增用户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-table :data="users" style="width: 100%; margin-top: 20px" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)" size="small">
            {{ getRoleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">
          {{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="160" />
      <el-table-column label="操作" fixed="right" width="240">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="editUser(row)">编辑</el-button>
          <el-button 
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'" 
            size="small" 
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
          <el-button type="danger" size="small" @click="deleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="loadUsers"
      @size-change="loadUsers"
      style="margin-top: 20px; text-align: center"
    />

    <!-- 新增/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="患者" value="PATIENT" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio label="MALE">男</el-radio>
            <el-radio label="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'
import type { User } from '@/types'

const searchForm = ref({
  role: '',
  status: '',
  keyword: ''
})

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const formData = reactive<Partial<User>>({})
const formRef = ref()
const submitting = ref(false)

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers({
      pageNum: page.value,
      pageSize: pageSize.value,
      role: searchForm.value.role,
      keyword: searchForm.value.keyword
    })

    if (res.code === 200 && res.data) {
      users.value = res.data.records.map((user: any) => ({
        id: user.id,
        name: user.nickname || user.username,
        phone: user.phone || '-',
        role: user.role,
        gender: user.gender === 1 ? 'MALE' : user.gender === 2 ? 'FEMALE' : 'UNKNOWN',
        age: user.age || '-',
        status: user.status === 1 ? 'ACTIVE' : 'DISABLED',
        createTime: user.createTime,
        ...user
      }))
      total.value = res.data.total
    }
  } catch (error) {
    console.error('加载用户列表失败', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  Object.keys(formData).forEach(key => delete formData[key as keyof typeof formData])
  dialogVisible.value = true
}

const editUser = (user: User) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  Object.assign(formData, user)
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        // 暂不支持编辑
        ElMessage.warning('暂不支持编辑用户，请使用禁用/启用功能')
      } else {
        // 暂不支持新增
        ElMessage.warning('暂不支持新增用户，请使用注册功能')
      }
      dialogVisible.value = false
    } catch (error) {
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const toggleStatus = async (user: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要${user.status === 'ACTIVE' ? '禁用' : '启用'}该用户吗？`,
      '提示',
      { type: 'warning' }
    )

    const res = await adminApi.toggleUserStatus(user.id)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      loadUsers()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换状态失败', error)
      ElMessage.error('操作失败')
    }
  }
}

const deleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复！', '警告', {
      type: 'error',
      confirmButtonText: '确定删除'
    })

    const res = await adminApi.deleteUser(user.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败', error)
      ElMessage.error('删除失败')
    }
  }
}

const getRoleText = (role: string) => {
  const map: Record<string, string> = {
    PATIENT: '患者',
    DOCTOR: '医生',
    ADMIN: '管理员'
  }
  return map[role] || role
}

const getRoleType = (role: string): any => {
  const map: Record<string, string> = {
    PATIENT: 'primary',
    DOCTOR: 'success',
    ADMIN: 'danger'
  }
  return map[role] || 'info'
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.users-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}
</style>
