<template>
  <div class="symptom-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>情绪日记</span>
          <el-button type="primary" @click="dialogVisible = true">添加记录</el-button>
        </div>
      </template>

      <el-table :data="records" style="width: 100%" v-loading="loading">
        <el-table-column prop="symptomType" label="症状类型" width="120" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityType(row.severity)">{{ row.severity }}/10</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moodScore" label="心情" width="80" />
        <el-table-column prop="sleepQuality" label="睡眠" width="80" />
        <el-table-column prop="stressLevel" label="压力" width="80" />
        <el-table-column prop="createTime" label="记录时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button type="danger" link @click="deleteRecord(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRecords"
        @current-change="loadRecords"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 添加记录对话框 -->
    <el-dialog v-model="dialogVisible" title="添加情绪日记" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="症状类型" prop="symptomType">
          <el-select v-model="form.symptomType" placeholder="请选择症状类型">
            <el-option label="焦虑" value="焦虑" />
            <el-option label="抑郁" value="抑郁" />
            <el-option label="失眠" value="失眠" />
            <el-option label="压力" value="压力" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="症状描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请详细描述您的症状" />
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-slider v-model="form.severity" :min="1" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="心情评分" prop="moodScore">
          <el-slider v-model="form.moodScore" :min="1" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="精力水平" prop="energyLevel">
          <el-slider v-model="form.energyLevel" :min="1" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="睡眠质量" prop="sleepQuality">
          <el-slider v-model="form.sleepQuality" :min="1" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="压力水平" prop="stressLevel">
          <el-slider v-model="form.stressLevel" :min="1" :max="10" show-stops />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { symptomApi } from '@/api'
import type { SymptomRecord } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const records = ref<SymptomRecord[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const form = reactive<SymptomRecord>({
  symptomType: '',
  description: '',
  severity: 5,
  moodScore: 5,
  energyLevel: 5,
  sleepQuality: 5,
  stressLevel: 5
})

const rules: FormRules = {
  symptomType: [{ required: true, message: '请选择症状类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入症状描述', trigger: 'blur' }]
}

const getSeverityType = (severity: number) => {
  if (severity <= 3) return 'success'
  if (severity <= 6) return 'warning'
  return 'danger'
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await symptomApi.getList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    records.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('Failed to load records:', error)
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await symptomApi.addRecord(form)
        ElMessage.success('记录添加成功')
        dialogVisible.value = false
        loadRecords()
        resetForm()
      } catch (error) {
        console.error('Failed to add record:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const resetForm = () => {
  Object.assign(form, {
    symptomType: '',
    description: '',
    severity: 5,
    moodScore: 5,
    energyLevel: 5,
    sleepQuality: 5,
    stressLevel: 5
  })
}

const viewDetail = (record: SymptomRecord) => {
  ElMessageBox.alert(
    `<p><strong>症状类型：</strong>${record.symptomType}</p>
     <p><strong>描述：</strong>${record.description}</p>
     <p><strong>严重程度：</strong>${record.severity}/10</p>
     <p><strong>心情：</strong>${record.moodScore}/10</p>
     <p><strong>睡眠：</strong>${record.sleepQuality}/10</p>
     <p><strong>压力：</strong>${record.stressLevel}/10</p>`,
    '症状详情',
    { dangerouslyUseHTMLString: true }
  )
}

const deleteRecord = (id: number | undefined) => {
  if (!id) return
  
  ElMessageBox.confirm('确定要删除此记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await symptomApi.deleteRecord(id)
    ElMessage.success('删除成功')
    loadRecords()
  })
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.symptom-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
