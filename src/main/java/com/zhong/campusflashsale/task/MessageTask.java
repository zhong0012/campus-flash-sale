package com.zhong.campusflashsale.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhong.campusflashsale.entity.LocalMessage;
import com.zhong.campusflashsale.mapper.LocalMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class MessageTask {

    @Autowired
    private LocalMessageMapper messageMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 每 3 秒扫描一次待发送的消息
    @Scheduled(fixedDelay = 3000)
    public void scanAndSend() {
        // 查询状态为 0（待发送）且下次重试时间已到的消息
        QueryWrapper<LocalMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
                .le("next_retry_time", LocalDateTime.now())
                .last("LIMIT 100");

        List<LocalMessage> messages = messageMapper.selectList(wrapper);

        for (LocalMessage msg : messages) {
            try {
                // 发送到 RocketMQ
                rocketMQTemplate.syncSend("seckill-order-topic", msg.getPayload());

                // 发送成功，更新状态为 1（已发送）
                msg.setStatus(1);
                messageMapper.updateById(msg);
                log.info("本地消息发送成功: msgId={}", msg.getMsgId());
            } catch (Exception e) {
                // 发送失败，重试次数 +1，下次重试时间延后 5 秒
                log.error("消息发送失败，等待重试: msgId={}", msg.getMsgId());
                msg.setRetryCount(msg.getRetryCount() + 1);
                msg.setNextRetryTime(LocalDateTime.now().plusSeconds(5));
                // 如果重试超过 5 次，可以标记为 2（失败），这里简化处理只做延后
                messageMapper.updateById(msg);
            }
        }
    }
}