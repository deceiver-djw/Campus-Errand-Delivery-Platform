package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 评价人ID */
    private Long fromUserId;

    /** 被评人ID */
    private Long toUserId;

    /** 评分（1~5星） */
    private Integer score;

    /** 文字评价内容 */
    private String content;

    /** 评价标签（逗号分隔） */
    private String tags;

    /** 评价类型: 0=用户评跑腿员, 1=跑腿员评用户 */
    private Integer type;

    /** 评价时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
