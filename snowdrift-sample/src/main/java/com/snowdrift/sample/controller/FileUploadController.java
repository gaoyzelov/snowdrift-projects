package com.snowdrift.sample.controller;

import com.snowdrift.framework.common.result.Result;
import com.snowdrift.framework.oss.core.IOssService;
import com.snowdrift.framework.oss.core.OssStrategyFactory;
import com.snowdrift.framework.oss.dto.OssResult;
import com.snowdrift.framework.oss.dto.OssUploadRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 *
 * @author 83674
 * @date 2026/5/9
 * @description 测试 OSS 本地存储上传功能
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Resource
    private OssStrategyFactory ossStrategyFactory;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            // 获取默认 OSS 服务
            IOssService ossService = ossStrategyFactory.getDefaultService();

            // 构建上传请求
            OssUploadRequest request = OssUploadRequest.builder()
                    .objectKey(System.currentTimeMillis() + "_" + file.getOriginalFilename())
                    .inputStream(file.getInputStream())
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            // 执行上传
            OssResult result = ossService.upload(request);

            // 返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("objectKey", result.getObjectKey());
            data.put("url", result.getUrl());
            data.put("size", result.getSize());
            data.put("bucket", result.getBucket());

            return Result.ok(data);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.err("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam String objectKey) {
        try {
            IOssService ossService = ossStrategyFactory.getDefaultService();
            ossService.delete(objectKey);
            return Result.ok();
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.err("文件删除失败: " + e.getMessage());
        }
    }
}
