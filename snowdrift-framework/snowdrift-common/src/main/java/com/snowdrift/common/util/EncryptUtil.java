package com.snowdrift.common.util;

import com.snowdrift.common.exception.BizException;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * CryptoUtil
 *
 * @author 83674
 * @date 2026/3/27-13:05
 * @description 加密工具类
 * @since 1.0.0
 */
public final class EncryptUtil {

    private EncryptUtil() {
    }

    /**
     * 获取消息摘要实例
     *
     * @param algorithm 算法
     * @return 消息摘要实例
     * @throws {@link NoSuchAlgorithmException} 找不到算法异常
     */
    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        AssertUtil.notBlank(algorithm, "算法不能为空");
        return MessageDigest.getInstance(algorithm);
    }

    /**
     * md5加密
     *
     * @param text 文本
     * @return md5 (16进制字符串)
     */
    public static String md5(String text) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        try {
            // 获取MD5消息摘要实例
            MessageDigest md5 = EncryptUtil.getInstance("MD5");
            // 计算消息摘要
            byte[] bytes = md5.digest(text.getBytes(StandardCharsets.UTF_8));
            // 转为16进制字符串
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * sha1
     *
     * @param text 文本
     * @return sha1  (16进制字符串)
     */
    public static String sha1(String text) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        try {
            // 获取SHA1消息摘要实例
            MessageDigest sha1 = EncryptUtil.getInstance("SHA-1");
            // 计算消息摘要
            byte[] bytes = sha1.digest(text.getBytes(StandardCharsets.UTF_8));
            // 转为16进制字符串
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * sha256
     *
     * @param text 文本
     * @return sha256  (16进制字符串)
     */
    public static String sha256(String text) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        try {
            // 获取SHA256消息摘要实例
            MessageDigest sha256 = EncryptUtil.getInstance("SHA-256");
            // 计算消息摘要
            byte[] bytes = sha256.digest(text.getBytes(StandardCharsets.UTF_8));
            // 转为16进制字符串
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * 随机生成AES密钥(256)
     *
     * @return AES密钥  (16进制字符串)
     */
    public static String aesKey() {
        SecretKey secretKey = aesKey(256);
        return HexFormat.of().formatHex(secretKey.getEncoded());
    }

    /**
     * 随机生成AES密钥
     *
     * @param keySize 密钥长度
     * @return AES密钥
     */
    public static SecretKey aesKey(int keySize) {
        AssertUtil.inside(keySize, List.of(128, 192, 256), "秘钥长度仅支持128、192或256");
        try {
            // 获取AES密钥生成器实例
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            // 设置秘钥长度
            keyGenerator.init(keySize);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * aes加密
     *
     * @param text   待加密文本
     * @param aesKey aes密钥
     * @return 加密后的文本  (Base64格式符串)
     */
    public static String aesEncrypt(String text, String aesKey) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        AssertUtil.notBlank(aesKey, "秘钥不能为空");
        return aesEncrypt(text, HexFormat.of().parseHex(aesKey));
    }

    /**
     * aes加密
     *
     * @param text 待加密文本
     * @param key  密钥
     * @return 加密后的文本  (Base64格式符串)
     */
    public static String aesEncrypt(String text, byte[] key) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        AssertUtil.custom(() -> ArrayUtils.isNotEmpty(key), "秘钥不能为空");
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            // 算法/模式/补码方式
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * aes解密
     *
     * @param text   待解密文本 (Base64格式符串)
     * @param aesKey aes密钥
     * @return 解密后的文本
     */
    public static String aesDecrypt(String text, String aesKey) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        AssertUtil.notBlank(aesKey, "秘钥不能为空");
        return aesDecrypt(text, HexFormat.of().parseHex(aesKey));
    }

    /**
     * aes解密
     *
     * @param text 待解密文本 (Base64格式符串)
     * @param key  密钥
     * @return 解密后的文本
     */
    public static String aesDecrypt(String text, byte[] key) {
        AssertUtil.notBlank(text, "待加密文本不能为空");
        AssertUtil.custom(() -> ArrayUtils.isNotEmpty(key), "秘钥不能为空");
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            // 算法/模式/补码方式
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(text));
            return StringUtils.toEncodedString(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * 随机生成RSA密钥对
     *
     * @param keySize 密钥长度
     * @return 密钥对
     */
    public static KeyPair rsaKeyPair(int keySize) {
        AssertUtil.inside(keySize, List.of(512, 1024, 2048, 4096), "秘钥长度仅支持512、1024、2048或4096");
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * 随机生成RSA密钥对
     *
     * @return 密钥对对象
     */
    public static KeyPair rsaKeyPair() {
        return EncryptUtil.rsaKeyPair(2048);
    }

    /**
     * 随机生成RSA密钥对
     *
     * @return 密钥对对象
     */
    public static RsaKeyPair rsaKeyPairBase64() {
        KeyPair keyPair = EncryptUtil.rsaKeyPair();
        Base64.Encoder encoder = Base64.getEncoder();
        return RsaKeyPair.builder()
                .publicKey(encoder.encodeToString(keyPair.getPublic().getEncoded()))
                .privateKey(encoder.encodeToString(keyPair.getPrivate().getEncoded()))
                .build();
    }

    /**
     * 随机生成RSA密钥对
     *
     * @return 密钥对对象
     */
    public static RsaKeyPair rsaKeyPairPem() {
        RsaKeyPair rsaKeyPair = rsaKeyPairBase64();
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" + formatBase64(rsaKeyPair.getPublicKey()) + "\n-----END PUBLIC KEY-----";
        String privateKey = "-----BEGIN PRIVATE KEY-----\n" + formatBase64(rsaKeyPair.getPrivateKey()) + "\n-----END PRIVATE KEY-----";
        rsaKeyPair.setPublicKey(publicKey);
        rsaKeyPair.setPrivateKey(privateKey);
        return rsaKeyPair;
    }

    /**
     * 格式化 Base64 字符串（每行 64 个字符）
     *
     * @param base64 Base64 字符串
     * @return 格式化后的字符串
     */
    private static String formatBase64(String base64) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < base64.length(); i += 64) {
            if (i > 0) {
                formatted.append("\n");
            }
            formatted.append(base64, i, Math.min(i + 64, base64.length()));
        }
        return formatted.toString();
    }

    /**
     * rsa公钥加密
     *
     * @param text   待加密文本
     * @param pubKey 公钥 (Base64格式符串)
     * @return 加密后的文本 (Base64格式符串)
     */
    public static String rsaEncrypt(String text, String pubKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pubKey));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] bytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    /**
     * rsa私钥解密
     *
     * @param text   待解密文本 (Base64格式符串)
     * @param priKey 私钥 (Base64格式符串)
     * @return 解密后的文本
     */
    public static String rsaDecrypt(String text, String priKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(priKey));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(text));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException(e);
        }
    }

    @Data
    @Builder
    public static class RsaKeyPair implements Serializable {
        /**
         * 公钥
         */
        private String publicKey;

        /**
         * 私钥
         */
        private String privateKey;
    }
}
