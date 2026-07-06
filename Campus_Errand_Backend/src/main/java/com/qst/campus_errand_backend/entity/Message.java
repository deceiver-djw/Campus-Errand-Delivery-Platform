package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送人ID */
    private Long fromUserId;

    /** 接收人ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 发送时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
