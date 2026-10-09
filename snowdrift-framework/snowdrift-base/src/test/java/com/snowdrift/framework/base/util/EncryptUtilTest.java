package com.snowdrift.framework.base.util;

import com.snowdrift.framework.base.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EncryptUtilTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 加解密工具类单元测试
 * @since 1.0.0
 */
class EncryptUtilTest {

    @Nested
    @DisplayName("摘要与 HMAC")
    class Digest {

        @Test
        @DisplayName("已知向量：md5 / sha1 / sha256 输出小写 Hex")
        void knownVectors() {
            assertThat(EncryptUtil.md5("abc")).isEqualTo("900150983cd24fb0d6963f7d28e17f72");
            assertThat(EncryptUtil.sha1("abc")).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
            assertThat(EncryptUtil.sha256("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        }

        @Test
        @DisplayName("摘要长度固定，且中文按 UTF-8 编码参与计算")
        void lengths() {
            assertThat(EncryptUtil.md5("中文")).hasSize(32);
            assertThat(EncryptUtil.sha1("中文")).hasSize(40);
            assertThat(EncryptUtil.sha256("中文")).hasSize(64);
            assertThat(EncryptUtil.sha256("中文")).isNotEqualTo(EncryptUtil.sha256("中"));
        }

        @Test
        @DisplayName("空白文本被前置校验拒绝")
        void blankText() {
            assertThatThrownBy(() -> EncryptUtil.md5(""))
                    .isInstanceOf(BizException.class).hasMessage("待加密文本不能为空");
            assertThatThrownBy(() -> EncryptUtil.sha1("  "))
                    .isInstanceOf(BizException.class).hasMessage("待加密文本不能为空");
            assertThatThrownBy(() -> EncryptUtil.sha256(null))
                    .isInstanceOf(BizException.class).hasMessage("待加密文本不能为空");
        }

        @Test
        @DisplayName("hmacSha256 返回 Base64，与 JDK Mac 独立计算结果一致")
        void hmacMatchesIndependentComputation() throws Exception {
            String text = "what do ya want for nothing?";
            String key = "Jefe";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));

            String actual = EncryptUtil.hmacSha256(text, key);
            assertThat(actual).isEqualTo(expected);
            assertThat(Base64.getDecoder().decode(actual)).hasSize(32);
        }

        @Test
        @DisplayName("hmacSha256 的密钥不能为空，不同密钥结果不同")
        void hmacKeyHandling() {
            assertThat(EncryptUtil.hmacSha256("text", "k1")).isNotEqualTo(EncryptUtil.hmacSha256("text", "k2"));

            assertThatThrownBy(() -> EncryptUtil.hmacSha256("text", " "))
                    .isInstanceOf(BizException.class).hasMessage("签名密钥不能为空");
            assertThatThrownBy(() -> EncryptUtil.hmacSha256("", "key"))
                    .isInstanceOf(BizException.class).hasMessage("待签名文本不能为空");
        }

        @Test
        @DisplayName("不支持的摘要算法抛 BizException")
        void unsupportedAlgorithm() {
            assertThatThrownBy(() -> EncryptUtil.getInstance("NOT-A-DIGEST"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不支持的摘要算法");
            assertThatThrownBy(() -> EncryptUtil.getInstance(" "))
                    .isInstanceOf(BizException.class).hasMessage("算法不能为空");
        }
    }

    @Nested
    @DisplayName("AES 密钥")
    class AesKey {

        @Test
        @DisplayName("默认生成 256 位密钥并以 Hex 输出，指定长度生成对应字节数")
        void generate() {
            assertThat(EncryptUtil.aesKey()).hasSize(64).matches("[0-9a-f]+");
            assertThat(EncryptUtil.aesKey(256).getEncoded()).hasSize(32);
            assertThat(EncryptUtil.aesKey(192).getEncoded()).hasSize(24);
            assertThat(EncryptUtil.aesKey(128).getEncoded()).hasSize(16);
        }

        @Test
        @DisplayName("密钥长度只支持 128/192/256")
        void unsupportedKeySize() {
            assertThatThrownBy(() -> EncryptUtil.aesKey(64))
                    .isInstanceOf(BizException.class).hasMessage("密钥长度仅支持128、192或256");
            assertThatThrownBy(() -> EncryptUtil.aesKey(512))
                    .isInstanceOf(BizException.class).hasMessage("密钥长度仅支持128、192或256");
        }
    }

    @Nested
    @DisplayName("AES-GCM")
    class Gcm {

        @Test
        @DisplayName("加解密往返：Hex 密钥入口与 byte[] 入口等价")
        void roundTrip() {
            SecretKey key = EncryptUtil.aesKey(256);
            String hexKey = EncryptUtil.aesKey();

            String cipher = EncryptUtil.aesGcmEncrypt("待加密内容", hexKey);
            assertThat(EncryptUtil.aesGcmDecrypt(cipher, hexKey)).isEqualTo("待加密内容");

            String cipher2 = EncryptUtil.aesGcmEncrypt("待加密内容", key.getEncoded());
            assertThat(EncryptUtil.aesGcmDecrypt(cipher2, key.getEncoded())).isEqualTo("待加密内容");
        }

        @Test
        @DisplayName("密文为 Base64(iv[12] + ciphertext + tag[16])，长度符合预期")
        void cipherLayout() {
            byte[] key = EncryptUtil.aesKey(128).getEncoded();
            byte[] raw = Base64.getDecoder().decode(EncryptUtil.aesGcmEncrypt("abc", key));

            // 12 字节 IV + 3 字节明文 + 16 字节认证标签（GCM 是流模式，密文长度等于明文长度）
            assertThat(raw).hasSize(12 + 3 + 16);
        }

        @Test
        @DisplayName("每次加密使用随机 IV，相同明文得到不同密文且均可解密")
        void randomIvPerCall() {
            byte[] key = EncryptUtil.aesKey(256).getEncoded();
            Set<String> ciphers = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                String cipher = EncryptUtil.aesGcmEncrypt("same", key);
                assertThat(EncryptUtil.aesGcmDecrypt(cipher, key)).isEqualTo("same");
                ciphers.add(cipher);
            }
            assertThat(ciphers).hasSize(20);
        }

        @Test
        @DisplayName("密文被篡改时解密失败（GCM 认证标签生效）")
        void tamperedCipherFails() {
            byte[] key = EncryptUtil.aesKey(256).getEncoded();
            byte[] raw = Base64.getDecoder().decode(EncryptUtil.aesGcmEncrypt("secret", key));
            raw[raw.length - 1] ^= 0x01;
            String tampered = Base64.getEncoder().encodeToString(raw);

            assertThatThrownBy(() -> EncryptUtil.aesGcmDecrypt(tampered, key))
                    .isInstanceOf(BizException.class).hasMessage("AES-GCM解密失败");
        }

        @Test
        @DisplayName("密文过短、Base64 非法、密钥错误均失败")
        void invalidCipherInputs() {
            byte[] key = EncryptUtil.aesKey(256).getEncoded();

            assertThatThrownBy(() -> EncryptUtil.aesGcmDecrypt(Base64.getEncoder().encodeToString(new byte[10]), key))
                    .isInstanceOf(BizException.class).hasMessage("密文数据长度不足，无法解密");

            assertThatThrownBy(() -> EncryptUtil.aesGcmDecrypt("not-base64!!", key))
                    .isInstanceOf(BizException.class).hasMessage("AES-GCM解密失败");

            String cipher = EncryptUtil.aesGcmEncrypt("x", key);
            byte[] wrongKey = EncryptUtil.aesKey(256).getEncoded();
            assertThatThrownBy(() -> EncryptUtil.aesGcmDecrypt(cipher, wrongKey))
                    .isInstanceOf(BizException.class).hasMessage("AES-GCM解密失败");
        }

        @Test
        @DisplayName("密钥为空、长度非法或 Hex 串非法均抛 BizException")
        void invalidKeys() {
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", (byte[]) null))
                    .isInstanceOf(BizException.class).hasMessage("密钥不能为空");
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", new byte[0]))
                    .isInstanceOf(BizException.class).hasMessage("密钥不能为空");
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", new byte[15]))
                    .isInstanceOf(BizException.class).hasMessage("AES-GCM加密失败");

            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", "zz"))
                    .isInstanceOf(BizException.class).hasMessage("密钥不是合法的十六进制字符串");
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", "abc"))
                    .isInstanceOf(BizException.class).hasMessage("密钥不是合法的十六进制字符串");
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("x", " "))
                    .isInstanceOf(BizException.class).hasMessage("密钥不能为空");
        }

        @Test
        @DisplayName("空明文被前置校验拒绝")
        void blankPlainText() {
            assertThatThrownBy(() -> EncryptUtil.aesGcmEncrypt("", EncryptUtil.aesKey()))
                    .isInstanceOf(BizException.class).hasMessage("待加密文本不能为空");
        }
    }

