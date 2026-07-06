package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Notification;
import com.qst.campus_errand_backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notification")
@Tag(name = "通知管理", description = "系统通知推送与查询接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户通知列表")
    public Page<Notification> list(@PathVariable Long userId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return notificationService.page(new Page<>(page, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
    }

    @GetMapping("/unread/{userId}")
    @Operation(summary = "查询未读通知")
    public List<Notification> unread(@PathVariable Long userId) {
        return notificationService.list(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreateTime));
    }

    @GetMapping("/unread-count/{userId}")
    @Operation(summary = "查询未读通知数量")
    public long unreadCount(@PathVariable Long userId) {
        return notificationService.count(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public boolean readOne(@PathVariable Long id) {
        Notification notif = new Notification();
        notif.setId(id);
        notif.setIsRead(1);
        notif.setReadTime(LocalDateTime.now());
        return notificationService.updateById(notif);
    }

    @PutMapping("/read-all/{userId}")
    @Operation(summary = "全部标记已读")
    public boolean readAll(@PathVariable Long userId) {
        return notificationService.update(
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .set(Notification::getIsRead, 1)
                        .set(Notification::getReadTime, LocalDateTime.now()));
    }
}
