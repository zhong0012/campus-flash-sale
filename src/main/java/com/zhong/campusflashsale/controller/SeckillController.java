package com.zhong.campusflashsale.controller;

import com.zhong.campusflashsale.entity.LocalMessage;
import com.zhong.campusflashsale.mapper.LocalMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LocalMessageMapper localMessageMapper;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @GetMapping("/do")
    public String seckill(@RequestParam Long userId, @RequestParam Long productId) {
        String stockKey = "seckill:stock:" + productId;
        String userKey = "seckill:users:" + productId;

        // 【测试用】没有库存则塞10个进去
        if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
            redisTemplate.opsForValue().set(stockKey, 10);
        }

        // 1. 执行 Lua 脚本原子扣减
        List<String> keys = Arrays.asList(stockKey, userKey);
        Long result = redisTemplate.execute(SECKILL_SCRIPT, keys, userId.toString());

        if (result == 0) {
            return "秒杀失败：库存不足";
        } else if (result == -1) {
            return "秒杀失败：请勿重复抢购";
        }

        // 2. Redis 扣减成功，写入本地消息表 (状态为0：待发送)
        // 这里为了简化，采用同步写表，实际上在真实高并发下，写表也会成为瓶颈，可以用异步线程写
        String msgId = UUID.randomUUID().toString();
        String payload = String.format("{\"userId\":%d,\"productId\":%d}", userId, productId);

        LocalMessage msg = new LocalMessage();
        msg.setMsgId(msgId);
        msg.setPayload(payload);
        msg.setStatus(0);
        msg.setRetryCount(0);
        msg.setNextRetryTime(LocalDateTime.now());
        msg.setCreateTime(LocalDateTime.now());

        localMessageMapper.insert(msg);

        return "秒杀成功，排队中！请稍后查询结果。消息ID：" + msgId;
    }
}