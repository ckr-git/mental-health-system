package com.mental.health;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能AI心理健康管理系统 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.mental.health.mapper")
@EnableScheduling
public class MentalHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApplication.class, args);
        System.out.println("========================================");
        System.out.println("智能AI心理健康管理系统启动成功！");
        System.out.println("访问地址：http://localhost:8080");
        System.out.println("========================================");
    }
}
