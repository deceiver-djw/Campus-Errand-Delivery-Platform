package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联任务ID */
    private Long taskId;

    /** 跑腿员ID */
    private Long runnerId;

    /** 发布用户ID（冗余） */
    private Long userId;

    /** 订单状态: 0=已接单, 1=已取件, 2=配送中, 3=已送达, 4=待评价, 5=已完成 */
    private Integer status;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 取件时间 */
    private LocalDateTime pickupTime;

    /** 配送开始时间 */
    private LocalDateTime deliveryTime;

    /** 送达时间 */
    private LocalDateTime arriveTime;

    /** 完成时间（双方互评后） */
    private LocalDateTime completeTime;

    /** 用户是否确认收货: 0=未确认, 1=已确认 */
    private Integer userConfirm;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0=正常, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
