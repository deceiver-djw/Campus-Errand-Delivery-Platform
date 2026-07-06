package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Settlement;
import com.qst.campus_errand_backend.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settlement")
@Tag(name = "结算管理", description = "跑腿费结算、提现相关接口")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/list")
    @Operation(summary = "分页查询结算记录")
    public Page<Settlement> list(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) @Parameter(description = "结算状态: 0=待结算,1=已结算,2=已退款") Integer status,
                                  @RequestParam(required = false) @Parameter(description = "跑腿员ID") Long runnerId) {
        LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Settlement::getStatus, status);
        if (runnerId != null) wrapper.eq(Settlement::getRunnerId, runnerId);
        wrapper.orderByDesc(Settlement::getCreateTime);
        return settlementService.page(new Page<>(page, size), wrapper);
    }

    @GetMapping("/runner/{runnerId}")
    @Operation(summary = "查询跑腿员结算记录")
    public Object runnerSettlements(@PathVariable Long runnerId) {
        return settlementService.list(
                new LambdaQueryWrapper<Settlement>()
                        .eq(Settlement::getRunnerId, runnerId)
                        .orderByDesc(Settlement::getCreateTime));
    }

    @PutMapping("/{id}/settle")
    @Operation(summary = "执行结算")
    public boolean settle(@PathVariable Long id) {
        Settlement settlement = new Settlement();
        settlement.setId(id);
        settlement.setStatus(1);
        settlement.setSettleTime(LocalDateTime.now());
        return settlementService.updateById(settlement);
    }

    @PutMapping("/{id}/refund")
    @Operation(summary = "执行退款")
    public boolean refund(@PathVariable Long id) {
        Settlement settlement = new Settlement();
        settlement.setId(id);
        settlement.setStatus(2);
        settlement.setSettleTime(LocalDateTime.now());
        return settlementService.updateById(settlement);
    }
}
