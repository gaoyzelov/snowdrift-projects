package com.snowdrift.pay.yee.properties;

import com.yeepay.yop.sdk.security.CertTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * YeeProperties
 *
 * @author gaoye
 * @date 2025/06/05 17:22:40
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "pay.yee")
public class YeeProperties implements Serializable {

    private Boolean enabled = Boolean.FALSE;

    private String serverRoot = "https://sandbox.yeepay.com/yop-center";

    private String appKey = "app_10088763205";

    private String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDEfLZ9Bu8tSLWNvJYBmXNCx7KWbpr2jWVq2kZveHX9DPFSjef8iSovooi9Rhu1mlbgi5aIjZoeOn5McF3yZfgXX2mGBLgSWLA9CRWmkkxBQqfN3iLbKA3LoYJloaf9v0CHJGFZt+WrTl4UGfBG4j9sepfJQGISmSUxXP6s+7lh3VW9WZkr25tv5iKYl0FvxrgEOUYuuLree4jUuIxdu8in4VXVXxekvafsVwwM+cdot5bLCd+f03KqnOkxCCroMmkJszsqAj+Rliu+MktD3rebjEtraCjQB94lOtNfyEzxJkYk45bXkiDKy/35aAj6wttfSU6/EfKL4s9UNGyQrK89AgMBAAECggEADR40Ex/9JGVpZPDYBVEmH5Qptr7kKX1HrdesTQFlGTOobJCiUhsjPfe95YBn1bBGnWpcWWS2IHoFwsXcE9SdzgurRcy3WwGLpKDgY+pPsn9dfiesOpGk7XksiutScKUN4mfkovgy2m9v5at/U0WEIqfYTedL6j4fZvlfMb6C7NxbcFzPZAsm0FYvXlClH4UqITyKA9q4XF2u9dxaTAqOQKeATT/IPgubFx8kdhB5PS0jZnkqnpbbnXeMzCuR7BRww2bngA/DaypKXRmypQuIE5EQ1/jrYFYk83+edk2ma2LZjfaqV2lwU8/Q1A2paypFQsDwyV1M8Kv/sRWkNWNrgQKBgQDN7LyTGijdZo1uy+454jIaRXpDCYVv+obIDosNHngGSkbTOZZXsIFy1O819p2i/MEujySa6Zwj52o5pMn8VlvvI/p3vqnzLwiwxL/nZgCvY7lAU5llLYwXxavGLvVqwRDqGzd2zhSh04IhKBRR76qplrOyqCK+++3fW/MtN+5BgQKBgQD0RHcu2ThetP3bBlCgX34y1gISq8VxyLF7qHsZgbW/DDA/Dinu7BjDPppC1UwUGUmCajsG6V+c8zvWLNFsZlW45+QxOkAcbndpQK129d61JQKo+L5gQJ8gZ6r7Y5zNt6ggkdkotNsIUyxHOTM3Dk+pAhxvkixKg5S1UdOsOEnTvQKBgAyT8DS//U0ArZTd6fApK8xFACHGHBtp3v+rO70SZlxj/w150AANLOr5rJ7MMHa7H4Wbgq3fpQTgZrGcqiW6lKT3up3DzJepAMvjSKF2roYH/lG6iX0PmiX9ke9qAN2Da2gRP4MHNWVpCOLDkvIqRG9VYCaCdJTbyHzDHgheCTSBAoGAQedfCKLO1K6cd0Wi50IceHbJJGR35xUw3Z49aEpY0Ozm73xQ0AefPr5bPIDz/9A6xxcGSf0ZMO1nTYfWERsjMbpPFQEUXxdm3Fz48EmyBsmviAtkwe9tf3644Q6lNdPBZtgy8QqqtfpKzgSeztHrqDvLlEap8IjDzJABPkwJjzkCgYBGgubnSkMeETuL+OjUoYdM4GPWWL4+vKHP81Nuakw385QLN2ZpjHJinRd7h21h5+qsUCbuZSJz5ZJR28aEA9ue44YzHHUPCH6vsi5+rNJQF9Y6qH+vsPhH48g+j0MW0VDvYXbTGTQLhcdIKIYSwvdcsWFFZPliUY6H/RT38h8JIg==";

    private String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6p0XWjscY+gsyqKRhw9MeLsEmhFdBRhT2emOck/F1Omw38ZWhJxh9kDfs5HzFJMrVozgU+SJFDONxs8UB0wMILKRmqfLcfClG9MyCNuJkkfm0HFQv1hRGdOvZPXj3Bckuwa7FrEXBRYUhK7vJ40afumspthmse6bs6mZxNn/mALZ2X07uznOrrc2rk41Y2HftduxZw6T4EmtWuN2x4CZ8gwSyPAW5ZzZJLQ6tZDojBK4GZTAGhnn3bg5bBsBlw2+FLkCQBuDsJVsFPiGh/b6K/+zGTvWyUcu+LUj2MejYQELDO3i2vQXVDk7lVi2/TcUYefvIcssnzsfCfjaorxsuwIDAQAB";

    private CertTypeEnum certType = CertTypeEnum.RSA2048;

    private String parentMerchantNo = "10088763205";

    private String merchantNo = "10089033171";

    private String payNotifyUrl = "";

    private String refundNotifyUrl = "";

    private String redirectUrl = "";

}