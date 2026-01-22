# 智能心理健康管理系统

> 基于 Spring Boot 3 + Vue 3 + MySQL 的多角色心理健康平台，聚合 AI 咨询、情绪日记、主题房间、医患沟通与后台运营能力。

**最后更新**: 2025-11-16 | **版本**: v1.0.0

---

## 项目统计

| 类别 | 数量 | 说明 |
|------|------|------|
| Java 类文件 | 109 | 后端业务逻辑实现 |
| Entity 实体类 | 25 | 数据模型定义 |
| Controller 控制器 | 22 | REST API 端点 |
| Service 服务类 | 22 | 核心业务逻辑 |
| Mapper 接口 | 23 | MyBatis-Plus 数据访问层 |
| Vue 页面 | 32 | 患者14 + 医生8 + 管理员8 + 公共2 |
| Vue 组件 | 25 | 可复用 UI 组件 |
| 数据库表 | 20+ | 核心业务数据存储 |
| API 接口 | 100+ | RESTful API 端点 |

---

## 目录

- [快速体验](#快速体验)
- [系统概览](#系统概览)
- [技术栈](#技术栈)
- [核心功能地图](#核心功能地图)
- [医患关系管理](#医患关系管理)
- [架构与目录](#架构与目录)
- [运行与调试](#运行与调试)
- [配置说明](#配置说明)
- [数据库与脚本](#数据库与脚本)
- [前端说明](#前端说明)
- [服务模块与接口](#服务模块与接口)
- [AI 与算法能力](#ai-与算法能力)
- [实时通信与调度](#实时通信与调度)
- [测试与质量保障](#测试与质量保障)
- [已知问题与待完善](#已知问题与待完善)
- [协作资料](#协作资料)
- [更新日志](#更新日志)

---

## 快速体验

### 测试账号

| 角色 | 账号 | 密码 | 功能说明 |
|------|------|------|----------|
| **患者** | 患者1 | 123456 | 情绪日记、AI咨询、时间胶囊、心情树洞 |
| **医生** | ooo | 123456 | 患者管理、评估报告、患者公海认领 |
| **管理员** | admin | 123456 | 用户管理、系统设置、审批管理 |

### 快速启动

```bash
# 1. 启动后端
cd E:\ddd\智能心里健康管理系统
mvn spring-boot:run

# 2. 启动前端
cd frontend
npm install
npm run dev

# 3. 访问地址
# 前端: http://localhost:3000 或 http://localhost:5173
# 后端: http://localhost:8080
```

---

## 系统概览

- 后端入口 `src/main/java/com/mental/health/MentalHealthApplication.java`，基于 Spring 分层（controller / service / mapper / security / scheduler / algorithm / common）。
- 配置位于 `src/main/resources/application.yml`，集中管理端口、MySQL、JWT、AI、文件存储、日志；可通过新增 `application-local.yml` 或环境变量覆盖敏感值。
- 前端在 `frontend/`，使用 Vite + TypeScript + Element Plus，按角色划分布局（患者/医生/管理员），公共逻辑集中在 `src/utils` 与 `src/stores`。
- 核心数据表、初始化脚本与测试账号脚本位于仓库根目录 (`database.sql`、`phase1-database.sql`、`create-test-accounts.sql` 等)。
- API 均以 `/api/<角色>/<资源>` 命名并返回 `com.mental.health.common.Result`，WebSocket 入口 `/ws` 使用 STOMP + SockJS。

---

## 技术栈

| 层级 | 使用技术 | 说明 |
|------|----------|------|
| **后端** | Spring Boot 3.2, Spring Security + JWT, MyBatis-Plus 3.5.5, Hutool, Apache Commons Math | 提供 REST API、鉴权、ORM、工具集与协同过滤计算 |
| **数据** | MySQL 8.0, Redis（预留）, Flyway/脚本导入 | `database.sql` 定义 20+ 张业务表，`phase1-database.sql` 等脚本负责分阶段初始化 |
| **前端** | Vue 3 + TypeScript, Vite, Element Plus, Pinia, Axios, ECharts, SockJS + StompJS, Howler | 构建多角色 SPA、可视化与实时通讯，包含声音/触觉/性能工具 |
| **脚本** | PowerShell、Batch、SQL | `start-backend.bat`、`test-all-apis.ps1`、`create-test-accounts.sql` 等简化运维与验证 |

### 主要依赖版本

**后端:**
- Java 17 (JDK)
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- JWT (io.jsonwebtoken) 0.12.3
- Hutool 5.8.23
- Apache Commons Math 3.6.1

**前端:**
- Vue 3.4.0
- Vite 5.0.0
- TypeScript 5.3.0
- Element Plus 2.5.0
- Pinia 2.1.7
- Axios 1.6.2
- ECharts 5.4.3
- Howler 2.2.4

---

## 核心功能地图

### 患者体验 (15个页面)

- **账号体系**：注册/登录/注销、个人资料维护、修改密码、医生名录浏览 (`UserController`)。
- **情绪与症状追踪**：`MoodDiaryController` / `SymptomRecordController` 支持写日记、管理症状、查看历史、统计最近 7/30 天趋势以及天气背景自动匹配。
- **互动社区**：`MoodCommentController` 允许在情绪日记下评论、表态并计入主题解锁数据；`TreeHoleController` 提供条件解锁、心情阈值控制的树洞记录，后台定期清理过期内容。
- **时间胶囊**：`TimeCapsuleController` 负责写信、定时解锁、AI 推荐信件类型、回信与阅读历史；`TimeCapsuleService` 自动检查可解锁条目并记录情绪触发信息。
- **房间与主题**：`RoomDecorationController` + `UserThemeConfigService` 搭建 13+ 装饰、8 套主题的收藏、拖拽、解锁与白/黑天光模式统计，配合 `soundService.ts`、`hapticService.ts` 提升沉浸感。
- **AI 服务**：`AIController` 面向问答、聊天记录、赞踩反馈以及 AI 生成综合评估报告（拉取最近日记作为上下文，支持 DashScope 和 mock）。
- **资源推荐**：`RecommendationController` 联合 `CollaborativeFiltering` 算法与 `mental_resource` / `resource_view_record` 数据，推送文章、音频、视频等内容并记录交互行为。
- **医患沟通**：`ChatController` + WebSocket 支持实时沟通，患者端 `Chat.vue` / `Communication.vue` 搭配 STOMP 订阅、状态提示。

### 医生工作台 (8个页面)

- `DoctorController` 聚合仪表盘统计（患者数、今日/待处理会话、报告数量）、患者分页、患者详情（最近日记、症状等）与报告列表。
- **患者公海**：浏览未分配医生的患者列表，支持关键词搜索和分页。
- **患者认领**：医生可提交认领申请，填写认领理由，等待管理员审核。
- **患者释放**：对现有患者发起释放申请，解除医患关系，患者回归公海。
- **在线咨询管理**：查看所有咨询会话、按状态筛选、结束会话、统计会话时长。
- 支持创建/编辑/删除评估报告、查看单条报告、筛查患者报告数据，并获取近期预约列表。
- `/api/doctor/appointments` 提供医生视角的预约查询与统计，保证医生仅能查看所属患者数据。

### 管理员能力 (8个页面)

- **用户 & 医生审核**：`UserController`、`UserService` 支持分页查询、启停账号、审批医生入驻、更新医生信息、统计各角色数量。
- **医患关系审核**：审核医生的认领/释放申请，通过或拒绝请求，记录审批意见。
- **预约管理**：`AppointmentController` 提供分页查询、详情、创建/更新/确认/取消/完成/删除以及趋势、医生/患者维度统计。
- **通知中心**：`MessageController` 管理系统通知、公告、预警与用户反馈（CRUD、发布、置顶、统计），并借助 `SystemNotificationMapper` 等持久层。
- **系统设置**：`SystemSettingsController` 操作键值配置（按分组加载、控制是否可编辑、CRUD），用于动态调整全局参数。
- **资源/脚本预留**：`MentalResourceController` 暂留公开资源接口骨架，未来更新需同步 `sql/` 与 PowerShell 脚本。

### 智能引擎

- **AI 问答与评估**：`AIService` 支持上下文拼接、调用阿里 DashScope（或 mock）、入库 `ai_conversation`，并自动生成多维度心理评估报告（emotion/depression/stress/sleep/social/overall）。
- **协同过滤推荐**：`CollaborativeFiltering` 负责计算余弦相似度、邻居筛选、评分预测，供 `RecommendationService` 推送 Top-N 资源。
- **主题成长体系**：`UserThemeConfigService` 统计日记/评论/信件数量、连续打卡/夜间模式次数，按月份/成就解锁节日与特殊主题，驱动前端视觉效果。
- **房间装饰解锁**：`RoomDecorationService` 依据日记连击、心情状态、时间胶囊数量等条件发放装饰，并自动处理重复记录、默认激活和坐标/缩放。

---

## 医患关系管理

### 核心特性

- **患者公海机制**：未分配的患者进入公海，医生可主动认领
- **审批工作流**：所有认领/释放操作需管理员审核
- **关系约束**：一个医生最多管理10个患者，一个患者只能分配给1个医生
- **在线咨询**：医生可管理与患者的咨询会话

### 数据库表

**医患关系表 (patient_doctor_relationship)**

```sql
CREATE TABLE patient_doctor_relationship (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    relationship_status VARCHAR(20) DEFAULT 'active' COMMENT '关系状态: active-活跃, inactive-已释放',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_patient_active (patient_id, deleted, relationship_status)
);
```

**患者分配变更请求表 (relationship_change_request)**

```sql
CREATE TABLE relationship_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型: claim-认领, release-释放',
    request_status VARCHAR(20) DEFAULT 'pending' COMMENT '请求状态: pending-待审核, approved-通过, rejected-拒绝',
    request_reason TEXT COMMENT '申请理由',
    admin_id BIGINT COMMENT '审核管理员ID',
    admin_note TEXT COMMENT '管理员审核备注',
    approval_time DATETIME COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### API 端点

**医生端:**
- `GET /api/doctor/patient-pool` - 获取患者公海列表
- `POST /api/doctor/patient-pool/claim/{patientId}` - 认领患者
- `POST /api/doctor/patients/{patientId}/release` - 释放患者
- `GET /api/doctor/requests` - 查看申请历史
- `GET /api/doctor/consultations` - 获取咨询会话列表
- `POST /api/doctor/consultations/{sessionId}/close` - 关闭咨询会话

**管理员端:**
- `GET /api/admin/patient-assignments/pending` - 获取待审核请求
- `POST /api/admin/patient-assignments/{requestId}/approve` - 审核通过
- `POST /api/admin/patient-assignments/{requestId}/reject` - 审核拒绝

**患者端:**
- `GET /api/patient/my-doctor` - 获取我的医生
- `GET /api/patient/my-consultation` - 获取我的咨询会话
- `POST /api/patient/consultation/start` - 发起咨询

### 业务流程

**患者认领流程:**
1. 医生浏览患者公海 → 提交认领申请（含理由）
2. 系统验证医生患者数量限制（<10）和患者状态
3. 管理员审核通过/拒绝
4. 审核通过后自动创建医患关系

**患者释放流程:**
1. 医生提交释放申请（含理由）
2. 管理员审核通过/拒绝
3. 审核通过后将关系状态设为 inactive
4. 患者回归患者公海

---

## 架构与目录

| 路径 | 说明 |
|------|------|
| `src/main/java/com/mental/health/controller` | REST 控制器，按角色/功能划分（auth、patient、doctor、admin、AI、房间/主题/树洞/时间胶囊等）。 |
| `src/main/java/com/mental/health/service` | 业务实现，覆盖 AI、预约、房间装饰、医生、消息、系统配置等复杂逻辑。 |
| `src/main/java/com/mental/health/mapper` | MyBatis-Plus Mapper，直接映射数据库表，部分包含自定义 SQL。 |
| `src/main/java/com/mental/health/entity` | 24 个实体类，定义所有数据模型。 |
| `src/main/java/com/mental/health/security` | JWT 过滤器、异常入口，结合 `SecurityConfig` 进行鉴权配置。 |
| `src/main/java/com/mental/health/algorithm` | `CollaborativeFiltering` 推荐算法实现。 |
| `src/main/java/com/mental/health/scheduler` | `TreeHoleCleanupTask` 等定时任务。 |
| `src/main/resources` | `application.yml`、Mapper XML（如有）、静态资源。 |
| `frontend/src` | `main.ts`、`App.vue`、`layouts/`、`views/`、`components/`、`api/index.ts`、`stores/`、`utils/`、`router/index.ts`。 |
| `frontend/public` | 静态资源与声音文件（rain.mp3、wind.mp3、card-flip.mp3 等）。 |
| `sql/` | 存放阶段性脚本（如 `phase3-room-decoration.sql`、`tree_hole.sql`、`update_time_capsule_conditions.sql`）。 |

### 实体类列表 (24个)

1. `User` - 用户账户（患者/医生/管理员）
2. `AIConversation` - AI 对话历史
3. `Announcement` - 系统公告
4. `Appointment` - 预约信息
5. `AssessmentReport` - 评估报告
6. `ChatMessage` - 聊天消息
7. `ConsultationSession` - 咨询会话
8. `DecorationConfig` - 装饰配置
9. `LetterRecommendation` - 信件推荐
10. `MentalResource` - 心理资源
11. `MoodComment` - 心情评论
12. `MoodDiary` - 情绪日记
13. `PatientDoctorRelationship` - 医患关系
14. `RelationshipChangeRequest` - 关系变更请求
15. `RoomDecoration` - 房间装饰
16. `SymptomRecord` - 症状记录
17. `SystemAlert` - 系统警报
18. `SystemNotification` - 系统通知
19. `SystemSettings` - 系统设置
20. `TimeCapsule` - 时间胶囊
21. `TreeHole` - 树洞帖子
22. `UserFeedback` - 用户反馈
23. `UserThemeConfig` - 用户主题配置
24. `WeatherConfig` - 天气配置

---

## 运行与调试

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 16+
- npm 8+
- MySQL 8.0
- Redis（可选）

### 1. 初始化数据库

```bash
mysql -uroot -p
CREATE DATABASE mental_health CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
source E:/ddd/智能心里健康管理系统/database.sql;
source E:/ddd/智能心里健康管理系统/patient_doctor_relationship_tables.sql;
source E:/ddd/智能心里健康管理系统/create-test-accounts.sql;
```

### 2. 启动后端

```bash
mvn clean verify         # 必跑，编译 + 测试
mvn spring-boot:run      # 调试
# 或使用 start-backend.bat 运行 target/ 下的可执行 Jar
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev      # 开发
npm run build    # 产物 + vue-tsc 类型检查
npm run preview  # 验证构建结果
```

### 4. Redis & 脚本

`start-redis-windows.bat` 或自行运行 redis-server；`powershell -File test-all-apis.ps1` 可快速冒烟 REST 接口。

---

## 配置说明

`src/main/resources/application.yml` 当前包含：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mental_health
    username: root
    password: root123456
  servlet:
    multipart:
      max-file-size: 10MB

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted

jwt:
  secret: mental-health-system-secret-key-2024
  expiration: 86400  # 24小时

ai:
  mock:
    enabled: false
  api:
    key: sk-55100f2bb6cf47e3bc32ad092c2d6579
    url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
  model: qwen-plus

file:
  upload-path: ./uploads/

logging:
  level:
    com.mental.health: debug
```

> **安全提示**: 正式环境务必通过环境变量或 `application-local.yml` 覆盖密钥和数据库密码。

---

## 前端说明

### 页面统计

**患者端 (14个页面):**
- Dashboard (工作台)
- MoodDiary (情绪日记)
- TimeCapsule (时间胶囊)
- TreeHole (心情树洞)
- RoomDecoration (房间装饰)
- Resources (心理资源)
- AIChat (AI咨询)
- Reports (评估报告)
- Doctors (医生列表)
- Chat (实时沟通)
- Communication (通讯)
- Profile (个人中心)
- Space (个人空间)
- SymptomRecord (症状记录)

**医生端 (8个页面):**
- Dashboard (工作台)
- Patients (患者管理)
- PatientPool (患者公海)
- Reports (评估报告)
- Chat (患者沟通)
- Appointments (预约管理)
- Consultations (咨询管理)
- Profile (个人中心)

**管理员端 (8个页面):**
- Dashboard (工作台)
- Users (用户管理)
- Doctors (医生管理 + 审批)
- Resources (资源管理)
- Appointments (预约管理)
- Messages (消息中心)
- Settings (系统设置)
- Statistics (数据统计)

### 组件库 (25个)

所有组件位于 `components/MoodDiary/`：

- CloudInteraction - 云端交互效果
- CommentTimeline - 评论时间线
- DecorationItem - 装饰物品
- DecorationList - 装饰列表
- DiaryCard - 日记卡片
- FireworkEffect - 烟花特效
- FloatingFeedback - 浮动反馈
- InteractionToolbar - 交互工具栏
- LetterRecommendationCard - 信件推荐卡片
- LightSwitch - 灯光开关
- MeteorShower - 流星雨特效
- MoodCommentDialog - 评论对话框
- MoodForecast - 心情预测
- ParticleEffect - 粒子效果
- RoomCanvas - 3D房间画布
- RoomDecorationShop - 装饰商店
- ThemeSelector - 主题选择器
- TimeCapsuleEditor - 时间胶囊编辑器
- TimeCapsuleList - 时间胶囊列表
- TimeCapsuleReplyDialog - 回信对话框
- TreeHoleCard - 树洞卡片
- TreeHoleDetail - 树洞详情
- TreeHoleEditor - 树洞编辑器
- TreeHoleList - 树洞列表
- WeatherBackground - 天气背景

### 工具模块

- `utils/request.ts` - Axios 拦截器，自动携带 JWT Token
- `utils/soundService.ts` - 环境音效管理
- `utils/hapticService.ts` - 触觉反馈
- `utils/performance.ts` - 性能优化工具（防抖/节流/懒加载）

---

## 服务模块与接口

| 模块 | 路径/类 | 说明 |
|------|---------|------|
| 认证 | `/api/auth` | 注册、登录、登出 |
| 用户中心 | `/api/user/**` | 个人信息、密码修改、医生列表 |
| 情绪日记 | `/api/patient/mood-diary/**` | 写日记、分页、统计、天气匹配 |
| 日记评论 | `/api/patient/mood-comment/**` | 添加评论、互动统计 |
| 症状记录 | `/api/patient/symptom/**` | CRUD + 历史记录 |
| 树洞 | `/api/patient/tree-hole/**` | 写入、查询、条件解锁 |
| 时间胶囊 | `/api/patient/time-capsule/**` | 写信、解锁、回复、推荐 |
| 房间装饰 | `/api/patient/room/**` | 装饰管理、解锁条件检查 |
| 主题 | `/api/patient/theme/**` | 主题切换、解锁、昼夜模式 |
| 资源推荐 | `/api/patient/recommendations/**` | 协同过滤推荐、行为记录 |
| AI 服务 | `/api/patient/ai/**` | AI 问答、评估报告生成 |
| 医生工作台 | `/api/doctor/**` | 患者管理、报告、预约、公海 |
| 医患关系 | `/api/doctor/patient-pool/**` | 认领/释放患者 |
| 咨询管理 | `/api/doctor/consultations/**` | 会话管理 |
| 管理员审批 | `/api/admin/patient-assignments/**` | 审核通过/拒绝 |
| 预约管理 | `/api/admin/appointments/**` | CRUD、统计分析 |
| 消息中心 | `/api/admin/messages/**` | 通知、公告、反馈 |
| 系统设置 | `/api/admin/settings/**` | 配置项管理 |
| WebSocket | `/ws` | 实时聊天、在线状态 |

---

## AI 与算法能力

- **AIService**：
  - `askQuestion`：调用 DashScope（或 mock）模型，拼接最近 5 篇日记上下文
  - `generateAssessmentReport`：分析最近 30 篇日记，按七个维度打分生成诊断+建议
  - `submitFeedback`：追踪 AI 答案质量

- **协同过滤**：`CollaborativeFiltering` 通过用户-资源评分矩阵计算余弦相似度 → K近邻 → 评分预测 → Top-N 推送

- **时间胶囊分析**：记录写信时的平均心情、趋势，根据最近 7 天心情选择 hope/praise/goal/thanks 模板

- **主题解锁**：基于日记数量、连击、信件数量、低心情次数、夜间模式使用、特定月份等条件解锁主题

- **房间装饰**：根据 JSON 条件（diary_count、letter_count、mood_better_count 等）自动发放装饰

---

## 实时通信与调度

- **WebSocket**：`WebSocketConfig` 配置 `/ws` 端点（允许跨域 + SockJS fallback），`/app` 为客户端发送前缀，`/topic`/`/queue`/`/user` 为订阅前缀

- **定时任务**：`TreeHoleCleanupTask` 每 5 分钟执行一次，清理过期树洞内容

---

## 测试与质量保障

- **编译与测试**：`mvn clean verify` 执行单元测试（含 `PasswordTest` 示范 Bcrypt 校验）

- **前端校验**：`npm run build` 自动运行 `vue-tsc` 类型检查与 Vite 构建

- **脚本冒烟**：`powershell -File test-all-apis.ps1` 按顺序请求 60+ API 并记录耗时/结果

- **日志与监控**：默认 MyBatis-Plus 输出 SQL 到控制台

---

## 已知问题与待完善

### 已实现功能 (✅)

- [x] 用户认证与授权（JWT + 角色权限）
- [x] 情绪日记系统（CRUD + 天气匹配 + 统计）
- [x] 症状记录与追踪
- [x] AI 智能问答（阿里 DashScope 集成）
- [x] AI 综合评估报告生成
- [x] 时间胶囊（写信/解锁/回信/推荐）
- [x] 心情树洞（匿名发布/定期清理）
- [x] 房间装饰系统（解锁/放置/主题）
- [x] 主题成长体系（多主题解锁）
- [x] 心理资源推荐（协同过滤算法）
- [x] 医患关系管理（认领/释放/审批）
- [x] 患者公海机制
- [x] 在线咨询会话管理
- [x] 患者咨询历史记录
- [x] 患者发起咨询功能
- [x] 管理员审批工作流
- [x] WebSocket 实时通信
- [x] 预约管理系统
- [x] 系统通知与公告
- [x] 多角色布局（患者/医生/管理员）

### 待完善功能 (⏳)

- [ ] **视频/语音咨询**：当前仅支持文字聊天
- [ ] **医生排班系统**：暂无排班功能
- [ ] **支付系统**：咨询付费功能未实现
- [ ] **消息推送**：缺少实时消息推送（如邮件/短信通知）
- [ ] **数据导出**：报告和统计数据的 PDF/Excel 导出
- [ ] **移动端适配**：响应式设计有待优化
- [ ] **国际化**：仅支持中文
- [ ] **Docker 部署**：Dockerfile 待完善
- [ ] **单元测试覆盖**：测试覆盖率较低
- [ ] **性能监控**：APM 工具集成
- [ ] **日志聚合**：ELK Stack 集成
- [ ] **API 文档**：Swagger/OpenAPI 文档自动生成

### 已知问题

1. **Profile 页面导入错误**：`@/store/user` 应为 `@/stores/user`
2. **医生端咨询详情**：`/api/doctor/consultations/{sessionId}` 接口返回 "功能暂未实现"
3. **心理测评表**：数据库表已创建但 UI 未实现
4. **Redis 缓存**：配置预留但未启用

---

## 协作资料

- **工作守则**：`AGENTS.md` 描述仓库结构、命名约定
- **快速入门**：`QUICKSTART.md` 提供环境准备、数据库导入
- **阶段总结**：`PROJECT_COMPLETION.md`、`Day1-Day6 完成总结`
- **功能文档**：`患者医生关系管理系统实现文档.md`、`房间装饰系统实现总结.md`
- **运维脚本**：`start-backend.bat`、`start.bat`、`stop.bat`
- **数据/账号**：`create-test-accounts.sql`、`测试账号说明.md`

---

## 更新日志

### v1.0.0 (2025-11-16)

**新增功能:**
- 医患关系管理系统
  - 患者公海机制
  - 医生认领/释放患者
  - 管理员审批工作流
  - 关系约束（10人上限、唯一分配）
- 在线咨询会话管理
- 医生端侧边栏扩展（患者公海、咨询管理菜单）

**修复问题:**
- DoctorController HashMap 导入缺失
- 前端路由配置不完整

**优化:**
- 项目文档更新
- 代码统计和结构梳理

---

## 许可证

本项目为学习和研究用途。

---

## 联系方式

如有问题或建议，请联系开发团队或提交 Issue。

---

**项目维护者**: Claude Code
**技术支持**: Spring Boot 3 + Vue 3 + MySQL
**文档版本**: v1.0.0
**最后更新**: 2025-11-16
