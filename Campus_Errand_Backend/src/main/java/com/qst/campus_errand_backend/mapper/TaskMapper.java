package com.qst.campus_errand_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qst.campus_errand_backend.entity.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper extends BaseMapper<Task> {

    /** 乐观锁抢单：更新任务状态 */
    @Update("UPDATE task SET status = #{newStatus}, version = version + 1, update_time = NOW() " +
            "WHERE id = #{taskId} AND status = #{oldStatus} AND version = #{version}")
    int updateStatusWithVersion(@Param("taskId") Long taskId,
                                @Param("oldStatus") Integer oldStatus,
                                @Param("newStatus") Integer newStatus,
                                @Param("version") Integer version);
}
