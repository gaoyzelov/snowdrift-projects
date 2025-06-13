package com.snowdrift.core.config;

import com.snowdrift.core.aspect.VerifyAspect;
import com.snowdrift.core.utils.SpringUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * SnowdriftAutoConfig
 *
 * @author gaoye
 * @date 2025/06/13 15:12:04
 * @description xxxxxxxx
 * @since 1.0
 */
@Configuration
@Import({VerifyAspect.class, SpringUtil.class})
public class SnowdriftAutoConfig { }