package com.snowdrift.orm.mybatisplus.interceptor;

import com.snowdrift.core.utils.ReflectUtils;
import com.snowdrift.orm.mybatisplus.anno.Desensitize;
import com.snowdrift.orm.mybatisplus.enums.DesensitizeTypeEnum;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DesensitizeInterceptor
 *
 * @author gaoye
 * @date 2025/03/20 19:34:38
 * @description 脱敏拦截器
 * @since 1.0.0
 */
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class DesensitizeInterceptor implements Interceptor {

    /**
     * 拦截器方法
     * @param invocation 拦截器
     * @return 拦截结果
     * @throws Throwable 拦截异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if (result instanceof List<?> resultList) {
            for (Object obj : resultList) {
                doDesensitize(obj);
            }
        } else if (result instanceof Map<?, ?> resultMap) {
            for (Object obj : resultMap.values()) {
                doDesensitize(obj);
            }
        } else {
            doDesensitize(result);
        }
        return result;
    }

    /**
     * 执行数据脱敏
     * @param obj 实体对象
     */
    private void doDesensitize(Object obj) throws IllegalAccessException{
        List<Field> fields = ReflectUtils.getAllFields(obj.getClass());
        for (Field field : fields) {
            if (field.isAnnotationPresent(Desensitize.class)) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (Objects.isNull(value)) {
                    continue;
                }
                Desensitize desensitize = field.getAnnotation(Desensitize.class);
                DesensitizeTypeEnum type = desensitize.value();
                String result = type.mask(value.toString());
                field.set(obj, result);
            }
        }
    }
}