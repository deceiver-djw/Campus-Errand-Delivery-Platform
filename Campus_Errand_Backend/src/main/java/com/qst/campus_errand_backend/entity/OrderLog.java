package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_log")
public class OrderLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色: 0=用户, 1=跑腿员, 2=管理员 */
    private Integer operatorRole;

    /** 备注说明 */
    private String remark;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
