package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Notification;
import com.qst.campus_errand_backend.entity.Order;
import com.qst.campus_errand_backend.entity.OrderLog;
import com.qst.campus_errand_backend.entity.Runner;
import com.qst.campus_errand_backend.entity.Task;
import com.qst.campus_errand_backend.service.NotificationService;
import com.qst.campus_errand_backend.service.OrderLogService;
import com.qst.campus_errand_backend.service.OrderService;
import com.qst.campus_errand_backend.service.RunnerService;
import com.qst.campus_errand_backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@Tag(name = "订单管理", description = "订单查询、状态流转相关接口")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RunnerService runnerService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderLogService orderLogService;

    /** 订单状态对应的中文描述 */
    private static final Map<Integer, String> STATUS_MAP = Map.of(
        0, "已接单",
        1, "已取件",
        2, "配送中",
        3, "已送达",
        4, "待评价",
        5, "已完成"
    );

    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询订单列表")
    public Page<Order> list(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) @Parameter(description = "订单状态") Integer status,
                            @RequestParam(required = false) @Parameter(description = "跑腿员ID") Long runnerId,
                            @RequestParam(required = false) @Parameter(description = "发布用户ID") Long userId,
                            @RequestParam(required = false) @Parameter(description = "关联任务ID") Long taskId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (runnerId != null) wrapper.eq(Order::getRunnerId, runnerId);
        if (userId != null) wrapper.eq(Order::getUserId, userId);
        if (taskId != null) wrapper.eq(Order::getTaskId, taskId);
        wrapper.orderByDesc(Order::getCreateTime);
        return orderService.page(new Page<>(page, size), wrapper);
    }

    @GetMapping("/my-accept")
    @Operation(summary = "查询跑腿员我的接单列表")
    public Page<Order> myAccept(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam Long runnerId,
                                @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getRunnerId, runnerId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreateTime);
        return orderService.page(new Page<>(page, size), wrapper);
    }

    @PostMapping("/grab")
    @Operation(summary = "抢单")
    public Map<String, Object> grab(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        Long taskId = Long.valueOf(body.get("taskId").toString());
        // runnerId 参数实际传入的是 userId，需要查 Runner 表获取真正的 runnerId
        Long userId = body.get("runnerId") != null ? Long.valueOf(body.get("runnerId").toString()) : null;

        if (taskId == null || userId == null) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return result;
        }

        // 查找任务
        Task task = taskService.getById(taskId);
        if (task == null) {
            result.put("code", 404);
            result.put("message", "任务不存在");
            return result;
        }
        if (task.getStatus() != 0) {
            result.put("code", 400);
            result.put("message", "该任务已被抢或已取消");
            return result;
        }

        // 安全保护：跑腿员不能抢自己发布的任务
        if (task.getUserId().equals(userId)) {
            result.put("code", 400);
            result.put("message", "不能接自己发布的任务");
            return result;
        }

        // 查找跑腿员档案
        Runner runner = runnerService.getOne(
                new LambdaQueryWrapper<Runner>().eq(Runner::getUserId, userId));
        if (runner == null) {
            result.put("code", 404);
            result.put("message", "跑腿员档案不存在，请先申请成为跑腿员");
            return result;
        }
        if (runner.getAuditStatus() != 1) {
            result.put("code", 400);
            result.put("message", "跑腿员审核未通过，无法接单");
            return result;
        }
        if (runner.getStatus() == 0) {
            result.put("code", 400);
            result.put("message", "跑腿员账号已被禁用");
            return result;
        }

        // 更新任务状态为"已接单"
        Task taskUpdate = new Task();
        taskUpdate.setId(taskId);
        taskUpdate.setStatus(1);
        taskService.updateById(taskUpdate);

        // 创建订单
        Order order = new Order();
        order.setTaskId(taskId);
        order.setRunnerId(runner.getId());
        order.setUserId(task.getUserId());
        order.setStatus(0);
        order.setAcceptTime(LocalDateTime.now());
        orderService.save(order);

        // 通知任务发布者 + 记录订单日志（失败不影响主流程）
        try {
            Notification notif = new Notification();
            notif.setUserId(task.getUserId());
            notif.setType("订单通知");
            notif.setTitle("任务已被接单");
            notif.setContent("您发布的任务已被跑腿员接单，请等待取件配送。");
            notif.setRelatedId(order.getId());
            notif.setIsRead(0);
            notificationService.save(notif);

            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(order.getId());
            orderLog.setFromStatus(null);
            orderLog.setToStatus(0);
            orderLog.setOperatorId(userId);
            orderLog.setOperatorRole(1);
            orderLog.setRemark("跑腿员抢单成功");
            orderLogService.save(orderLog);
        } catch (Exception e) {
            log.warn("创建抢单通知/日志失败: {}", e.getMessage());
        }

        result.put("code", 200);
        result.put("message", "抢单成功");
        result.put("data", order);
        return result;
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态（状态流转）")
    public boolean updateStatus(@PathVariable Long id,
                                @RequestParam @Parameter(description = "新状态: 1=已取件,2=配送中,3=已送达") Integer status,
                                @RequestParam(required = false) @Parameter(description = "操作人用户ID") Long operatorId) {
        // 读取旧订单获取原状态
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) return false;
        Integer oldStatus = oldOrder.getStatus();

        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        switch (status) {
            case 1: order.setPickupTime(LocalDateTime.now()); break;
            case 2: order.setDeliveryTime(LocalDateTime.now()); break;
            case 3: order.setArriveTime(LocalDateTime.now()); break;
        }
        boolean success = orderService.updateById(order);
        if (!success) return false;

        // 通知订单发布者 + 记录订单日志（失败不影响主流程）
        try {
            String statusDesc = STATUS_MAP.getOrDefault(status, "状态更新");
            Notification notif = new Notification();
            notif.setUserId(oldOrder.getUserId());
            notif.setType("快递状态");
            notif.setTitle("订单" + statusDesc);
            notif.setContent("您的订单状态已更新为：" + statusDesc + "，请关注物流进度。");
            notif.setRelatedId(id);
            notif.setIsRead(0);
            notificationService.save(notif);

            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(id);
            orderLog.setFromStatus(oldStatus);
            orderLog.setToStatus(status);
            orderLog.setOperatorId(operatorId != null ? operatorId : oldOrder.getRunnerId());
            orderLog.setOperatorRole(1);
            orderLog.setRemark("跑腿员更新状态 → " + statusDesc);
            orderLogService.save(orderLog);
        } catch (Exception e) {
            log.warn("创建状态更新通知/日志失败: {}", e.getMessage());
        }

        return true;
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "用户确认收货")
    public boolean confirmArrive(@PathVariable Long id,
                                  @RequestParam(required = false) @Parameter(description = "操作人用户ID") Long operatorId) {
        // 读取旧订单获取原状态
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) return false;
        Integer oldStatus = oldOrder.getStatus();

        Order order = new Order();
        order.setId(id);
        order.setStatus(4);
        order.setUserConfirm(1);
        boolean success = orderService.updateById(order);
        if (!success) return false;

        // 通知跑腿员 + 记录订单日志（失败不影响主流程）
        try {
            Runner runner = runnerService.getById(oldOrder.getRunnerId());
            Long runnerUserId = runner != null ? runner.getUserId() : null;
            if (runnerUserId != null) {
                Notification notif = new Notification();
                notif.setUserId(runnerUserId);
                notif.setType("订单通知");
                notif.setTitle("用户已确认收货");
                notif.setContent("用户已确认收到快递，请等待用户评价。");
                notif.setRelatedId(id);
                notif.setIsRead(0);
                notificationService.save(notif);
            }

            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(id);
            orderLog.setFromStatus(oldStatus);
            orderLog.setToStatus(4);
            orderLog.setOperatorId(operatorId != null ? operatorId : oldOrder.getUserId());
            orderLog.setOperatorRole(0);
            orderLog.setRemark("用户确认收货");
            orderLogService.save(orderLog);
        } catch (Exception e) {
            log.warn("创建确认收货通知/日志失败: {}", e.getMessage());
        }

        return true;
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成订单（双方互评后）")
    public boolean complete(@PathVariable Long id,
                            @RequestParam(required = false) @Parameter(description = "操作人用户ID") Long operatorId) {
        // 读取旧订单获取原状态
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) return false;
        Integer oldStatus = oldOrder.getStatus();

        Order order = new Order();
        order.setId(id);
        order.setStatus(5);
        order.setCompleteTime(LocalDateTime.now());
        boolean success = orderService.updateById(order);
        if (!success) return false;

        // 通知双方 + 记录订单日志（失败不影响主流程）
        try {
            Notification notifUser = new Notification();
            notifUser.setUserId(oldOrder.getUserId());
            notifUser.setType("订单通知");
            notifUser.setTitle("订单已完成");
            notifUser.setContent("您的订单已完成，感谢使用校园跑腿服务！");
            notifUser.setRelatedId(id);
            notifUser.setIsRead(0);
            notificationService.save(notifUser);

            Runner runner = runnerService.getById(oldOrder.getRunnerId());
            if (runner != null) {
                Notification notifRunner = new Notification();
                notifRunner.setUserId(runner.getUserId());
                notifRunner.setType("订单通知");
                notifRunner.setTitle("订单已完成");
                notifRunner.setContent("一笔订单已完成，感谢您的辛勤付出！");
                notifRunner.setRelatedId(id);
                notifRunner.setIsRead(0);
                notificationService.save(notifRunner);
            }

            OrderLog orderLog = new OrderLog();
            orderLog.setOrderId(id);
            orderLog.setFromStatus(oldStatus);
            orderLog.setToStatus(5);
            orderLog.setOperatorId(operatorId != null ? operatorId : oldOrder.getUserId());
            orderLog.setOperatorRole(0);
            orderLog.setRemark("订单完成");
            orderLogService.save(orderLog);
        } catch (Exception e) {
            log.warn("创建订单完成通知/日志失败: {}", e.getMessage());
        }

        return true;
    }
}
