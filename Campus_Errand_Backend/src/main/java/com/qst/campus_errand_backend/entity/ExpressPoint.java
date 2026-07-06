package com.qst.campus_errand_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("express_point")
public class ExpressPoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快递点名称 */
    private String name;

    /** 位置描述 */
    private String location;

    /** 排序权重（越小越靠前） */
    private Integer sortOrder;

    /** 状态: 0=禁用, 1=启用 */
    private Integer status;

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
