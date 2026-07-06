package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.OrderLog;
import com.qst.campus_errand_backend.mapper.OrderLogMapper;
import com.qst.campus_errand_backend.service.OrderLogService;
import org.springframework.stereotype.Service;

@Service
public class OrderLogServiceImpl extends ServiceImpl<OrderLogMapper, OrderLog> implements OrderLogService {
}
