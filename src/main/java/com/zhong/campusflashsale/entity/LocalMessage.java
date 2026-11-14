package com.zhong.campusflashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("local_message")
public class LocalMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String msgId;
    private String payload;
    private Integer status; // 0待发送 1已发送 2失败
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime createTime;
}