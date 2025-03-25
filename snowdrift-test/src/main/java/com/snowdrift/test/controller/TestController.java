package com.snowdrift.test.controller;

import com.snowdrift.cache.redis.anno.DistLock;
import com.snowdrift.core.result.JsonResult;
import com.snowdrift.test.dto.UserDto;
import com.snowdrift.web.anno.AccessLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * TestController
 *
 * @author gaoye
 * @date 2025/03/18 18:20:49
 * @description xxxxxxxx
 * @since 1.0
 */
@Api(tags = "测试接口")
@RestController
@RequestMapping("/v1/test")
public class TestController {

    @ApiOperation(value = "测试分布式锁接口")
    @GetMapping
    @DistLock(value = "test:", key = "#id")
    public JsonResult<?> test(Integer id) throws InterruptedException{
        TimeUnit.SECONDS.sleep(id);
        return JsonResult.ok();
    }

    @ApiOperation(value = "测试日志口")
    @AccessLog(value = "测试日志接口", module = "测试模块")
    @PostMapping
    public JsonResult<UserDto> test2(@RequestBody @Validated UserDto userDto) {
        int i = 1 / 0;
        return JsonResult.ok(userDto);
    }
}