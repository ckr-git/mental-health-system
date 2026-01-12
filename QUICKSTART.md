# 智能心理健康管理系统 - 快速启动指南

## 📋 目录
- [环境要求](#环境要求)
- [后端启动](#后端启动)
- [前端启动](#前端启动)
- [默认测试账号](#默认测试账号)
- [常见问题](#常见问题)

---

## 🔧 环境要求

### 必需软件
| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.8+ | Java项目构建工具 |
| MySQL | 8.0+ | 关系型数据库 |
| Node.js | 16+ | JavaScript运行环境 |
| npm/pnpm | 最新版 | 包管理工具 |

### 可选软件
| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Redis | 6.0+ | 内存数据库(缓存，当前未启用) |

### 推荐IDE
- **后端**: IntelliJ IDEA (推荐) / Eclipse
- **前端**: VS Code (推荐) / WebStorm

---

## 🚀 后端启动

### 1. 安装MySQL和Redis

#### MySQL安装 (Windows)
```bash
# 下载MySQL 8.0安装包
# 官网: https://dev.mysql.com/downloads/mysql/

# 安装后设置root密码
```

#### Redis安装 (Windows)
```bash
# 方式1: 使用WSL2安装
wsl --install
# 在WSL2中安装Redis

# 方式2: 下载Windows版本
# GitHub: https://github.com/tporadowski/redis/releases
```

### 2. 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE mental_health CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出
exit;
```

### 3. 导入数据库表结构

```bash
# 导入SQL文件
mysql -u root -p mental_health < database.sql

# 或在MySQL客户端中执行
use mental_health;
source E:/ddd/智能心里健康管理系统/database.sql;
```

### 4. 修改配置文件

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mental_health?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root          # 修改为你的MySQL用户名
    password: your_password # 修改为你的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver

jwt:
  secret: your-secret-key-change-this-in-production  # 建议修改
  expiration: 86400  # 24小时
```

> **注意**: Redis为可选配置，当前项目未启用Redis缓存。

### 5. 启动SpringBoot应用

#### 方式1: 使用IDE
1. 用IntelliJ IDEA打开项目根目录
2. 等待Maven依赖下载完成
3. 运行主类 `MentalHealthApplication`

#### 方式2: 使用Maven命令
```bash
# 在项目根目录执行
# 首次运行需要安装依赖
mvn clean install

# 启动项目
mvn spring-boot:run
```

### 6. 验证后端启动成功

- 后端服务地址: `http://localhost:8080`
- 健康检查: 访问任意API返回401表示服务正常

看到如下日志表示启动成功:
```
Started MentalHealthApplication in 15.234 seconds
```

---

## 🎨 前端启动

### 1. 安装Node.js

下载地址: https://nodejs.org/

验证安装:
```bash
node -v
npm -v
```

### 2. 安装前端依赖

```bash
cd frontend

# 使用npm
npm install

# 或使用pnpm (更快)
npm install -g pnpm
pnpm install
```

如果安装慢，可以使用淘宝镜像:
```bash
npm config set registry https://registry.npmmirror.com
```

### 3. 启动开发服务器

```bash
# 使用npm
npm run dev

# 或使用pnpm
pnpm dev
```

### 4. 访问前端应用

- 前端地址: `http://localhost:5173`
- 自动在浏览器打开

看到登录页面表示启动成功!

---

## 👤 默认测试账号

系统已预置以下测试账号:

### 患者账号
- **用户名**: patient002
- **密码**: 123456
- **角色**: PATIENT
- **功能**:
  - 情绪日记记录
  - **心灵小屋（特色功能）** ⭐
    - 个性化房间装饰
    - 主题切换（8种主题）
    - 时光信箱
    - 隐藏彩蛋
  - AI聊天助手
  - 心理资源浏览
  - 在线咨询医生

### 医生账号
- **用户名**: doctor001
- **密码**: 123456
- **角色**: DOCTOR
- **功能**: 患者管理、评估报告、在线咨询、资源发布

### 管理员账号
- **用户名**: admin
- **密码**: 123456
- **角色**: ADMIN
- **功能**: 用户管理、系统统计、数据分析、权限配置

---

## ❓ 常见问题

### Q1: Maven依赖下载失败怎么办?

**解决方案**: 配置阿里云Maven镜像

编辑 `~/.m2/settings.xml`:
```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### Q2: MySQL连接失败?

**检查清单**:
1. MySQL服务是否启动
2. 用户名密码是否正确
3. 数据库 `mental_health` 是否已创建
4. 端口3306是否被占用

```bash
# Windows查看端口
netstat -ano | findstr 3306

# 启动MySQL服务
net start mysql80
```

### Q3: 前端npm install太慢?

**解决方案**:
```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 或使用cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install

# 或使用pnpm (推荐)
npm install -g pnpm
pnpm install
```

### Q4: 前端跨域问题?

**解决方案**: 已在 `vite.config.ts` 中配置代理，无需额外处理

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### Q5: WebSocket连接失败?

**原因**: 
1. 后端未启动
2. JWT Token未正确携带
3. WebSocket端口被防火墙拦截

**解决方案**:
```javascript
// 确保Token正确传递
const token = localStorage.getItem('token')
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`)
```

### Q6: AI接口调用失败?

**原因**: 未配置AI API密钥

**解决方案**: 在 `application.yml` 中配置:
```yaml
ai:
  api-key: your-api-key-here
  api-url: https://api.openai.com/v1/chat/completions
```

### Q7: 页面样式错乱?

**解决方案**:
```bash
# 清除缓存重新安装
cd frontend
rm -rf node_modules
rm package-lock.json
npm install
```

### Q8: 后端启动报端口占用?

**解决方案**:
```bash
# Windows查看占用8080端口的进程
netstat -ano | findstr 8080

# 杀死进程 (PID为查到的进程号)
taskkill /F /PID <PID>

# 或修改application.yml中的端口
server:
  port: 8081
```

### Q9: 数据库表不存在?

**解决方案**:
```bash
# 重新导入数据库脚本
mysql -u root -p mental_health < database.sql

# 或检查MyBatis-Plus是否配置表前缀
mybatis-plus:
  global-config:
    db-config:
      table-prefix: ""
```

---

## 🔍 调试技巧

### 后端调试
1. **查看日志**: 关注控制台输出
2. **Debug模式**: IDE中打断点调试
3. **Swagger测试**: 访问 `http://localhost:8080/doc.html` 测试API
4. **查看SQL**: 开启MyBatis SQL日志
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 前端调试
1. **浏览器开发者工具**: F12打开
2. **Vue DevTools**: Chrome插件
3. **网络请求**: 查看Network标签
4. **Console日志**: 查看控制台错误

---

## 📦 生产环境部署

### 后端打包
```bash
cd backend
mvn clean package -DskipTests

# 生成的jar包位于 target/mental-health-*.jar
java -jar target/mental-health-*.jar
```

### 前端打包
```bash
cd frontend
npm run build

# 生成的静态文件在 dist/ 目录
# 部署到Nginx或其他Web服务器
```

### Docker部署 (推荐)
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d
```

---

## 📞 获取帮助

- **GitHub Issues**: 提交问题
- **文档**: 查看 `README.md`
- **API文档**: `http://localhost:8080/doc.html`

---

## ✅ 启动检查清单

- [ ] MySQL 8.0已安装并启动
- [ ] Redis已安装并启动
- [ ] 数据库 `mental_health` 已创建
- [ ] 数据库表结构已导入
- [ ] `application.yml` 配置已修改
- [ ] Maven依赖已下载完成
- [ ] 后端服务启动成功 (8080端口)
- [ ] Node.js已安装
- [ ] 前端依赖已安装
- [ ] 前端服务启动成功 (5173端口)
- [ ] 可以访问登录页面
- [ ] 可以使用测试账号登录

---

**祝您使用愉快! 🎉**
