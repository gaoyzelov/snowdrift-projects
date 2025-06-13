package com.snowdrift.pay.allin.utils;


import com.snowdrift.core.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Slf4j
@SuppressWarnings("all")
public class SybUtil {

    public static final String MD5 = "MD5";
    public static final String RSA = "RSA";


    /**
     * 对象转TreeMap
     *
     * @param obj 对象
     * @return TreeMap
     */
    public static TreeMap<String, String> objToTreeMap(Object obj) {
        TreeMap<String, String> treeMap = new TreeMap<>();
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null) {
                        treeMap.put(field.getName(), fieldValue.toString());
                    }
                } catch (IllegalAccessException e) {
                    log.error("获取对象属性[{}]异常:{}", field.getName(), e.getLocalizedMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return treeMap;
    }

    /**
     * md5签名
     */
    public static String md5Sign(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance(MD5);
            md.reset();
            md.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest();
            StringBuffer outStrBuf = new StringBuffer(32);
            for (int i = 0; i < hash.length; i++) {
                int v = hash[i] & 0xFF;
                if (v < 16) {
                    outStrBuf.append('0');
                }
                outStrBuf.append(Integer.toString(v, 16).toLowerCase());
            }
            return outStrBuf.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5签名异常：{}", e.getLocalizedMessage());
            return data;
        }
    }

    /**
     * 将TreeMap拼接成url参数
     *
     * @param data TreeMap
     * @return url参数
     */
    public static String treeMapToUrlParams(TreeMap<String, String> data) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> each : data.entrySet()) {
            joiner.add(StringUtils.join(each.getKey(), "=", each.getValue()));
        }
        return joiner.toString();
    }

    /**
     * 验签
     *
     * @param data     参数
     * @param appkey   秘钥
     * @param signType 签名方式
     * @return true or false
     */
    public static boolean validSign(TreeMap<String, String> data, String appkey, String signType) throws Exception {
        if (MapUtils.isEmpty(data) || !data.containsKey("sign")) {
            return false;
        }
        String sign = data.remove("sign");
        if (StringUtils.equals(MD5, signType)) {
            // 如果是md5则需要把md5的key加入到排序
            data.put("key", appkey);
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> each : data.entrySet()) {
            joiner.add(StringUtils.join(each.getKey(), "=", each.getValue()));
        }
        log.debug("验签原文 : {}", joiner.toString());
        if (StringUtils.equals(MD5, signType)) {
            return StringUtils.equalsIgnoreCase(sign, md5Sign(joiner.toString()));
        } else {
            return rsaSignVerify(joiner.toString(), sign, appkey);
        }
    }

    /**
     * RSA签名校验
     *
     * @param content   待签名内容
     * @param sign      签名
     * @param publicKey 公钥
     * @return 签名校验结果
     */
    public static boolean rsaSignVerify(String data, String sign, String publicKey) throws Exception {
        try {
            PublicKey pubKey = getPublicKeyFromX509(RSA, Base64.decodeBase64(publicKey.getBytes()));
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(pubKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decodeBase64(sign.getBytes()));
        } catch (Exception e) {
            log.error("RSA验签异常：{}", e.getLocalizedMessage());
            throw new BaseException("RSA content = " + data + ",sign=" + sign, e);
        }
    }

    /**
     * 统一签名
     *
     * @param data     待签数据
     * @param appkey   秘钥
     * @param signType 签名方式
     * @return 签名结果
     */
    public static String unionSign(TreeMap<String, String> data, String appkey, String signType) throws Exception {
        data.remove("sign");
        if (StringUtils.equals(MD5, signType)) {
            // 如果是md5则需要把md5的key加入到排序
            data.put("key", appkey);
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> each : data.entrySet()) {
            joiner.add(StringUtils.join(each.getKey(), "=", each.getValue()));
        }
        log.debug("签名原文 : {}", joiner.toString());
        if ("MD5".equals(signType)) {
            String sign = md5Sign(joiner.toString());
            data.remove("key");
            return sign;
        } else {
            return rsaSign(joiner.toString(), appkey);
        }
    }

    /**
     * RSA签名
     *
     * @param data     待签名数据
     * @param appkey   appkey
     * @param signType 签名方式
     * @return 签名
     */
    public static String rsaSign(String content, String privateKey) throws Exception {
        PrivateKey priKey = getPrivateKeyFromPKCS8(RSA, Base64.decodeBase64(privateKey.getBytes()));
        Signature signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(priKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return new String(Base64.encodeBase64(signed));
    }

    /**
     * 动态遍历获取所有收到的参数
     * 因为收银宝以后可能会加字段,动态获取可以兼容由于收银宝加字段而引起的签名异常
     *
     * @param request HttpServletRequest
     * @return 请求携带参数
     */
    public static TreeMap<String, String> getRequestParams(HttpServletRequest request) {
        TreeMap<String, String> data = new TreeMap();
        Map reqMap = request.getParameterMap();
        for (Object key : reqMap.keySet()) {
            String title = ((String[]) reqMap.get(key))[0];
            data.put(key.toString(), value);
        }
        return data;
    }

    public static PrivateKey getPrivateKeyFromPKCS8(String algorithm, byte[] encodedKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
    }

    public static PublicKey getPublicKeyFromX509(String algorithm, byte[] encodedKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
    }
}