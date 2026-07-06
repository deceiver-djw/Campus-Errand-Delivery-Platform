package com.qst.campus_errand_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qst.campus_errand_backend.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动评价服务：已送达超过1分钟的订单自动完成评价
 */
@Component
public class AutoEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AutoEvaluationService.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RunnerService runnerService;

    @Autowired
    private UserService userService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private TransactionRecordService transactionRecordService;

    @Autowired
    private NotificationService notificationService;

    /** 每30秒检查一次 */
    @Scheduled(fixedDelay = 30000)
    public void autoEvaluate() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(1);

        // 查找已送达(status=3)或待评价(status=4)且超过1分钟的订单
        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 3, 4)
                .isNotNull(Order::getArriveTime)
                .lt(Order::getArriveTime, deadline));

        for (Order order : orders) {
            try {
                // 检查用户是否已评价
                long evalCount = evaluationService.count(new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getOrderId, order.getId())
                        .eq(Evaluation::getFromUserId, order.getUserId()));
                if (evalCount > 0) continue;

                // 如果还是已送达状态，先确认收货
                if (order.getStatus() == 3) {
                    Order upd = new Order();
                    upd.setId(order.getId());
                    upd.setStatus(4);
                    upd.setUserConfirm(1);
                    orderService.updateById(upd);
                }

                // 获取跑腿员的userId
                Runner runner = runnerService.getById(order.getRunnerId());
                if (runner == null) continue;

                Long runnerUserId = runner.getUserId();

                // 创建自动评价
                Evaluation eval = new Evaluation();
                eval.setOrderId(order.getId());
                eval.setFromUserId(order.getUserId());
                eval.setToUserId(runnerUserId);
                eval.setScore(5);
                eval.setContent("很好，不错");
                eval.setType(0);
                evaluationService.save(eval);

                log.info("自动评价完成: orderId={}", order.getId());

                // 通知跑腿员
                tryNotify(runnerUserId, order.getId());

                // 结算跑腿费
                trySettle(order, runner, runnerUserId);

                // 检查是否完成订单
                tryComplete(order.getId());

            } catch (Exception e) {
                log.warn("自动评价订单{}失败: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void tryNotify(Long userId, Long orderId) {
        try {
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType("系统通知");
            notif.setTitle("收到系统自动评价");
            notif.setContent("用户超时未评价，系统已自动给出5星好评。");
            notif.setRelatedId(orderId);
            notif.setIsRead(0);
            notificationService.save(notif);
        } catch (Exception e) {
            log.warn("自动评价通知失败: {}", e.getMessage());
        }
    }

    private void trySettle(Order order, Runner runner, Long runnerUserId) {
        try {
            long count = settlementService.count(new LambdaQueryWrapper<Settlement>()
                    .eq(Settlement::getOrderId, order.getId())
                    .eq(Settlement::getStatus, 1));
            if (count > 0) return;

            Task task = taskService.getById(order.getTaskId());
            if (task == null || task.getFee() == null) return;

            BigDecimal fee = task.getFee();

            // 给跑腿员加钱
            userService.update(new LambdaUpdateWrapper<User>()
                    .eq(User::getId, runnerUserId)
                    .setSql("balance = balance + " + fee));

            User runnerUser = userService.getById(runnerUserId);

            // 创建结算记录
            Settlement settlement = new Settlement();
            settlement.setOrderId(order.getId());
            settlement.setRunnerId(runner.getId());
            settlement.setUserId(order.getUserId());
            settlement.setAmount(fee);
            settlement.setStatus(1);
            settlement.setSettleTime(LocalDateTime.now());
            settlement.setRemark("系统自动评价后结算");
            settlementService.save(settlement);

            // 交易流水
            TransactionRecord record = new TransactionRecord();
            record.setUserId(runnerUserId);
            record.setType(2);
            record.setAmount(fee);
            record.setBalance(runnerUser != null ? runnerUser.getBalance() : fee);
            record.setRelatedId(order.getId());
            record.setRemark("完成跑腿任务收入(自动评价)");
            transactionRecordService.save(record);

            // 更新跑腿员统计
            Runner upd = new Runner();
            upd.setId(runner.getId());
            upd.setOrderCount((runner.getOrderCount() != null ? runner.getOrderCount() : 0) + 1);
            upd.setTotalIncome((runner.getTotalIncome() != null ? runner.getTotalIncome() : BigDecimal.ZERO).add(fee));

            List<Evaluation> evals = evaluationService.list(new LambdaQueryWrapper<Evaluation>()
                    .eq(Evaluation::getToUserId, runnerUserId));
            if (!evals.isEmpty()) {
                BigDecimal totalScore = evals.stream()
                        .map(e -> BigDecimal.valueOf(e.getScore() != null ? e.getScore() : 0))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                upd.setScore(totalScore.divide(BigDecimal.valueOf(evals.size()), 2, RoundingMode.HALF_UP));
            }
            runnerService.updateById(upd);

        } catch (Exception e) {
            log.warn("自动结算失败: {}", e.getMessage());
        }
    }

    private void tryComplete(Long orderId) {
        try {
            List<Evaluation> evals = evaluationService.list(new LambdaQueryWrapper<Evaluation>()
                    .eq(Evaluation::getOrderId, orderId));
            if (evals.size() >= 2) {
                Order upd = new Order();
                upd.setId(orderId);
                upd.setStatus(5);
                upd.setCompleteTime(LocalDateTime.now());
                orderService.updateById(upd);
            }
        } catch (Exception e) {
            log.warn("自动完成订单失败: {}", e.getMessage());
        }
    }
}
