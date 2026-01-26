import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/patient'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue')
    },
    // 患者端路由
    {
      path: '/patient',
      component: () => import('@/layouts/PatientLayout.vue'),
      meta: { requiresAuth: true, role: 'PATIENT' },
      children: [
        {
          path: '',
          redirect: '/patient/dashboard'
        },
        {
          path: 'dashboard',
          name: 'PatientDashboard',
          component: () => import('@/views/patient/Dashboard.vue')
        },
        {
          path: 'mood-diary',
          name: 'MoodDiary',
          component: () => import('@/views/patient/MoodDiary.vue')
        },
        {
          path: 'time-capsule',
          name: 'TimeCapsule',
          component: () => import('@/views/patient/TimeCapsule.vue')
        },
        {
          path: 'tree-hole',
          name: 'TreeHole',
          component: () => import('@/views/patient/TreeHole.vue')
        },
        {
          path: 'room-decoration',
          name: 'RoomDecoration',
          component: () => import('@/views/patient/RoomDecoration.vue')
        },
        {
          path: 'resources',
          name: 'Resources',
          component: () => import('@/views/patient/Resources.vue')
        },
        {
          path: 'ai-chat',
          name: 'AIChat',
          component: () => import('@/views/patient/AIChat.vue')
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('@/views/patient/Reports.vue')
        },
        {
          path: 'doctors',
          name: 'Doctors',
          component: () => import('@/views/patient/Doctors.vue')
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/patient/Chat.vue')
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/patient/Profile.vue')
        },
        {
          path: 'communication',
          name: 'Communication',
          component: () => import('@/views/patient/Communication.vue')
        }
      ]
    },
    // 医生端路由
    {
      path: '/doctor',
      component: () => import('@/layouts/DoctorLayout.vue'),
      meta: { requiresAuth: true, role: 'DOCTOR' },
      children: [
        {
          path: '',
          redirect: '/doctor/dashboard'
        },
        {
          path: 'dashboard',
          name: 'DoctorDashboard',
          component: () => import('@/views/doctor/Dashboard.vue')
        },
        {
          path: 'patients',
          name: 'Patients',
          component: () => import('@/views/doctor/Patients.vue')
        },
        {
          path: 'reports',
          name: 'DoctorReports',
          component: () => import('@/views/doctor/Reports.vue')
        },
        {
          path: 'chat',
          name: 'DoctorChat',
          component: () => import('@/views/doctor/Chat.vue')
        },
        {
          path: 'appointments',
          name: 'DoctorAppointments',
          component: () => import('@/views/doctor/Appointments.vue')
        },
        {
          path: 'profile',
          name: 'DoctorProfile',
          component: () => import('@/views/doctor/Profile.vue')
        },
        {
          path: 'patient-pool',
          name: 'PatientPool',
          component: () => import('@/views/doctor/PatientPool.vue')
        },
        {
          path: 'consultations',
          name: 'Consultations',
          component: () => import('@/views/doctor/Consultation.vue')
        }
      ]
    },
    // 管理员端路由
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue')
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/admin/Users.vue')
        },
        {
          path: 'resources',
          name: 'AdminResources',
          component: () => import('@/views/admin/Resources.vue')
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/admin/Statistics.vue')
        },
        {
          path: 'doctors',
          name: 'AdminDoctors',
          component: () => import('@/views/admin/Doctors.vue')
        },
        {
          path: 'appointments',
          name: 'AdminAppointments',
          component: () => import('@/views/admin/Appointments.vue')
        },
        {
          path: 'messages',
          name: 'AdminMessages',
          component: () => import('@/views/admin/Messages.vue')
        },
        {
          path: 'settings',
          name: 'AdminSettings',
          component: () => import('@/views/admin/Settings.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    if (!userStore.token) {
      next('/login')
    } else if (to.meta.role && userStore.userInfo?.role !== to.meta.role) {
      // 角色不匹配，重定向到对应角色的首页
      const role = userStore.userInfo?.role
      const rolePath = typeof role === 'string' ? role.toLowerCase() : 'patient'
      next(`/${rolePath}`)
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
