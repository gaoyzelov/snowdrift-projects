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
@Schema(title = "OssObj", description = "OSS对象")
public class OssObj implements Serializable {

    @Schema(title = "存储桶", example = "test")
    private String bucket;

    @Schema(title = "对象名", example = "test.jpg")
    private String name;

    @Schema(title = "文件大小，Byte", example = "1024")
    private Long size;

    @Schema(title = "是否为文件夹", example = "true")
    private Boolean isDir;

}