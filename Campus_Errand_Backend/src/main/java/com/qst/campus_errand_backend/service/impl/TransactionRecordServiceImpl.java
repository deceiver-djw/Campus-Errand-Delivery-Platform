package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.TransactionRecord;
import com.qst.campus_errand_backend.mapper.TransactionRecordMapper;
import com.qst.campus_errand_backend.service.TransactionRecordService;
import org.springframework.stereotype.Service;

@Service
public class TransactionRecordServiceImpl extends ServiceImpl<TransactionRecordMapper, TransactionRecord> implements TransactionRecordService {
}
