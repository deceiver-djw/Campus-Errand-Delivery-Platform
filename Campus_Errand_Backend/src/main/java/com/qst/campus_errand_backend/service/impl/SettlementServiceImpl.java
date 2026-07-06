package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.Settlement;
import com.qst.campus_errand_backend.mapper.SettlementMapper;
import com.qst.campus_errand_backend.service.SettlementService;
import org.springframework.stereotype.Service;

@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementMapper, Settlement> implements SettlementService {
}
