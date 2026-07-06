package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qst.campus_errand_backend.entity.*;
import com.qst.campus_errand_backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
@Tag(name = "评价管理", description = "用户与跑腿员互评相关接口")
public class EvaluationController {

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RunnerService runnerService;

    @Autowired
    private UserService userService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private TransactionRecordService transactionRecordService;

    @PostMapping
    @Operation(summary = "提交评价（自动结算跑腿费）")
    public boolean save(@RequestBody Evaluation evaluation) {
        boolean success = evaluationService.save(evaluation);
        if (!success) return false;

        // 通知被评价方
        try {
            Notification notif = new Notification();
            notif.setUserId(evaluation.getToUserId());
            notif.setType("系统通知");
            notif.setTitle("收到新评价");
            notif.setContent("您收到了一条" + evaluation.getScore() + "星评价，快去查看吧！");
            notif.setRelatedId(evaluation.getOrderId());
            notif.setIsRead(0);
            notificationService.save(notif);
        } catch (Exception e) {
            log.warn("创建评价通知失败: {}", e.getMessage());
        }

        // 结算跑腿费给跑腿员（只结算一次）
        try {
            payRunner(evaluation);
        } catch (Exception e) {
            log.warn("结算跑腿费失败: {}", e.getMessage());
        }

        // 检查双方是否都已评价，完成订单
        try {
            checkAndComplete(evaluation.getOrderId());
        } catch (Exception e) {
            log.warn("完成订单失败: {}", e.getMessage());
        }

        return true;
    }

    /** 向跑腿员支付跑腿费 */
    private void payRunner(Evaluation evaluation) {
        Order order = orderService.getById(evaluation.getOrderId());
        if (order == null) return;

        // 检查是否已结算过（避免重复付款）
        long count = settlementService.count(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getOrderId, evaluation.getOrderId())
                .eq(Settlement::getStatus, 1));
        if (count > 0) return;

        Task task = taskService.getById(order.getTaskId());
        if (task == null || task.getFee() == null) return;

        // 查找跑腿员的 Runner 记录
        Runner runner = runnerService.getOne(new LambdaQueryWrapper<Runner>()
                .eq(Runner::getUserId, evaluation.getToUserId()));
        if (runner == null) return;

        BigDecimal fee = task.getFee();

        // 给跑腿员用户账户加钱
        userService.update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, evaluation.getToUserId())
                .setSql("balance = balance + " + fee));

        User runnerUser = userService.getById(evaluation.getToUserId());

        // 创建结算记录
        Settlement settlement = new Settlement();
        settlement.setOrderId(order.getId());
        settlement.setRunnerId(runner.getId());
        settlement.setUserId(order.getUserId());
        settlement.setAmount(fee);
        settlement.setStatus(1);
        settlement.setSettleTime(LocalDateTime.now());
        settlement.setRemark("评价后自动结算");
        settlementService.save(settlement);

        // 记录交易流水
        TransactionRecord record = new TransactionRecord();
        record.setUserId(evaluation.getToUserId());
        record.setType(2); // 收入跑腿费
        record.setAmount(fee);
        record.setBalance(runnerUser != null ? runnerUser.getBalance() : fee);
        record.setRelatedId(order.getId());
        record.setRemark("完成跑腿任务收入");
        transactionRecordService.save(record);

        // 更新跑腿员统计
        Runner updateRunner = new Runner();
        updateRunner.setId(runner.getId());
        updateRunner.setOrderCount((runner.getOrderCount() != null ? runner.getOrderCount() : 0) + 1);
        updateRunner.setTotalIncome((runner.getTotalIncome() != null ? runner.getTotalIncome() : BigDecimal.ZERO).add(fee));

        // 重新计算平均评分
        List<Evaluation> evals = evaluationService.list(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getToUserId, evaluation.getToUserId()));
        if (!evals.isEmpty()) {
            BigDecimal totalScore = evals.stream()
                    .map(e -> BigDecimal.valueOf(e.getScore() != null ? e.getScore() : 0))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            updateRunner.setScore(totalScore.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP));
        }
        runnerService.updateById(updateRunner);
    }

    /** 检查双方是否都已评价，是则完成订单 */
    private void checkAndComplete(Long orderId) {
        List<Evaluation> evals = evaluationService.list(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, orderId));
        if (evals.size() >= 2) {
            Order order = new Order();
            order.setId(orderId);
            order.setStatus(5);
            order.setCompleteTime(LocalDateTime.now());
            orderService.updateById(order);
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的双方评价")
    public List<Evaluation> getByOrderId(@PathVariable Long orderId) {
        return evaluationService.list(
                new LambdaQueryWrapper<Evaluation>().eq(Evaluation::getOrderId, orderId));
    }

    @GetMapping("/from/{userId}")
    @Operation(summary = "查询用户发出的评价")
    public List<Evaluation> getByFromUser(@PathVariable Long userId) {
        return evaluationService.list(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getFromUserId, userId)
                        .orderByDesc(Evaluation::getCreateTime));
    }

    @GetMapping("/to/{userId}")
    @Operation(summary = "查询用户收到的评价")
    public List<Evaluation> getByToUser(@PathVariable Long userId) {
        return evaluationService.list(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getToUserId, userId)
                        .orderByDesc(Evaluation::getCreateTime));
    }
}
