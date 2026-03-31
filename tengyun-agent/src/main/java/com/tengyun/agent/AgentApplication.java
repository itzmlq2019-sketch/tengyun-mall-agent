package com.tengyun.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@EnableAsync // 开启异步支持审计存库
@MapperScan("com.tengyun.agent.mapper")
@EnableFeignClients(basePackages = "com.tengyun.agent.client") // 指向 Feign 接口所在包
public class AgentApplication {
    public static void main(String[] args) {


        SpringApplication.run(AgentApplication.class, args);
    }
}