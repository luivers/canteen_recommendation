# 高校食堂菜品推荐系统

## 项目简介

本系统是一个面向高校的智能食堂菜品推荐与订餐管理平台，采用前后端分离架构。系统为学生提供个性化菜品推荐、在线点餐、评价互动等功能，同时为食堂管理员提供数据看板、菜品管理、订单管理等后台运营能力。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3.13 / Java 17 |
| 持久层 | Spring Data JPA + Hibernate |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (Lettuce) |
| 安全认证 | Spring Security + JWT (jjwt 0.11.5) |
| 前端框架 | Vue 3.3 + Vite 4.4 |
| UI 组件库 | Element Plus 2.3 |
| 状态管理 | Pinia 2.1 |
| 路由 | Vue Router 4.2 |
| 图表可视化 | ECharts 5.4 + vue-echarts + echarts-wordcloud |
| HTTP 客户端 | Axios 1.5 |
| 工具库 | Lodash、Day.js |
| 其他 | Lombok、EasyExcel（Excel 导出）、Jackson |

---

## 项目结构

```
├── frontend/                    # 前端项目 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                 # API 接口层（按模块拆分）
│   │   ├── components/          # 公共组件 & 管理端分析组件
│   │   ├── router/              # 路由配置（含权限守卫）
│   │   ├── stores/              # Pinia 状态管理
│   │   ├── views/               # 页面视图
│   │   │   ├── admin/           # 管理后台页面
│   │   │   └── *.vue            # 用户端页面
│   │   ├── App.vue
│   │   └── main.js
│   └── vite.config.js
│
├── untitled1/                   # 后端项目 (Spring Boot)
│   ├── src/main/java/com/school/canteen/
│   │   ├── config/              # 配置类（Security、JWT、Cache、Jackson）
│   │   ├── controller/          # REST 控制器（25 个）
│   │   ├── entity/              # JPA 实体类（23 个）
│   │   ├── dto/                 # 数据传输对象
│   │   ├── repository/          # Spring Data JPA 仓库
│   │   ├── service/             # 业务逻辑层
│   │   │   ├── impl/            # 服务实现
│   │   │   ├── strategy/        # 推荐策略模式
│   │   │   │   └── impl/        # 四种推荐策略实现
│   │   │   └── recommendation/  # 推荐引擎
│   │   └── CanteenOrderingApplication.java
│   └── src/main/resources/
│       └── application.yml      # 应用配置
│
├── canteen_recommendation.sql   # 数据库建表 & 初始数据脚本
└── canteen_views_fixed.sql      # 数据库统计视图脚本
```

---

## 功能模块

### 用户端

| 模块 | 说明 |
|------|------|
| 注册 / 登录 | 学号注册，JWT 令牌认证 |
| 首页 | 智能推荐、公告通知、促销活动展示 |
| 菜品浏览 | 按食堂/窗口/分类/口味筛选，支持搜索 |
| 购物车 | 添加菜品、修改数量、代金券抵扣 |
| 在线下单 | 创建订单、支付、查看订单状态流转 |
| 订单管理 | 查看历史订单、取消订单、确认取餐 |
| 评价系统 | 对订单菜品评分评价、上传图片 |
| 个人中心 | 口味偏好设置、饮食数据看板 |
| 积分 & 兑换 | 评价获积分、积分兑换代金券/奖品 |
| 饮食数据 | 个人消费统计、营养摄入分析 |

### 管理端（ADMIN / WINDOW_MANAGER）

| 模块 | 说明 |
|------|------|
| 数据看板 | 营收趋势、订单趋势、热销排行、用户活跃时段 |
| 高级分析 | 关联规则挖掘、用户分群（RFM）、异常检测、对比分析 |
| 菜品管理 | 菜品 CRUD、库存管理、分类管理、套餐管理 |
| 订单管理 | 订单查询、状态流转（制作中→已就绪→已完成） |
| 用户管理 | 用户列表、角色分配、状态管理 |
| 评价管理 | 查看评价、食堂回复、负面评价预警 |
| 促销管理 | 折扣、特价、买赠活动配置 |
| 代金券管理 | 代金券发放、兑换订单管理 |
| 公告管理 | 系统公告发布与管理 |
| 窗口管理 | 食堂窗口信息维护 |

