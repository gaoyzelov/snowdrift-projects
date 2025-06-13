package com.snowdrift.pay.allin.properties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

/**
 * AllinSybProperties
 *
 * @author gaoye
 * @date 2025/05/20 19:46:21
 * @description xxxxxxxx
 * @since 1.0
 */
@Data
@Validated
@ConfigurationProperties(prefix = "pay.allin.syb")
public class AllinSybProperties implements Serializable {

    @Schema(title = "是否启用")
    @NotNull(message = "必须指定是否启用")
    private Boolean enabled = Boolean.FALSE;

    @Schema(title = "集团商户号,集团模式下不能为空")
    private String orgId;

    @Schema(title = "应用ID")
    @NotBlank(message = "APPID不能为空")
    private String appId = "00353766";

    @Schema(title = "商户号")
    @NotBlank(message = "商户号不能为空")
    private String cusId = "56459504816ASJ2";

    @Schema(title = "MD5密钥")
    private String md5key = "sk_2a0b2c5d0e5f2a0z";

    @Schema(title = "RSA私钥")
    private String rsaPriKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDZUaAOVyOBQNm6/uaMuYZ1ZY/59AeBnJRPSk5fYXSN4hC/k8NR2v9CI8LyZ9xdPdYSheW+Sf5ocXBCU560wJopFv5DRjYhKoKdcn9Om7T65UTWDoQ9/C4okJc5OwycK71XFDuNS8Gbh63+8obfeWa5TpMBYPYg5TC0pUZZO4wxilFjULO4YE4qGlllEo6MYoAOfbyyX/drDvQNpQIhKnkXRK08wowj4BkZ8P+1iJ7wyR1O7RnPMjNFHtKa6JMsL6MuYzfN0rdb3y8e8QozxcUEg/S1z9NosDCcZOi7Zn1iFGl6fz2+G0SZcTvC5vO8E6KC9iVPxQboDuunqJveU8YBAgMBAAECggEAZtYp8+9HRWBPSB5cMgaO+pnDbmgMb0iNpBHJEXiYA5YRD98YA/Gqbf1nQHdmf5cF6MEE7S2tnnXjxfc0+FrV+JDBZEtizQIFSgVG+gdTtpBsqRC4gm6F1ztI3FO1jWdjU0QnRB8vsG4dp7HTogecVR86EGJ2Ri2+hRVau1oi5sOqdOoug0xycGIl2wggvhBca6zeASwygJF8FjF9NUppY/A6eAUOC84P5AOcftPy6fg3a54l8LOzPhuwLIQURAESgd7ViItbfb+kD3fXaZ3qSZXxUmCz5MhHypU41T7iFqXjLumjdzMG9zAUTxKGUBntkFbJXrtViTV6SZAgIgV3ewKBgQD2/fk+hEDejP/XsR9I0DvtOYbLbENBj3WQabMmn+ERcstkQoINHMrDhuLNlrQ1pW0dBHsnKbyLgXibXbmNxEvbFRgbwSkKeAdgCx7xqxg8IAb/6oP+ZK6oi91X2T3l0USolPU0dXGla1QoT+eiKbTo75KFeYNdCrjrQxmNqCD4QwKBgQDhPpv5BP8w52MfIKUIINdR6YdnKu15r9UqI/zKqb4BcRuR34GjTeQ94wgM0EttFHCF+Cg+tOpZTwblpLRIgxLuvmsW1m0LRQh+JrZjyd3v6m8KHQv6vnmTTOaESXb9GDOoaA94kCpIar14OuMb6Sqk+D8rcvqu14MZ788FFKzWawKBgHDmdPj7sT9JimUIxLke3lW4lwIDcdbVAbNKbaa5LEMyiwBAWnwt7g1kaeX5/lQLUw4Rj02+iT3np761H+1KjjYNtWBychUj+pMPYkyrnb9WEH0IsN8nHKNUk4/lcKLNfqnktRUOni00G1r183avw886O2qhucdc8Fwx5stW0ANLAoGBAMfYiASadVzLf7uneCVNFh38KAHWKZs08dmQ0oJxtj6LP13ew6PDwUVTgNn1mWZvoyKKOhLg66hPoJvx1W8ctJiU5Gj/2QINvupGqL7C5aIvC1Qaz30qsK2Lpl8q22LVrUe3BDsBFjLoWbEp9kTWTNrP0yRWwKqOViiWB8THQk5/AoGBANLfKakTha0u7c+KIIU2Z2oV3FGXUBmaOo2AjVga3GQ1u4I+m6G+OYXJWSEBvDKlMgrP3SyFfeR2LZkuGWXsUowPQlqdb3/BoXORH3vZMZJeer+IjgGPvRzCQ9VpdSFypWHgy2aBbVaQa5sRT15t63pT1JV06wecDbQQv4r50GFH";

    @Schema(title = "验签公钥")
    private String tlsPubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCm9OV6zH5DYH/ZnAVYHscEELdCNfNTHGuBv1nYYEY9FrOzE0/4kLl9f7Y9dkWHlc2ocDwbrFSm0Vqz0q2rJPxXUYBCQl5yW3jzuKSXif7q1yOwkFVtJXvuhf5WRy+1X5FOFoMvS7538No0RpnLzmNi3ktmiqmhpcY/1pmt20FHQQIDAQAB";

}