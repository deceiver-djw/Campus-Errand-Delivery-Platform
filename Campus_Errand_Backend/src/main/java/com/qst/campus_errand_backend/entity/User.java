package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学号（唯一登录标识） */
    private String studentNo;

    /** 昵称 */
    private String nickname;

    /** 密码（BCrypt加密） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 真实姓名 */
    private String realName;

    /** 宿舍楼栋（默认送达地址） */
    private String dormitory;

    /** 账户余额 */
    private BigDecimal balance;

    /** 角色: 0=普通用户, 1=跑腿员, 2=管理员 */
    private Integer role;

    /** 状态: 0=禁用, 1=启用 */
    private Integer status;

    /** 是否实名认证: 0=未认证, 1=已认证 */
    private Integer isVerified;

    /** 注册时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0=正常, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
