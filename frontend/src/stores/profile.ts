import { defineStore } from 'pinia'
import { ref } from 'vue'
import { profileApi } from '@/api'
import type { ProfileAggregate, UpdateProfileCommand } from '@/types'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<ProfileAggregate | null>(null)
  const loading = ref(false)

  const loadProfile = async () => {
    loading.value = true
    try {
      const res = await profileApi.getAggregateProfile()
      if (res.code === 200) {
        profile.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  const updateProfile = async (cmd: UpdateProfileCommand) => {
    loading.value = true
    try {
      const res = await profileApi.updateAggregateProfile(cmd)
      if (res.code === 200) {
        profile.value = res.data
      }
      return res
    } finally {
      loading.value = false
    }
  }

  const clearProfile = () => {
    profile.value = null
  }

  return {
    profile,
    loading,
    loadProfile,
    updateProfile,
    clearProfile
  }
})
