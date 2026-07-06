package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.TransactionRecord;
import com.qst.campus_errand_backend.service.TransactionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "资金流水", description = "平台资金流水查询接口")
public class TransactionRecordController {

    @Autowired
    private TransactionRecordService transactionRecordService;

    @GetMapping("/list")
    @Operation(summary = "分页查询资金流水")
    public Page<TransactionRecord> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) @Parameter(description = "用户ID") Long userId,
                                         @RequestParam(required = false) @Parameter(description = "交易类型") Integer type) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(TransactionRecord::getUserId, userId);
        if (type != null) wrapper.eq(TransactionRecord::getType, type);
        wrapper.orderByDesc(TransactionRecord::getCreateTime);
        return transactionRecordService.page(new Page<>(page, size), wrapper);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户账户流水")
    public Page<TransactionRecord> userRecords(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return transactionRecordService.page(new Page<>(page, size),
                new LambdaQueryWrapper<TransactionRecord>()
                        .eq(TransactionRecord::getUserId, userId)
                        .orderByDesc(TransactionRecord::getCreateTime));
    }
}