### 智能推荐引擎

系统采用策略模式实现多种推荐算法，支持灵活切换与组合：

| 策略 | 说明 |
|------|------|
| CollaborativeFilteringStrategy | 协同过滤：基于相似用户的行为推荐 |
| ContentBasedStrategy | 基于内容：根据用户口味偏好匹配菜品特征 |
| PopularityStrategy | 热门推荐：基于销量和评分的热门菜品 |
| ContextAwareStrategy | 上下文感知：结合时间、天气等环境因素推荐 |

此外还支持：
- 健康目标推荐（减脂、增肌、均衡饮食等）
- 新菜品发现推荐
- 带推荐理由的个性化推荐
- 综合推荐（多策略混合）

---

## 数据模型（核心实体）

| 实体 | 说明 |
|------|------|
| User | 用户（学生/窗口管理员/管理员），含口味偏好、积分 |
| UserProfile | 用户画像扩展信息 |
| Dish | 菜品，含价格、营养信息、口味标签、促销信息、评分 |
| Category | 菜品分类（川菜、粤菜、鲁菜等） |
| Canteen | 食堂 |
| Window | 食堂窗口 |
| Order / OrderItem | 订单及订单明细 |
| CartItem | 购物车项 |
| Review / ReviewItem | 评价及评价明细 |
| Promotion | 促销活动 |
| Combo | 套餐 |
| Reward / RewardExchange | 奖品及兑换记录 |
| PointLog | 积分变动日志 |
| Notification | 通知消息 |
| SystemAnnouncement | 系统公告 |
| DailyDishStatistic | 菜品每日销售统计 |

---

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+（推荐 18+）
- npm 8+

---

## 快速启动

### 1. 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE canteen_recommendation DEFAULT CHARACTER SET utf8mb4;

-- 导入表结构和初始数据
source canteen_recommendation.sql;

-- 导入统计视图
source canteen_views_fixed.sql;
```

### 2. 启动后端

```bash
cd untitled1

# 修改 src/main/resources/application.yml 中的数据库和 Redis 连接信息（如需要）

# 编译并运行
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8089`。

### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:3000`，API 请求通过 Vite 代理转发到后端 8089 端口。

---

## 默认账户

数据库初始数据中包含以下角色的测试账户（具体学号和密码请查看 `canteen_recommendation.sql` 中 `users` 表的插入数据）：

| 角色 | 说明 |
|------|------|
| ADMIN | 系统管理员，拥有全部管理权限 |
| WINDOW_MANAGER | 窗口管理员，可管理所属窗口的菜品和订单 |
| STUDENT | 普通学生用户 |

---

## API 概览

所有接口以 `/api` 为前缀，主要模块：

| 路径前缀 | 说明 |
|----------|------|
| `/api/users` | 用户注册、登录、信息管理 |
| `/api/dishes` | 菜品查询与管理 |
| `/api/orders` | 订单创建、查询、状态管理 |
| `/api/orders/cart` | 购物车操作 |
| `/api/recommendations` | 智能推荐（个性化/策略/健康/发现/综合） |
| `/api/reviews` | 评价管理 |
| `/api/promotions` | 促销活动 |
| `/api/statistics` | 数据统计与分析 |
| `/api/points` | 积分管理 |
| `/api/rewards` | 奖品与兑换 |
| `/api/admin/vouchers` | 代金券管理 |
| `/api/admin/announcements` | 公告管理 |
| `/api/canteens` | 食堂信息 |
| `/api/windows` | 窗口信息 |
| `/api/weather` | 天气信息（用于上下文感知推荐） |

---

## 构建部署

### 前端构建

```bash
cd frontend
npm run build
# 产物输出到 frontend/dist/
```

### 后端打包

```bash
cd untitled1
mvn clean package -DskipTests
# 产物：target/canteen-recommendation-1.0.0.jar
java -jar target/canteen-recommendation-1.0.0.jar
```
