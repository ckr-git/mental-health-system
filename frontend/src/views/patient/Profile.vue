<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="profile-header">
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :before-upload="handleAvatarUpload"
            >
              <el-avatar :size="100" :src="userInfo.avatar || '/default-avatar.jpg'">
                <el-icon :size="50"><UserFilled /></el-icon>
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </el-upload>
            <h3>{{ userInfo.name || '未设置' }}</h3>
            <p class="user-phone">{{ userInfo.phone }}</p>
          </div>

          <el-divider />

          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ stats.symptomsCount }}</div>
              <div class="stat-label">情绪日记</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.reportsCount }}</div>
              <div class="stat-label">评估报告</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.daysCount }}</div>
              <div class="stat-label">使用天数</div>
            </div>
          </div>
        </el-card>

        <el-card class="info-card" style="margin-top: 20px">
          <template #header>
            <span>账户安全</span>
          </template>
          <div class="security-item" @click="showPasswordDialog = true">
            <div>
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <div class="security-item" @click="showPhoneDialog = true">
            <div>
              <el-icon><Iphone /></el-icon>
              <span>更换手机</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-button v-if="!editing" type="primary" size="small" @click="editing = true">编辑</el-button>
              <div v-else>
                <el-button size="small" @click="cancelEdit">取消</el-button>
                <el-button type="primary" size="small" @click="saveProfile">保存</el-button>
              </div>
            </div>
          </template>

          <el-form :model="formData" label-width="100px" :disabled="!editing">
            <el-form-item label="姓名">
              <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="formData.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="formData.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="年龄">
              <el-input-number v-model="formData.age" :min="1" :max="120" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="职业">
              <el-input v-model="formData.occupation" placeholder="请输入职业" />
            </el-form-item>
            <el-form-item label="婚姻状况">
              <el-select v-model="formData.maritalStatus" placeholder="请选择">
                <el-option label="未婚" value="SINGLE" />
                <el-option label="已婚" value="MARRIED" />
                <el-option label="离异" value="DIVORCED" />
                <el-option label="丧偶" value="WIDOWED" />
              </el-select>
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input
                v-model="formData.introduction"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己..."
              />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="info-card" style="margin-top: 20px">
          <template #header>
            <span>紧急联系人</span>
          </template>
          <el-form :model="formData" label-width="120px" :disabled="!editing">
            <el-form-item label="联系人姓名">
              <el-input v-model="formData.emergencyContactName" placeholder="请输入紧急联系人姓名" />
            </el-form-item>
            <el-form-item label="联系人电话">
              <el-input v-model="formData.emergencyContactPhone" placeholder="请输入紧急联系人电话" />
            </el-form-item>
            <el-form-item label="与您的关系">
              <el-select v-model="formData.emergencyContactRelation" placeholder="请选择关系">
                <el-option label="父母" value="PARENT" />
                <el-option label="配偶" value="SPOUSE" />
                <el-option label="子女" value="CHILD" />
                <el-option label="兄弟姐妹" value="SIBLING" />
                <el-option label="朋友" value="FRIEND" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="info-card" style="margin-top: 20px">
          <template #header>
            <span>健康档案</span>
          </template>
          <el-form :model="formData" label-width="120px" :disabled="!editing">
            <el-form-item label="病史">
              <el-input
                v-model="formData.medicalHistory"
                type="textarea"
                :rows="3"
                placeholder="请描述既往病史"
              />
            </el-form-item>
            <el-form-item label="过敏史">
              <el-input
                v-model="formData.allergyHistory"
                type="textarea"
                :rows="2"
                placeholder="请描述过敏史"
              />
            </el-form-item>
            <el-form-item label="家族病史">
              <el-input
                v-model="formData.familyHistory"
                type="textarea"
                :rows="2"
                placeholder="请描述家族病史"
              />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="info-card" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>🎵 音效设置</span>
            </div>
          </template>
          <div class="sound-settings">
            <div class="setting-item">
              <div class="setting-label">
                <el-icon><Headset /></el-icon>
                <span>环境音效</span>
                <el-tooltip content="根据天气类型自动播放环境音" placement="top">
                  <el-icon class="info-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-switch
                v-model="soundSettings.ambientEnabled"
                @change="handleSoundSettingChange"
              />
            </div>

            <div class="setting-item">
              <div class="setting-label">
                <el-icon><Notification /></el-icon>
                <span>交互音效</span>
                <el-tooltip content="按钮点击、卡片翻转等操作音效" placement="top">
                  <el-icon class="info-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-switch
                v-model="soundSettings.interactionEnabled"
                @change="handleSoundSettingChange"
              />
            </div>

            <div class="setting-item">
              <div class="setting-label">
                <el-icon><MagicStick /></el-icon>
                <span>音量控制</span>
              </div>
              <div class="volume-control">
                <el-slider
                  v-model="soundSettings.volume"
                  :min="0"
                  :max="100"
                  :show-tooltip="true"
                  :format-tooltip="(val: number) => `${val}%`"
                  @change="handleVolumeChange"
                  style="width: 200px"
                />
                <span class="volume-value">{{ soundSettings.volume }}%</span>
              </div>
            </div>

            <div class="setting-hint">
              <el-alert
                title="提示"
                type="info"
                :closable="false"
                show-icon
              >
                音效会根据当前天气和操作自动播放，增强沉浸感体验
              </el-alert>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="changePassword" :loading="passwordLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 更换手机对话框 -->
    <el-dialog v-model="showPhoneDialog" title="更换手机号" width="500px">
      <el-form :model="phoneForm" ref="phoneFormRef" label-width="100px">
        <el-form-item label="新手机号" prop="newPhone">
          <el-input v-model="phoneForm.newPhone" placeholder="请输入新手机号" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div style="display: flex; gap: 10px">
            <el-input v-model="phoneForm.code" placeholder="请输入验证码" style="flex: 1" />
            <el-button @click="sendCode" :disabled="countdown > 0">
              {{ countdown > 0 ? `${countdown}秒` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" @click="changePhone" :loading="phoneLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import {
  UserFilled, Camera, Lock, Iphone, ArrowRight,
  Headset, Notification, MagicStick, QuestionFilled
} from '@element-plus/icons-vue'
import { useProfile } from './profile/useProfile'

const {
  userInfo, stats, editing, formData,
  showPasswordDialog, passwordForm, passwordFormRef, passwordLoading,
  showPhoneDialog, phoneForm, phoneFormRef, phoneLoading, countdown,
  soundSettings, passwordRules,
  loadProfile, loadStats, saveProfile, cancelEdit,
  handleAvatarUpload, changePassword, sendCode, changePhone,
  handleSoundSettingChange, handleVolumeChange
} = useProfile()

onMounted(() => { loadProfile(); loadStats() })
</script>

<style scoped lang="scss">
.profile-container {
  padding: 20px;
}

.profile-card {
  .profile-header {
    text-align: center;

    .avatar-uploader {
      position: relative;
      display: inline-block;
      cursor: pointer;

      &:hover .avatar-overlay {
        opacity: 1;
      }

      .avatar-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: rgba(0, 0, 0, 0.6);
        color: white;
        border-radius: 50%;
        opacity: 0;
        transition: opacity 0.3s;
        gap: 5px;
        font-size: 12px;
      }
    }

    h3 {
      margin: 15px 0 5px 0;
      font-size: 20px;
    }

    .user-phone {
      color: #909399;
      margin: 0;
    }
  }

  .profile-stats {
    display: flex;
    justify-content: space-around;
    text-align: center;

    .stat-item {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #409EFF;
        margin-bottom: 5px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }
}

.info-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .security-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 0;
    cursor: pointer;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      color: #409EFF;
    }

    div {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

// 音效设置样式
.sound-settings {
  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-of-type {
      border-bottom: none;
    }

    .setting-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      color: #303133;

      .el-icon {
        font-size: 18px;
        color: #409EFF;
      }

      .info-icon {
        font-size: 16px;
        color: #909399;
        cursor: help;

        &:hover {
          color: #409EFF;
        }
      }
    }

    .volume-control {
      display: flex;
      align-items: center;
      gap: 16px;

      .volume-value {
        min-width: 40px;
        text-align: right;
        font-size: 14px;
        color: #606266;
        font-weight: 500;
      }
    }
  }

  .setting-hint {
    margin-top: 20px;
  }
}
</style>
