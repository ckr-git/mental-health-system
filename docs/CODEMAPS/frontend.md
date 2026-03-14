<!-- Generated: 2026-03-14 | Files scanned: 110 | Token estimate: ~700 -->

# Frontend Codemap

## Stack
Vue 3 + TypeScript + Vite + Element Plus + Pinia + vue-router + ECharts + Howler.js + STOMP.js

## Page Tree (3 Layouts × N Pages)

### PatientLayout (/patient/*)
```
dashboard      → Dashboard.vue          统计卡+情绪趋势图+快捷操作
mood-diary     → MoodDiary.vue          日记CRUD+天气主题+心情预测
time-capsule   → TimeCapsule.vue        时间胶囊信件
tree-hole      → TreeHole.vue           匿名树洞
room-decoration → RoomDecoration.vue    心灵小屋装饰
ai-chat        → AIChat.vue             AI助手对话
reports        → Reports.vue            评估报告历史
assessments    → Assessments.vue        PHQ-9/GAD-7答题向导 ★Phase2
meditation     → Meditation.vue         7个冥想/呼吸练习 ★Phase3A
appointments   → Appointments.vue       预约管理+等候名单 ★Phase4
doctors        → Doctors.vue            医生列表
chat           → Chat.vue               即时通讯
communication  → Communication.vue      沟通记录
profile        → Profile.vue            档案+紧急联系人+健康档案 ★Phase1
resources      → Resources.vue          心理资源
```

### DoctorLayout (/doctor/*)
```
dashboard      → Dashboard.vue          工作台统计+预约趋势+日程
patients       → Patients.vue           患者管理
patient-pool   → PatientPool.vue        患者公海
reports        → Reports.vue            评估报告
crisis-alerts  → CrisisAlerts.vue       危机告警列表+处置 ★Phase3
chat           → Chat.vue               在线咨询
consultations  → Consultation.vue       咨询管理
appointments   → Appointments.vue       预约管理
scheduling     → Scheduling.vue         排班设置+特殊日期 ★Phase4
treatment-plans → TreatmentPlans.vue    治疗计划+目标+干预+SOAP ★Phase5
profile        → Profile.vue            个人资料
```

### AdminLayout (/admin/*)
```
dashboard      → Dashboard.vue          数据概览+用户趋势
users          → Users.vue              用户CRUD+状态管理
doctors        → Doctors.vue            医生管理
resources      → Resources.vue          资源管理
appointments   → Appointments.vue       预约管理
messages       → Messages.vue           消息管理
statistics     → Statistics.vue         统计分析
settings       → Settings.vue           系统设置
```

## State Management (Pinia Stores)
```
stores/user.ts          → token, userInfo, login/logout
stores/assessment.ts    → scales, activeSession, answers, history
stores/notification.ts  → unreadCount, notifications
stores/profile.ts       → profileData, aggregate load/save
```

## API Modules (frontend/src/api/modules/)
```
auth.ts, user.ts, analytics.ts, scheduling.ts, treatment.ts, notification.ts
patient/: ai, assessment, capsule, chat, comment, dashboard, diary, meditation, profile, report, resource, room, treeHole
doctor/: index, chat, crisis
admin/: index
```

## Key Components
```
components/MoodDiary/     → 20+ components (WeatherBackground, DiaryCard, RoomCanvas, ThemeSelector...)
components/notifications/ → NotificationBell.vue (bell icon + drawer)
composables/useStompClient.ts → WebSocket STOMP with JWT auth
```

## Route Guard
router/index.ts: beforeEach checks token expiry + role matching → redirect to /login or role home
