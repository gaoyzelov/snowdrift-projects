package com.snowdrift.test.controller;

import com.snowdrift.core.result.JsonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @ApiOperation(value = "测试接口")
    @GetMapping
    public JsonResult<?> test(){
        return JsonResult.ok();
    }
}