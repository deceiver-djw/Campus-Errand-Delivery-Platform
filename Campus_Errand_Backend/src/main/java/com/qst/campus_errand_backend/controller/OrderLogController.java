package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.campus_errand_backend.entity.OrderLog;
import com.qst.campus_errand_backend.service.OrderLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-log")
@Tag(name = "订单日志", description = "订单状态流转日志查询接口")
public class OrderLogController {

    @Autowired
    private OrderLogService orderLogService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单的完整状态日志")
    public List<OrderLog> getByOrderId(@PathVariable Long orderId) {
        return orderLogService.list(
                new LambdaQueryWrapper<OrderLog>()
                        .eq(OrderLog::getOrderId, orderId)
                        .orderByAsc(OrderLog::getCreateTime));
    }
}
