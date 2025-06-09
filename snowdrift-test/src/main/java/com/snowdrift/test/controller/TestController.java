package com.snowdrift.test.controller;

import com.snowdrift.core.result.JsonResult;
import com.snowdrift.test.dto.UserDto;
import com.snowdrift.web.anno.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Tag(name = "测试模块",description = "测试模块")
@RestController
@RequestMapping("/v1/test")
public class TestController {

    @Operation(summary = "测试日志接口",description = "测试日志口")
    @AccessLog(value = "测试日志接口", module = "测试模块")
    @PostMapping
    public JsonResult<UserDto> test2(@RequestBody @Validated UserDto userDto) {
        int i = 1 / 0;
        return JsonResult.ok(userDto);
    }
}