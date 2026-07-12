package com.snowdrift.framework.security.satoken.listener;

import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.snowdrift.framework.common.util.DesensitizeUtil;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * Token 状态监听器
 * <p>
 * 监听 Sa-Token 框架的登录、登出、被踢、被顶、封禁、续期等事件，
 * 统一处理日志记录与上下文清理。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/5/27
 * @since 1.0.0
 */
@Slf4j
public class TokenStateListener implements SaTokenListener {

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginParameter) {
        log.info("[登录事件] loginType={}, loginId={}, token={}", loginType, loginId, desensitizeToken(tokenValue));
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        log.info("[登出事件] loginType={}, loginId={}, token={}", loginType, loginId, desensitizeToken(tokenValue));
        // 清除请求线程中的安全上下文，防止残留数据被后续复用
        SecurityContextHolder.clear();
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        log.warn("[被踢下线] loginType={}, loginId={}, token={}", loginType, loginId, desensitizeToken(tokenValue));
        // 删除 Token 映射，使其立即失效
        StpUtil.getStpLogic().deleteTokenToIdMapping(tokenValue);
        SecurityContextHolder.clear();
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        log.warn("[被顶下线] loginType={}, loginId={}, token={}", loginType, loginId, desensitizeToken(tokenValue));
        // 删除 Token 映射，使其立即失效
        StpUtil.getStpLogic().deleteTokenToIdMapping(tokenValue);
        SecurityContextHolder.clear();
    }

    @Override
    public void doDisable(String loginType, Object loginId, String service, int level, long disableTime) {
        log.warn("[账号封禁] loginType={}, loginId={}, service={}, level={}, disableTime={}s",
                loginType, loginId, service, level, disableTime);
    }

    @Override
    public void doUntieDisable(String loginType, Object loginId, String service) {
        log.info("[账号解封] loginType={}, loginId={}, service={}", loginType, loginId, service);
    }

    @Override
    public void doOpenSafe(String loginType, String tokenValue, String service, long safeTime) {

    }

    @Override
    public void doCloseSafe(String loginType, String tokenValue, String service) {

    }

    @Override
    public void doCreateSession(String id) {

    }

    @Override
    public void doLogoutSession(String id) {

    }

    @Override
    public void doRenewTimeout(String loginType, Object loginId, String tokenValue, long timeout) {
        log.info("[Token 续期] loginType={}, loginId={}, token={}, newTimeout={}s",
                loginType, loginId, desensitizeToken(tokenValue), timeout);
    }

    /**
     * 对 Token 进行脱敏处理
     *
     * @param tokenValue Token 值
     * @return 脱敏后的 Token 值
     */
    private String desensitizeToken(String tokenValue) {
        return DesensitizeUtil.process(tokenValue, "([\\w-]{5})[\\w-]+([\\w-]{5})", "$1*****$2");
    }
}
