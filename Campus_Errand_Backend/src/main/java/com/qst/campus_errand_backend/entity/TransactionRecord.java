package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction_record")
public class TransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 交易类型: 0=充值, 1=支付跑腿费, 2=收入跑腿费, 3=提现, 4=退款 */
    private Integer type;

    /** 交易金额 */
    private BigDecimal amount;

    /** 交易后余额 */
    private BigDecimal balance;

    /** 关联业务ID */
    private Long relatedId;

    /** 备注说明 */
    private String remark;

    /** 交易时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
