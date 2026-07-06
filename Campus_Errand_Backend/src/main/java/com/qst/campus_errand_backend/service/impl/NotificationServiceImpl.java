package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.Notification;
import com.qst.campus_errand_backend.mapper.NotificationMapper;
import com.qst.campus_errand_backend.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
}
