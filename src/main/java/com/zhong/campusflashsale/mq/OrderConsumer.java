package com.zhong.campusflashsale.mq;

import com.zhong.campusflashsale.entity.SeckillOrder;
import com.zhong.campusflashsale.mapper.SeckillOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill-order-topic", consumerGroup = "seckill-consumer-group")
public class OrderConsumer implements RocketMQListener<String> {

    @Autowired
    private SeckillOrderMapper orderMapper;

    @Override
    public void onMessage(String payload) {
        log.info("收到秒杀订单消息: {}", payload);
        // 简单解析 JSON (实际可用 Fastjson/Gson 等)
        // payload 格式：{"userId":1,"productId":100}
        Long userId = Long.parseLong(payload.split(",")[0].split(":")[1]);
        Long productId = Long.parseLong(payload.split(",")[1].split(":")[1].replace("}", ""));

        // 插入订单表（依靠 uk_user_activity 唯一索引防重）
        try {
            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
            order.setUserId(userId);
            order.setProductId(productId);
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insert(order);
            log.info("订单落库成功: userId={}, productId={}", userId, productId);
        } catch (Exception e) {
            // 如果是唯一索引冲突（DuplicateKeyException），说明是重复消费，直接忽略
            log.warn("订单落库失败，可能是重复消费: {}", e.getMessage());
        }
    }
}