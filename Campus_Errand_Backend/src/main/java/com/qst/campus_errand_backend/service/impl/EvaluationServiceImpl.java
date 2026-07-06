package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.Evaluation;
import com.qst.campus_errand_backend.mapper.EvaluationMapper;
import com.qst.campus_errand_backend.service.EvaluationService;
import org.springframework.stereotype.Service;

@Service
public class EvaluationServiceImpl extends ServiceImpl<EvaluationMapper, Evaluation> implements EvaluationService {
}
