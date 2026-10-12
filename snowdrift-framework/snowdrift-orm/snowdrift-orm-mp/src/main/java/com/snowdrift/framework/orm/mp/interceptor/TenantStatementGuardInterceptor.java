package com.snowdrift.framework.orm.mp.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.orm.mp.handler.MultiTenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * TenantStatementGuardInterceptor
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 多租户语句守卫：拒绝 MyBatis-Plus 无法注入租户条件的语句
 * @since 1.0.0
 */
@Slf4j
public class TenantStatementGuardInterceptor implements InnerInterceptor {

    /**
     * MyBatis-Plus 的 {@code JsqlParserSupport} 只分派 Select / Insert / Update / Delete 四类语句，
     * 其余语句（如 REPLACE 解析出的 Upsert、MERGE）会被 <b>原样返回且不报错</b>，
     * 既不注入租户条件也不抛异常——租户隔离因此静默失效。
     * <p>这里做一次廉价的文本预筛：命中四类关键字就跳过后续解析，避免给每条 SQL 增加一次 JSqlParser 解析开销。</p>
     */
    private static final Pattern SUPPORTED_LEADING_KEYWORD =
            Pattern.compile("^\\s*\\(*\\s*(SELECT|INSERT|UPDATE|DELETE)\\b", Pattern.CASE_INSENSITIVE);

    private final MultiTenantLineHandler tenantLineHandler;

    public TenantStatementGuardInterceptor(MultiTenantLineHandler tenantLineHandler) {
        this.tenantLineHandler = tenantLineHandler;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        checkSql(boundSql == null ? null : boundSql.getSql());
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        BoundSql boundSql = PluginUtils.mpStatementHandler(sh).boundSql();
        checkSql(boundSql == null ? null : boundSql.getSql());
    }

    /**
     * 校验语句是否可能绕过租户注入，命中则抛 {@link BizException} 阻断执行
     *
     * @param sql 待执行的 SQL，可为 null / 空白
     * @throws BizException 语句类型不受支持且涉及未被忽略的表时抛出
     */
    void checkSql(String sql) {
        if (StringUtils.isBlank(sql) || SUPPORTED_LEADING_KEYWORD.matcher(sql).find()) {
            return;
        }
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            // 解析不了时由 MyBatis-Plus 自身的解析路径决定行为（同样会失败并抛出），此处不改变语义
            log.debug("多租户守卫跳过无法解析的 SQL: {}", e.getMessage());
            return;
        }
        if (isSupported(statement)) {
            return;
        }
        if (allTablesIgnored(statement)) {
            return;
        }
        String keyword = leadingKeyword(sql);
        throw new BizException("多租户模式下不支持的语句类型 [" + keyword + "]：MyBatis-Plus 无法为其注入租户条件，"
                + "继续执行会导致跨租户数据访问。请改用 INSERT/UPDATE/DELETE，"
                + "或将该语句涉及的表加入 snowdrift.orm.mp.tenant.ignore-tables");
    }

    private boolean isSupported(Statement statement) {
        return statement instanceof Select
                || statement instanceof Insert
                || statement instanceof Update
                || statement instanceof Delete;
    }

    /**
     * 判断语句涉及的表是否全部在忽略清单中
     * <p>表清单为空（解析器无法枚举）时返回 {@code false}，即按“未确认安全”处理，属于 fail-closed。</p>
     */
    private boolean allTablesIgnored(Statement statement) {
        List<String> tables;
        try {
            tables = new TablesNamesFinder().getTableList(statement);
        } catch (Exception e) {
            return false;
        }
        if (CollectionUtils.isEmpty(tables)) {
            return false;
        }
        return tables.stream().allMatch(tenantLineHandler::ignoreTable);
    }

    private String leadingKeyword(String sql) {
        String trimmed = sql.trim();
        int end = 0;
        while (end < trimmed.length()
                && !Character.isWhitespace(trimmed.charAt(end))
                && trimmed.charAt(end) != '(') {
            end++;
        }
        return trimmed.substring(0, end).toUpperCase(Locale.ROOT);
    }
}
