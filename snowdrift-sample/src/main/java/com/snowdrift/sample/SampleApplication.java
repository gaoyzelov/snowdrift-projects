package com.snowdrift.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Application
 *
 * @author 83674
 * @date 2026/5/9
 * @description 示例应用启动类
 * @since 1.0.0
 */
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
        System.out.println("====================================");
        System.out.println("Snowdrift Sample 启动成功！");
        System.out.println("访问地址: http://localhost:8081");
        System.out.println("====================================");
    }
}
