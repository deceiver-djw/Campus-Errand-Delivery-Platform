package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户ID */
    private Long userId;

    /** 快递点名称 */
    private String expressPoint;

    /** 取件码 */
    private String pickupCode;

    /** 取件码截图URL */
    private String pickupImg;

    /** 送达地址 */
    private String deliveryAddr;

    /** 跑腿费 */
    private BigDecimal fee;

    /** 是否加急: 0=普通, 1=加急 */
    private Integer isUrgent;

    /** 备注说明 */
    private String remark;

    /** 任务状态: 0=待接单, 1=已接单, 2=已取消 */
    private Integer status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 发布时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0=正常, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
