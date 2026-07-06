package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.Message;
import com.qst.campus_errand_backend.mapper.MessageMapper;
import com.qst.campus_errand_backend.service.MessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
}
