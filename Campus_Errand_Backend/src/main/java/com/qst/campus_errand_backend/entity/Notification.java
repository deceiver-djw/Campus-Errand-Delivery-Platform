package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知类型: TASK_ASSIGNED/STATUS_CHANGED/SETTLEMENT/ANNOUNCEMENT等 */
    private String type;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 关联业务ID */
    private Long relatedId;

    /** 是否已读: 0=未读, 1=已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 通知时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
