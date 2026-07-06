# 校园跑腿代取快递平台 🏃‍♂️📦

> Campus Errand — 基于 Spring Boot 3 + Vue 3 的全栈校园跑腿代取快递服务平台

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.38-4FC08D.svg)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 📖 项目简介

校园跑腿代取快递平台是一个面向高校学生的互助服务平台。普通用户可以在平台上发布代取快递的任务并支付一定费用，跑腿员可以接单并完成配送，管理员负责审核跑腿员资质和管理平台运营。

### 核心功能

| 角色 | 功能 |
|------|------|
| **普通用户** | 注册登录、发布跑腿任务、查看任务状态、支付费用、评价跑腿员、站内消息 |
| **跑腿员** | 申请成为跑腿员、抢单大厅接单、订单状态流转（取件→配送→送达）、收入结算、评价用户 |
| **管理员** | 用户管理、跑腿员审核、任务/订单管理、结算管理、数据统计 |

### 订单生命周期

```
用户发布任务 → 待接单(0) → 跑腿员抢单 → 已接单(0)
→ 已取件(1) → 配送中(2) → 已送达(3)
→ 用户确认(4) → 双方互评 → 已完成(5)
```

## 🏗️ 技术架构

```
Campus_Errand_Frontend (Vue 3)    Campus_Errand_Backend (Spring Boot 3)
   localhost:5173                       localhost:8080
        │                                      │
   Vite Dev Proxy ─────── /api ────────▶  REST Controllers
        │                                      │
   Pinia Stores ◀─── Axios ─────────────  Service Layer
        │                                      │
   Vue Router (权限守卫)                    MyBatis-Plus Mappers
        │                                      │
   Element Plus UI                        MySQL 8.0 + Redis
```

### 后端技术栈

- **框架**: Spring Boot 3.5.16
- **语言**: Java 21
- **构建工具**: Maven
- **ORM**: MyBatis-Plus 3.5.9
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **API 文档**: SpringDoc OpenAPI (Swagger UI)
- **工具库**: Lombok

### 前端技术栈

- **框架**: Vue 3 (Composition API)
- **语言**: TypeScript 6.0
- **构建工具**: Vite 8.0
- **UI 组件库**: Element Plus 2.14
- **状态管理**: Pinia 3.0
- **路由**: Vue Router 5.1
- **HTTP 客户端**: Axios

## 🚀 快速开始

### 环境要求

- **JDK** ≥ 21
- **Maven** ≥ 3.8
- **Node.js** ≥ 18
- **MySQL** ≥ 8.0
- **Redis** ≥ 7.0

### 1. 初始化数据库

```bash
# 使用 MySQL 客户端导入数据库脚本
mysql -u root -p < campus_errand.sql
```

数据库包含 11 张业务表及初始数据（1 个管理员账号 + 10 个快递点）。

**默认管理员账号**: `admin001` / `admin123`

### 2. 启动后端服务

```bash
cd Campus_Errand_Backend

# 安装依赖并编译
mvn clean install -DskipTests

# 启动 Spring Boot 应用
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`，Swagger 文档地址 `http://localhost:8080/swagger-ui.html`。

### 3. 启动前端项目

```bash
cd Campus_Errand_Frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:5173`，请求通过 Vite 代理转发至后端。

### 4. 访问应用

打开浏览器访问 `http://localhost:5173` 即可使用。

## 📁 项目结构

```
javaWeb综合实训/
├── Campus_Errand_Backend/           # Spring Boot 后端
│   ├── pom.xml                      # Maven 配置
│   └── src/main/
│       ├── java/com/qst/campus_errand_backend/
│       │   ├── CampusErrandBackendApplication.java  # 启动类
│       │   ├── config/              # 配置类
│       │   ├── controller/          # REST 控制器 (11个)
│       │   ├── common/              # 通用类 (Result 响应封装)
│       │   ├── entity/              # 实体类 (11个)
│       │   ├── mapper/              # MyBatis-Plus Mapper
│       │   ├── service/             # 业务逻辑层
│       │   │   └── impl/            # 服务实现
│       │   └── utils/               # 工具类
│       └── resources/
│           ├── application.yaml     # 应用配置
│           ├── static/              # 静态资源
│           └── templates/           # 模板文件
├── Campus_Errand_Frontend/          # Vue 3 前端
│   ├── package.json                 # Node 依赖配置
│   ├── vite.config.ts               # Vite 构建配置
│   └── src/
│       ├── main.ts                  # 应用入口
│       ├── App.vue                  # 根组件
│       ├── api/                     # API 接口封装
│       ├── assets/                  # 静态资源 & 样式
│       ├── components/              # 公共组件
│       ├── layout/                  # 布局组件
│       ├── router/                  # 路由配置 (含权限守卫)
│       ├── stores/                  # Pinia 状态管理
│       └── views/                   # 页面视图
│           ├── admin/               # 管理员页面
│           ├── user/                # 普通用户页面
│           └── runner/              # 跑腿员页面
└── campus_errand.sql                # 数据库初始化脚本
```

## 📊 数据库表结构

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | student_no, password, role(0/1/2), balance |
| `runner` | 跑腿员表 | user_id, audit_status, score, total_income |
| `task` | 任务表 | express_point, pickup_code, fee, status |
| `order` | 订单表 | task_id, runner_id, status(0-5), version |
| `order_log` | 订单日志 | order_id, from_status, to_status |
| `evaluation` | 评价表 | order_id, score(1-5), type(0/1) |
| `settlement` | 结算表 | order_id, amount, status |
| `message` | 私信表 | from_user_id, to_user_id, content |
| `notification` | 通知表 | user_id, type, title, is_read |
| `transaction_record` | 交易记录 | user_id, type, amount, balance |
| `express_point` | 快递点 | name, location, sort_order |

## ⚙️ 配置说明

后端主要配置位于 `Campus_Errand_Backend/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_errand
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
```

前端代理配置位于 `Campus_Errand_Frontend/vite.config.ts`，默认将 `/api` 请求代理至 `http://192.168.106.1:8080`，请根据实际后端地址修改。

## 📝 API 文档

启动后端后，访问 Swagger UI 查看完整 API 文档：

```
http://localhost:8080/swagger-ui.html
```

所有 API 接口以 `/api/` 为前缀，涵盖用户、跑腿员、任务、订单、评价、结算、消息、通知、交易记录、快递点等模块。

## 🐛 已知问题 & 改进方向

- [ ] 密码存储使用 MD5（安全性较低），建议升级为 BCrypt
- [ ] Token 使用 UUID 而非 JWT，无过期机制
- [ ] 前端存在 Vite 脚手架残留组件（HelloWorld 等）未清理
- [ ] 图片上传路径硬编码为 `C:/images/`，建议改为可配置
- [ ] 前端 dev proxy 指向固定 IP，建议改为 `localhost` 或环境变量

## 📄 许可证

本项目仅供学习交流使用。
