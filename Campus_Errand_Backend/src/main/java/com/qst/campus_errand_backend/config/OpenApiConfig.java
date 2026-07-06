package com.qst.campus_errand_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;


public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线新闻阅读系统接口")
                        .version("1.0.0")
                        .description("基于SpringBoot3 接口文档"));
    }
}
