# Repository Guidelines

## 项目结构与模块组织
后端位于 src/main/java/com/mental/health，按照 Spring 分层拆分为 controller、service、mapper、security、scheduler、lgorithm 以及共享的 common，入口 MentalHealthApplication.java 读取 src/main/resources/application.yml 来加载端口、数据源、Redis、JWT 与 AI 配置。测试代码在 src/test/java/com/mental/health 中使用相同包结构。前端工程在 rontend/：接口封装在 src/api，布局与基础组件放在 src/layouts、src/components，三端页面位于 src/views/patient|doctor|admin，静态资源放在 rontend/public。仓库根目录保留 SQL 与脚本（database.sql、phase1-database.sql、create-test-accounts.sql、start-backend.bat、start-redis-windows.bat、	est-all-apis.ps1），涉及数据结构或启动流程的改动需同步更新这些文件。

## 构建、测试与开发命令
- mvn clean verify：编译并运行 Spring 测试套件，提交前务必执行。
- mvn spring-boot:run：本地调试后端；若已打包，可用 start-backend.bat 运行 	arget/ 下的可执行 Jar。
- 前端在 rontend/ 中先 
pm install，再依次使用 
pm run dev（开发服务器）、
pm run build（含 ue-tsc 类型检查）、
pm run preview（构建结果验收）。
- 初始化 MySQL/Redis 并导入 SQL 后，执行 powershell -File test-all-apis.ps1 进行脚本化 API 冒烟测试。

## 编码风格与命名规范
Java 代码使用 4 空格缩进与 Lombok，类名保持 *Controller、*Service、*Mapper 等后缀，REST 路径统一为 /api/<角色>/<资源> 且返回 com.mental.health.common.Result。实体字段使用 camelCase，数据库列保持 snake_case，依赖 MyBatis-Plus 完成映射。Vue/TypeScript 组件以 PascalCase 命名并使用 <script setup lang="ts"> + scoped SCSS，公共逻辑集中在 rontend/src/utils，状态放入 Pinia (rontend/src/stores)，HTTP 调用聚合在 rontend/src/api/index.ts。

## 测试规范
新增或修改业务时，在 src/test/java 中以 *Test 结尾创建单元/集成测试，运行 mvn test 或 mvn -Dtest=Name verify 确认通过。使用 create-test-accounts.sql 迅速准备演示数据以覆盖鉴权流程。更新接口后需同步维护 	est-all-apis.ps1，并在诸如 Bug修复说明-2025-11-05.md 的日志中记录手工验证步骤；涉及前端界面时附上截图或录屏，方便复查。

## 提交与 Pull Request 指南
提交信息采用“模块标签 + 精简动词”格式，例如 ix(patient-mood): 持久化天气背景 或 eat(ai-chat): add typing indicator，正文需要说明受影响的 SQL、配置或脚本。PR 描述应列出已执行的验证命令（mvn clean verify、
pm run build、API 冒烟测试等）、突出数据库/配置/脚本更新，并关联 QUICKSTART.md 及阶段总结中的需求或任务编号。

## 安全与配置提示
严禁提交真实密钥或数据库口令，可通过额外的 pplication-local.yml 或环境变量覆盖 src/main/resources/application.yml。确保 MySQL/Redis 地址与凭据在配置文件、启动脚本和测试脚本中保持一致，避免环境错配。切换 AI mock 模式时，请在 README.md 中注明当前策略与调用限制，方便维护 AIController 的同学确认是否允许访问真实接口。