    @Nested
    @DisplayName("AES-ECB（废弃兼容路径）")
    class Ecb {

        @Test
        @DisplayName("加解密往返，确定性输出（这也是它被废弃的原因）")
        @SuppressWarnings("deprecation")
        void roundTrip() {
            String key = EncryptUtil.aesKey();

            String cipher = EncryptUtil.aesEcbEncrypt("hello", key);
            assertThat(EncryptUtil.aesEcbDecrypt(cipher, key)).isEqualTo("hello");
            assertThat(EncryptUtil.aesEcbEncrypt("hello", key)).isEqualTo(cipher);
        }

        @Test
        @DisplayName("非法密文与非法密钥均抛 BizException")
        @SuppressWarnings("deprecation")
        void invalidInputs() {
            assertThatThrownBy(() -> EncryptUtil.aesEcbDecrypt("not-base64!!", EncryptUtil.aesKey()))
                    .isInstanceOf(BizException.class).hasMessage("AES-ECB解密失败");
            assertThatThrownBy(() -> EncryptUtil.aesEcbEncrypt("x", "not-hex"))
                    .isInstanceOf(BizException.class).hasMessage("密钥不是合法的十六进制字符串");
        }
    }

    @Nested
    @DisplayName("RSA")
    class Rsa {

        @Test
        @DisplayName("密钥长度只支持 2048/3072/4096，默认 2048")
        void keyGeneration() {
            KeyPair keyPair = EncryptUtil.rsaKeyPair();
            assertThat(((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength()).isEqualTo(2048);
            assertThat(((RSAPrivateKey) keyPair.getPrivate()).getModulus().bitLength()).isEqualTo(2048);

            // 3072 位生成较慢，这里只验证长度参数被接受；更长的 4096 位同理
            assertThat(((RSAPublicKey) EncryptUtil.rsaKeyPair(3072).getPublic()).getModulus().bitLength())
                    .isEqualTo(3072);

            assertThatThrownBy(() -> EncryptUtil.rsaKeyPair(1024))
                    .isInstanceOf(BizException.class)
                    .hasMessage("新生成的 RSA 密钥长度仅支持2048、3072或4096");
        }

        @Test
        @DisplayName("Base64 密钥对可直接用于加解密往返")
        void base64RoundTrip() {
            EncryptUtil.RsaKeyPair pair = EncryptUtil.rsaKeyPairBase64();
            String cipher = EncryptUtil.rsaEncrypt("待加密内容", pair.getPublicKey());

            assertThat(EncryptUtil.rsaDecrypt(cipher, pair.getPrivateKey())).isEqualTo("待加密内容");
        }

        @Test
        @DisplayName("PKCS#1 v1.5 单次明文上限为 密钥字节数 - 11")
        void plainTextLengthLimit() {
            EncryptUtil.RsaKeyPair pair = EncryptUtil.rsaKeyPairBase64();

            assertThat(EncryptUtil.rsaEncrypt("a".repeat(245), pair.getPublicKey())).isNotBlank();
            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("a".repeat(246), pair.getPublicKey()))
                    .isInstanceOf(BizException.class)
                    .hasMessage("RSA 明文过长，当前密钥单次最大支持 245 字节");
        }

        @Test
        @DisplayName("PEM 输出仅用于交换，直接传给裸 Base64 入参会失败（已知用法约束）")
        void pemOutputIsNotAcceptedByRawEntryPoints() {
            EncryptUtil.RsaKeyPair pem = EncryptUtil.rsaKeyPairPem();

            assertThat(pem.getPublicKey())
                    .startsWith("-----BEGIN PUBLIC KEY-----")
                    .contains("\n")
                    .endsWith("-----END PUBLIC KEY-----");
            assertThat(pem.getPrivateKey()).startsWith("-----BEGIN PRIVATE KEY-----");

            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("x", pem.getPublicKey()))
                    .isInstanceOf(BizException.class).hasMessage("RSA加密失败");
        }

