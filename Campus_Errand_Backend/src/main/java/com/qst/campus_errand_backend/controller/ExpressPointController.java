package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qst.campus_errand_backend.entity.ExpressPoint;
import com.qst.campus_errand_backend.service.ExpressPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/express-point")
@Tag(name = "快递点管理", description = "快递点增删改查相关接口")
public class ExpressPointController {

    @Autowired
    private ExpressPointService expressPointService;

    @GetMapping("/list")
    @Operation(summary = "获取启用的快递点列表")
    public List<ExpressPoint> list() {
        return expressPointService.list(
                new LambdaQueryWrapper<ExpressPoint>()
                        .eq(ExpressPoint::getStatus, 1)
                        .orderByAsc(ExpressPoint::getSortOrder));
    }

    @GetMapping("/all")
    @Operation(summary = "获取全部快递点（含禁用）")
    public List<ExpressPoint> all() {
        return expressPointService.list(
                new LambdaQueryWrapper<ExpressPoint>()
                        .orderByAsc(ExpressPoint::getSortOrder));
    }

    @PostMapping
    @Operation(summary = "新增快递点")
    public boolean save(@RequestBody ExpressPoint point) {
        return expressPointService.save(point);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新快递点")
    public boolean update(@PathVariable Long id, @RequestBody ExpressPoint point) {
        point.setId(id);
        return expressPointService.updateById(point);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用快递点")
    public boolean updateStatus(@PathVariable Long id,
                                @RequestParam @Parameter(description = "0=禁用, 1=启用") Integer status) {
        ExpressPoint point = new ExpressPoint();
        point.setId(id);
        point.setStatus(status);
        return expressPointService.updateById(point);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除快递点")
    public boolean delete(@PathVariable Long id) {
        return expressPointService.removeById(id);
    }
}
