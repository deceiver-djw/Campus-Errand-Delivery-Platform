package com.qst.campus_errand_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.campus_errand_backend.entity.Message;
import com.qst.campus_errand_backend.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/message")
@Tag(name = "消息管理", description = "站内私信收发相关接口")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping
    @Operation(summary = "发送消息")
    public boolean send(@RequestBody Message message) {
        return messageService.save(message);
    }

    @GetMapping("/chat")
    @Operation(summary = "查询两人聊天记录")
    public List<Message> chat(@RequestParam Long userId1,
                               @RequestParam Long userId2) {
        return messageService.list(
                new LambdaQueryWrapper<Message>()
                        .and(w -> w.eq(Message::getFromUserId, userId1).eq(Message::getToUserId, userId2))
                        .or(w -> w.eq(Message::getFromUserId, userId2).eq(Message::getToUserId, userId1))
                        .orderByAsc(Message::getCreateTime));
    }

    @GetMapping("/unread/{userId}")
    @Operation(summary = "查询未读消息")
    public List<Message> unread(@PathVariable Long userId) {
        return messageService.list(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getToUserId, userId)
                        .eq(Message::getIsRead, 0)
                        .orderByDesc(Message::getCreateTime));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户所有收到的消息（含已读和未读）")
    public List<Message> userMessages(@PathVariable Long userId) {
        return messageService.list(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getToUserId, userId)
                        .orderByDesc(Message::getCreateTime));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public boolean readOne(@PathVariable Long id) {
        Message msg = new Message();
        msg.setId(id);
        msg.setIsRead(1);
        msg.setReadTime(LocalDateTime.now());
        return messageService.updateById(msg);
    }

    @PutMapping("/read-all/{userId}")
    @Operation(summary = "全部标记已读")
    public boolean readAll(@PathVariable Long userId) {
        return messageService.update(
                new LambdaUpdateWrapper<Message>()
                        .eq(Message::getToUserId, userId)
                        .eq(Message::getIsRead, 0)
                        .set(Message::getIsRead, 1)
                        .set(Message::getReadTime, LocalDateTime.now()));
    }
}