        @Test
        @DisplayName("非法密钥/密文与空入参均抛 BizException")
        void invalidInputs() {
            EncryptUtil.RsaKeyPair pair = EncryptUtil.rsaKeyPairBase64();

            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("x", "not-base64"))
                    .isInstanceOf(BizException.class).hasMessage("RSA加密失败");
            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("x", pair.getPrivateKey()))
                    .isInstanceOf(BizException.class).hasMessage("RSA加密失败");
            assertThatThrownBy(() -> EncryptUtil.rsaDecrypt("not-base64", pair.getPrivateKey()))
                    .isInstanceOf(BizException.class).hasMessage("RSA解密失败");

            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("", pair.getPublicKey()))
                    .isInstanceOf(BizException.class).hasMessage("待加密文本不能为空");
            assertThatThrownBy(() -> EncryptUtil.rsaEncrypt("x", " "))
                    .isInstanceOf(BizException.class).hasMessage("公钥不能为空");
            assertThatThrownBy(() -> EncryptUtil.rsaDecrypt("x", " "))
                    .isInstanceOf(BizException.class).hasMessage("私钥不能为空");
        }

        @ParameterizedTest
        @ValueSource(ints = {2048, 3072})
        @DisplayName("不同密钥长度均可完成往返")
        void roundTripForKeySizes(int keySize) {
            KeyPair keyPair = EncryptUtil.rsaKeyPair(keySize);
            String pub = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String pri = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            assertThat(EncryptUtil.rsaDecrypt(EncryptUtil.rsaEncrypt("payload", pub), pri)).isEqualTo("payload");
        }
    }
}
