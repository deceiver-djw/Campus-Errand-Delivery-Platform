package com.qst.campus_errand_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.campus_errand_backend.entity.Runner;
import com.qst.campus_errand_backend.mapper.RunnerMapper;
import com.qst.campus_errand_backend.service.RunnerService;
import org.springframework.stereotype.Service;

@Service
public class RunnerServiceImpl extends ServiceImpl<RunnerMapper, Runner> implements RunnerService {
}
