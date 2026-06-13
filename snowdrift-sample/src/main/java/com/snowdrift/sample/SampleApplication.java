package com.snowdrift.sample;

import com.snowdrift.framework.cache.ICacheService;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Duration;

/**
 * Sample Application
 *
 * @author 83674
 * @date 2026/5/9
 * @description 示例应用启动类
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
public class SampleApplication implements ApplicationRunner {

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    @Resource
    private ICacheService cacheService;

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
        System.out.println("====================================");
        System.out.println("Snowdrift Sample 启动成功！");
        System.out.println("访问地址: http://localhost:8081");
        System.out.println("====================================");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        cacheService.put("aaaaa",1);
        cacheService.put("bbbbb",2, Duration.ofMinutes(1));
    }
}
