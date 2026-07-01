package com.snowdrift.framework.orm.mp.properties;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * 分页配置属性
 * <p>配置前缀：{@code snowdrift.orm.mp.pagination}</p>
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:27
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "snowdrift.orm.mp.pagination")
public class OrmMpPaginationProperties implements Serializable {

    /**
     * 数据库类型（如 MYSQL、POSTGRE_SQL、ORACLE 等）
     */
    private DbType dbType = DbType.MYSQL;

    /**
     * 单页最大条数限制（超过限制自动修正为最大值）
     */
    private Long maxLimit = 1000L;

    /**
     * 页码溢出处理：{@code true} 超出最大页数时返回首页，{@code false} 继续请求
     */
    private Boolean overflow = true;

    /**
     * 是否优化 count SQL 中的 LEFT JOIN（仅支持 left join）
     */
    private Boolean optimizeJoin = true;
}
