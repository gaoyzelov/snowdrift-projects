# snowdrift-oss

对象存储模块，策略模式驱动，一套 API 适配 5 种存储后端，配置即切换。

## 模块结构

```
snowdrift-oss
├── snowdrift-oss-core        ← 通用层：IOssService、OssStrategyFactory、AbstractOssService
├── snowdrift-oss-local       ← 本地文件系统存储
├── snowdrift-oss-minio       ← MinIO 对象存储
├── snowdrift-oss-aliyun      ← 阿里云 OSS
├── snowdrift-oss-qiniu       ← 七牛云 Kodo
└── snowdrift-oss-tencent     ← 腾讯云 COS
```

## 快速开始

按需引入一个后端实现即可，核心 API（`snowdrift-oss-core`）会作为传递依赖自动引入。

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-oss-minio</artifactId>
</dependency>
<!-- 或 local / aliyun / qiniu / tencent -->
```

## 配置

```yaml
snowdrift:
  oss:
    default-config-key: default
    configs:
      default:
        oss-type: minio                    # local / minio / aliyun / qiniu / tencent
        endpoint: http://localhost:9000
        access-key: minioadmin
        secret-key: minioadmin
        bucket: my-bucket
        domain: http://localhost:9000/my-bucket
        private-bucket: false
        path-prefix: upload/
        signature-expiry: 30               # 签名 URL 有效期（分钟），默认 30
```

多实例配置（如同时对接阿里云和七牛云）：

```yaml
snowdrift:
  oss:
    default-config-key: aliyun
    configs:
      aliyun:
        oss-type: aliyun
        endpoint: oss-cn-hangzhou.aliyuncs.com
        access-key: xxx
        secret-key: xxx
        bucket: public-bucket
        domain: https://cdn.example.com
      qiniu:
        oss-type: qiniu
        access-key: xxx
        secret-key: xxx
        bucket: private-bucket
        domain: https://cdn2.example.com
        private-bucket: true
```

## 代码示例

注入 `IOssService` 或通过 `OssStrategyFactory` 按配置名获取：

```java
// 默认配置
@Autowired
private IOssService ossService;

// 多实例——按名称切换
@Autowired
private OssStrategyFactory ossFactory;

IOssService qiniuOss = ossFactory.getService("qiniu");
```

### 文件上传

```java
// InputStream 上传
OssUploadRequest request = OssUploadRequest.builder()
        .inputStream(stream)
        .objectKey("avatar/user-123.jpg")
        .contentType("image/jpeg")
        .size(fileSize)
        .build();
OssResult result = ossService.upload(request);

// File 上传
OssResult result = ossService.upload(OssUploadRequest.from(new File("test.jpg")));
```

返回的 `OssResult` 包含 `objectKey`、`url`、`bucket`、`size`。

### 文件下载

```java
InputStream stream = ossService.download("avatar/user-123.jpg");
// 使用后关闭流
```

### 文件 URL

```java
// 公开 Bucket：直接返回 CDN/域名 URL
String url = ossService.getUrl("avatar/user-123.jpg", null);

// 私有 Bucket：生成带签名的临时 URL（默认 30 分钟有效）
String signedUrl = ossService.getUrl("avatar/user-123.jpg", Duration.ofHours(1));
```

### 文件删除

```java
ossService.delete("avatar/user-123.jpg");
ossService.deleteBatch(List.of("file1.jpg", "file2.jpg"));
```

### 文件存在性

```java
boolean exists = ossService.exists("avatar/user-123.jpg");
```

## 后端对比

| 后端 | 模块 | 批量删除 | 私有 Bucket | 路径穿越防护 |
|------|------|---------|------------|:--:|
| Local | `snowdrift-oss-local` | 逐条 | — | ✅ 双重 |
| MinIO | `snowdrift-oss-minio` | ✅ 原生 | ✅ | ✅ |
| 阿里云 OSS | `snowdrift-oss-aliyun` | ✅ 原生 | ✅ | ✅ |
| 七牛云 Kodo | `snowdrift-oss-qiniu` | ✅ 原生 | ✅ | ✅ |
| 腾讯云 COS | `snowdrift-oss-tencent` | ✅ 原生 | ✅ | ✅ |

## 扩展

实现 `IOssService` 接口并注册为 Spring Bean 即可接入其他存储后端。参考现有 Provider 模块实现 `AbstractOssService` 子类。

## 配置属性参考

### snowdrift.oss

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `default-config-key` | String | — | 默认配置的 key |
| `configs.<name>.oss-type` | String | — | 后端类型（local/minio/aliyun/qiniu/tencent） |
| `configs.<name>.endpoint` | String | — | 服务端点 |
| `configs.<name>.access-key` | String | — | 访问密钥 |
| `configs.<name>.secret-key` | String | — | 秘密密钥 |
| `configs.<name>.bucket` | String | — | Bucket 名称 |
| `configs.<name>.domain` | String | — | 访问域名/CDN |
| `configs.<name>.region` | String | — | 区域 |
| `configs.<name>.private-bucket` | Boolean | false | 是否私有 Bucket |
| `configs.<name>.path-prefix` | String | "" | 上传路径前缀 |
| `configs.<name>.signature-expiry` | Integer | 30 | 签名过期时间（分钟） |
| `configs.<name>.upload-token-expire` | Integer | — | 上传 Token 过期时间（分钟，七牛专用） |
