# 校园闪购/秒杀模块（Campus Flash Sale）

## 📌 项目简介
基于 Spring Boot 4 + MySQL + Redis + RocketMQ 实现的高并发秒杀核心模块。核心解决高并发场景下的库存超卖、接口幂等、以及分布式事务一致性问题。不追求大而全，只专注秒杀核心链路的极致与严谨。

## 🚀 核心技术点
1. **防超卖与高并发**：利用 Redis + Lua 脚本实现原子性库存扣减和用户去重，将大部分无效流量拦截在数据库之前。
2. **异步削峰与最终一致性**：引入 RocketMQ 异步落单。通过“本地消息表 + 定时任务补偿”机制，彻底解决 Redis 扣减成功但 DB 落单失败的数据丢失问题。
3. **接口幂等与防重**：Redis 用户集合判断 + 数据库唯一索引 (`uk_user_activity`) 双重兜底，杜绝网络重试导致的重复下单。
4. **一键环境隔离**：使用 Docker Compose 一键拉起 MySQL、Redis、RocketMQ 全套环境，实现开箱即用。

## 🛠️ 技术栈
- **后端框架**：Spring Boot 4.1.2, MyBatis-Plus
- **数据存储**：MySQL 8.0, Redis 7.0
- **消息中间件**：RocketMQ 4.9.6
- **开发环境**：JDK 25, Docker, Docker Compose

## 💻 快速启动
1. 确保本地已安装 Docker 并启动。
2. 克隆代码到本地，在根目录执行：`docker compose up -d`
3. 启动 Spring Boot 项目 `CampusFlashSaleApplication`。
4. 发起秒杀测试请求：
   `GET http://localhost:8080/seckill/do?userId=1&productId=100`
5. 观察控制台日志，验证“Redis 扣减 -> 本地消息 -> MQ 消费 -> DB 落单”的完整异步链路。

## 📝 核心链路流程图
*(可以后续自己在本地用 draw.io 画一张加进来，绝对是加分项)*
