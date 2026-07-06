package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("runner")
public class Runner {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard = "";

    /** 手机号 */
    private String phone;

    /** 学生证/身份证照片URL */
    private String studentCardImg = "";

    /** 审核状态: 0=待审核, 1=已通过, 2=已拒绝 */
    private Integer auditStatus;

    /** 审核备注（拒绝原因） */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 平均评分（1.00~5.00） */
    private BigDecimal score;

    /** 累计接单数 */
    private Integer orderCount;

    /** 累计收益 */
    private BigDecimal totalIncome;

    /** 状态: 0=禁用, 1=启用 */
    private Integer status;

    /** 申请时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0=正常, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
