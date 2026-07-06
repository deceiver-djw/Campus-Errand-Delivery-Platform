package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Task;
import com.qst.campus_errand_backend.entity.TransactionRecord;
import com.qst.campus_errand_backend.entity.User;
import com.qst.campus_errand_backend.service.TaskService;
import com.qst.campus_errand_backend.service.TransactionRecordService;
import com.qst.campus_errand_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/task")
@Tag(name = "任务管理", description = "任务发布、查询、取消相关接口")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRecordService transactionRecordService;

    @GetMapping("/{id}")
    @Operation(summary = "查询任务详情")
    public Task getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询任务列表")
    public Page<Task> list(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) @Parameter(description = "任务状态") Integer status,
                           @RequestParam(required = false) @Parameter(description = "快递点搜索") String expressPoint,
                           @RequestParam(required = false) @Parameter(description = "发布用户ID") Long userId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Task::getStatus, status);
        if (expressPoint != null) wrapper.like(Task::getExpressPoint, expressPoint);
        if (userId != null) wrapper.eq(Task::getUserId, userId);
        wrapper.orderByDesc(Task::getCreateTime);
        return taskService.page(new Page<>(page, size), wrapper);
    }

    @GetMapping("/available")
    @Operation(summary = "获取待接单任务列表（抢单大厅）")
    public Page<Task> available(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String expressPoint,
                                @RequestParam(required = false) @Parameter(description = "最低跑腿费") Double minFee,
                                @RequestParam(required = false) @Parameter(description = "最高跑腿费") Double maxFee) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, 0);
        if (expressPoint != null) wrapper.like(Task::getExpressPoint, expressPoint);
        if (minFee != null) wrapper.ge(Task::getFee, minFee);
        if (maxFee != null) wrapper.le(Task::getFee, maxFee);
        wrapper.orderByDesc(Task::getIsUrgent).orderByDesc(Task::getCreateTime);
        return taskService.page(new Page<>(page, size), wrapper);
    }

    @PostMapping
    @Operation(summary = "发布任务（自动扣除余额）")
    public boolean publish(@RequestBody Task task) {
        User user = userService.getById(task.getUserId());
        if (user == null) return false;

        BigDecimal fee = task.getFee() != null ? task.getFee() : BigDecimal.ZERO;
        BigDecimal balance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

        if (balance.compareTo(fee) < 0) return false;

        // 扣除余额
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, task.getUserId())
                .setSql("balance = balance - " + fee));

        boolean saved = taskService.save(task);

        // 记录交易流水
        if (saved) {
            TransactionRecord record = new TransactionRecord();
            record.setUserId(task.getUserId());
            record.setType(1); // 支付跑腿费
            record.setAmount(fee.negate());
            record.setBalance(balance.subtract(fee));
            record.setRelatedId(task.getId());
            record.setRemark("发布任务支付跑腿费");
            transactionRecordService.save(record);
        }
        return saved;
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新任务信息")
    public boolean update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        return taskService.updateById(task);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消任务（退款）")
    public boolean cancel(@PathVariable Long id) {
        Task task = taskService.getById(id);
        if (task == null || task.getStatus() != 0) return false;

        Task update = new Task();
        update.setId(id);
        update.setStatus(2);
        update.setCancelTime(LocalDateTime.now());
        boolean ok = taskService.updateById(update);

        // 退款
        if (ok && task.getFee() != null && task.getFee().compareTo(BigDecimal.ZERO) > 0) {
            userService.update(new LambdaUpdateWrapper<User>()
                    .eq(User::getId, task.getUserId())
                    .setSql("balance = balance + " + task.getFee()));

            User user = userService.getById(task.getUserId());
            TransactionRecord record = new TransactionRecord();
            record.setUserId(task.getUserId());
            record.setType(4); // 退款
            record.setAmount(task.getFee());
            record.setBalance(user.getBalance());
            record.setRelatedId(id);
            record.setRemark("取消任务退款");
            transactionRecordService.save(record);
        }
        return ok;
    }
}
