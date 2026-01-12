<template>
  <div class="doctors-container">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="专长领域">
          <el-select v-model="searchForm.specialty" placeholder="全部" clearable style="width: 150px">
            <el-option label="焦虑症" value="anxiety" />
            <el-option label="抑郁症" value="depression" />
            <el-option label="压力管理" value="stress" />
            <el-option label="睡眠障碍" value="sleep" />
            <el-option label="情绪管理" value="emotion" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生姓名">
          <el-input v-model="searchForm.keyword" placeholder="搜索医生" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDoctors">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :xs="24" :sm="12" :md="8" v-for="doctor in doctors" :key="doctor.id" style="margin-bottom: 20px">
        <el-card shadow="hover" class="doctor-card">
          <div class="doctor-header">
            <el-avatar :size="80" :src="doctor.avatar || '/default-avatar.jpg'">
              <el-icon :size="40"><UserFilled /></el-icon>
            </el-avatar>
            <div class="doctor-info">
              <h3>{{ doctor.name }}</h3>
              <el-tag size="small">{{ doctor.title || '主治医师' }}</el-tag>
              <div class="rating">
                <el-rate v-model="doctor.rating" disabled show-score text-color="#ff9900" />
              </div>
            </div>
          </div>

          <el-divider />

          <div class="doctor-details">
            <div class="detail-item">
              <span class="label">专长：</span>
              <span class="value">{{ doctor.specialty || '心理咨询' }}</span>
            </div>
            <div class="detail-item">
              <span class="label">经验：</span>
              <span class="value">{{ doctor.experience || '5' }} 年</span>
            </div>
            <div class="detail-item">
              <span class="label">咨询次数：</span>
              <span class="value">{{ doctor.consultCount || 0 }} 次</span>
            </div>
            <div class="detail-item intro">
              <span class="label">简介：</span>
              <p class="value">{{ doctor.introduction || '暂无介绍' }}</p>
            </div>
          </div>

          <div class="doctor-actions">
            <el-button type="primary" @click="contactDoctor(doctor)" style="width: 100%">
              <el-icon><ChatDotRound /></el-icon> 联系医生
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="doctors.length === 0 && !loading" description="暂无医生信息" />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadDoctors"
      style="margin-top: 20px; text-align: center"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, ChatDotRound } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import type { User } from '@/types'

const router = useRouter()

const searchForm = ref({
  specialty: '',
  keyword: ''
})

const doctors = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(9)
const total = ref(0)

const loadDoctors = async () => {
  loading.value = true
  try {
    const { data } = await userApi.getDoctorList({
      page: page.value,
      size: pageSize.value,
      specialty: searchForm.value.specialty,
      keyword: searchForm.value.keyword
    })
    doctors.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error('加载医生列表失败')
  } finally {
    loading.value = false
  }
}

const contactDoctor = (doctor: User) => {
  router.push({
    path: '/patient/chat',
    query: { doctorId: doctor.id }
  })
}

onMounted(() => {
  loadDoctors()
})
</script>

<style scoped lang="scss">
.doctors-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.doctor-card {
  height: 100%;
  
  .doctor-header {
    display: flex;
    gap: 15px;
    align-items: center;

    .doctor-info {
      flex: 1;

      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
      }

      .rating {
        margin-top: 8px;
      }
    }
  }

  .doctor-details {
    margin: 15px 0;

    .detail-item {
      margin-bottom: 10px;
      font-size: 14px;

      .label {
        color: #909399;
        margin-right: 8px;
      }

      .value {
        color: #606266;
      }

      &.intro {
        .value {
          display: block;
          margin-top: 5px;
          line-height: 1.6;
          color: #909399;
        }
      }
    }
  }

  .doctor-actions {
    margin-top: 15px;
  }
}
</style>
