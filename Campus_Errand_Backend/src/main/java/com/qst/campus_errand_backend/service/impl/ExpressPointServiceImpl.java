package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.ExpressPoint;
import com.qst.campus_errand_backend.mapper.ExpressPointMapper;
import com.qst.campus_errand_backend.service.ExpressPointService;
import org.springframework.stereotype.Service;

@Service
public class ExpressPointServiceImpl extends ServiceImpl<ExpressPointMapper, ExpressPoint> implements ExpressPointService {
}
