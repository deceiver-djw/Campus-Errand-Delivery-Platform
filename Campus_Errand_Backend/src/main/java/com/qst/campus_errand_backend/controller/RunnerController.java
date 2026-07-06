package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Runner;
import com.qst.campus_errand_backend.entity.User;
import com.qst.campus_errand_backend.service.RunnerService;
import com.qst.campus_errand_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/runner")
@Tag(name = "跑腿员管理", description = "跑腿员申请、审核、信息管理相关接口")
public class RunnerController {

    @Autowired
    private RunnerService runnerService;

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询跑腿员")
    public Runner getById(@PathVariable Long id) {
        return runnerService.getById(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询跑腿员档案")
    public Runner getByUserId(@PathVariable Long userId) {
        return runnerService.getOne(
                new LambdaQueryWrapper<Runner>().eq(Runner::getUserId, userId));
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询跑腿员列表")
    public Page<Runner> list(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) @Parameter(description = "审核状态: 0=待审核,1=已通过,2=已拒绝") Integer auditStatus) {
        LambdaQueryWrapper<Runner> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Runner::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Runner::getCreateTime);
        return runnerService.page(new Page<>(page, size), wrapper);
    }

    @PostMapping("/apply")
    @Operation(summary = "申请成为跑腿员")
    public Map<String, Object> apply(@RequestBody Runner runner) {
        Map<String, Object> result = new HashMap<>();
        Long userId = runner.getUserId();
        if (userId == null) {
            result.put("code", 400);
            result.put("message", "用户ID不能为空");
            return result;
        }
        // 检查用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在");
            return result;
        }
        // 检查用户是否已经是跑腿员
        if (user.getRole() != null && user.getRole() == 1) {
            result.put("code", 409);
            result.put("message", "您已经是跑腿员，无需重复申请");
            return result;
        }
        // 检查是否已有待审核或已通过的申请
        Runner exist = runnerService.getOne(
                new LambdaQueryWrapper<Runner>()
                        .eq(Runner::getUserId, userId)
                        .and(w -> w.eq(Runner::getAuditStatus, 0).or().eq(Runner::getAuditStatus, 1))
        );
        if (exist != null) {
            if (exist.getAuditStatus() == 0) {
                result.put("code", 409);
                result.put("message", "您已有待审核的申请，请耐心等待管理员审核");
            } else {
                result.put("code", 409);
                result.put("message", "您的申请已通过审核，无需重复申请");
            }
            return result;
        }
        // 检查是否有被拒绝的旧申请，如有则更新而非新增
        Runner rejected = runnerService.getOne(
                new LambdaQueryWrapper<Runner>()
                        .eq(Runner::getUserId, userId)
                        .eq(Runner::getAuditStatus, 2)
        );
        if (rejected != null) {
            rejected.setRealName(runner.getRealName());
            rejected.setPhone(runner.getPhone());
            rejected.setAuditStatus(0);
            rejected.setAuditRemark(null);
            rejected.setAuditTime(null);
            runnerService.updateById(rejected);
        } else {
            runner.setAuditStatus(0);
            runner.setStatus(1);
            runnerService.save(runner);
        }
        result.put("code", 200);
        result.put("message", "申请已提交，请等待管理员审核");
        return result;
    }

    @PutMapping("/{id}/audit")
    @Operation(summary = "审核跑腿员申请")
    public boolean audit(@PathVariable Long id,
                         @RequestParam @Parameter(description = "1=通过, 2=拒绝") Integer auditStatus,
                         @RequestParam(required = false) @Parameter(description = "拒绝原因") String remark) {
        Runner runner = runnerService.getById(id);
        if (runner == null) return false;
        // 更新审核状态
        Runner update = new Runner();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(remark);
        update.setAuditTime(LocalDateTime.now());
        runnerService.updateById(update);
        // 同步更新用户角色：通过 -> 跑腿员(1)，拒绝 -> 保持普通用户(0)
        if (auditStatus == 1) {
            User user = new User();
            user.setId(runner.getUserId());
            user.setRole(1);
            userService.updateById(user);
        } else if (auditStatus == 2) {
            User user = new User();
            user.setId(runner.getUserId());
            user.setRole(0);
            userService.updateById(user);
        }
        return true;
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用跑腿员")
    public boolean updateStatus(@PathVariable Long id,
                                @RequestParam @Parameter(description = "0=禁用, 1=启用") Integer status) {
        Runner runner = new Runner();
        runner.setId(id);
        runner.setStatus(status);
        return runnerService.updateById(runner);
    }

    @GetMapping("/ranking")
    @Operation(summary = "跑腿员接单量排行")
    public Object ranking() {
        return runnerService.list(
                new LambdaQueryWrapper<Runner>()
                        .eq(Runner::getAuditStatus, 1)
                        .eq(Runner::getStatus, 1)
                        .orderByDesc(Runner::getOrderCount)
                        .last("LIMIT 20"));
    }
}
