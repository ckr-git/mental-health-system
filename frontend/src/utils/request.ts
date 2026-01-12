import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { ApiResponse } from '@/types'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000  // 增加到60秒，用于AI报告生成
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        // 延迟跳转，给组件处理错误的时间
        setTimeout(() => {
          if (router.currentRoute.value.path !== '/login') {
            router.push('/login')
          }
        }, 100)
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  (error) => {
    console.error('Response error:', error)

    if (error.response?.status === 401) {
      // 静默处理401错误，不显示重复提示
      const userStore = useUserStore()
      userStore.logout()
      // 延迟跳转，给组件处理错误的时间
      setTimeout(() => {
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
      }, 100)
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }

    return Promise.reject(error)
  }
)

export default service
