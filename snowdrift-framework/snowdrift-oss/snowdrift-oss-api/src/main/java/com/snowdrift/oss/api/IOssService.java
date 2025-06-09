package com.snowdrift.oss.api;

import com.snowdrift.oss.api.bo.OssObj;
import com.snowdrift.oss.api.bo.OssUploaded;
import com.snowdrift.oss.api.exception.OssException;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * IOssService
 *
 * @author gaoye
 * @date 2025/03/25 15:14:51
 * @description 对象存储服务接口
 * @since 1.0.0
 */
public interface IOssService {

    String getBucket() throws OssException;

    boolean bucketExists(String bucket) throws OssException;

    boolean createBucket() throws OssException;

    boolean createBucket(String bucket) throws OssException;

    boolean createBucket(String bucket,String policy) throws OssException;

    boolean setBucketPolicy(String bucket,String policy) throws OssException;

    List<String> listBuckets() throws OssException;

    boolean removeBucket(String bucket) throws OssException;

    List<OssObj> listObjects() throws OssException;

    List<OssObj> listObjects(boolean recursive) throws OssException;

    List<OssObj> listObjects(String bucket, boolean recursive) throws OssException;

    List<OssObj> listObjects(String bucket, String prefix) throws OssException;

    List<OssObj> listObjects(String bucket, String prefix, boolean recursive) throws OssException;

    InputStream getObject(String objectName) throws OssException;

    InputStream getObject(String bucket, String objectName) throws OssException;

    String getObjectUrl(String objectName) throws OssException;

    String getObjectUrl(String bucket, String objectName) throws OssException;

    String getObjectUrl(String bucket, String objectName, int expireDays) throws OssException;

    OssUploaded putObject(String objectName, InputStream inputStream) throws OssException;

    OssUploaded putObject(String bucket, String objectName, InputStream inputStream) throws OssException;

    OssUploaded putObject(String bucket, String objectName, InputStream inputStream, String contentType) throws OssException;

    boolean removeObject(String objectName) throws OssException;

    boolean removeObject(String bucket, String objectName) throws OssException;

    List<String> removeObjects(Collection<String> objectNames) throws OssException;

    List<String> removeObjects(String bucket, Collection<String> objectNames) throws OssException;
}