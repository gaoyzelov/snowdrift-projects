package com.snowdrift.oss.api.bo;

import io.swagger.annotations.Schema;
import io.swagger.annotations.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * OssObj
 *
 * @author gaoye
 * @date 2025/03/25 15:17:29
 * @description OSS对象
 * @since 1.0.0
 */
@Data
@Schema(value = "OssObj", description = "OSS对象")
public class OssObj implements Serializable {

    @Schema(value = "存储桶", example = "test")
    private String bucket;

    @Schema(value = "对象名", example = "test.jpg", position = 1)
    private String name;

    @Schema(value = "文件大小，Byte", example = "1024", position = 2)
    private Long size;

    @Schema(value = "是否为文件夹", example = "true", position = 3)
    private Boolean isDir;

}