package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement")
public class Settlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 跑腿员ID */
    private Long runnerId;

    /** 支付用户ID（冗余） */
    private Long userId;

    /** 结算金额（跑腿费） */
    private BigDecimal amount;

    /** 结算状态: 0=待结算, 1=已结算, 2=已退款 */
    private Integer status;

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
