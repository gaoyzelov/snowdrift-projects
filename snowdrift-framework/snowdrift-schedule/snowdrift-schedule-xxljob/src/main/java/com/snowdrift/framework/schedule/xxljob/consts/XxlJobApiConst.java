package com.snowdrift.framework.schedule.xxljob.consts;

/**
 * XXL-JOB Admin API 路径常量
 * <p>
 * 对照 XXL-JOB Admin {@code JobInfoController} / {@code JobGroupController} 源码
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/15
 * @since 1.0.0
 */
public final class XxlJobApiConst {

    /**
     * 登录
     */
    public static final String LOGIN_PATH = "/auth/doLogin";
    /** 分页查询 */
    public static final String JOB_PAGE_PATH = "/jobinfo/pageList";
    /** 新增 */
    public static final String JOB_INSERT_PATH = "/jobinfo/insert";
    /** 更新 */
    public static final String JOB_UPDATE_PATH = "/jobinfo/update";
    /** 删除 */
    public static final String JOB_DELETE_PATH = "/jobinfo/delete";
    /** 暂停 */
    public static final String JOB_STOP_PATH = "/jobinfo/stop";
    /** 恢复 */
    public static final String JOB_START_PATH = "/jobinfo/start";
    /** 手动触发 */
    public static final String JOB_TRIGGER_PATH = "/jobinfo/trigger";

    /** 执行器分组分页 */
    public static final String GROUP_PAGE_PATH = "/jobgroup/pageList";

    private XxlJobApiConst() {
    }
}
