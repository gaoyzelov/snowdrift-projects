package com.snowdrift.framework.orm.core.scope;

import java.util.List;

/**
 *
 * @author gaoyzelov
 * @date 2026/7/2-16:50
 * @description
 * @since 1.0.0
 */
public interface IDataScopeProvider {

    /**
     * 根据用户ID获取自定义部门ID列表
     *
     * @param userId 用户ID
     * @return 自定义部门ID列表
     */
    List<Long> getCustomDeptIds(Long userId);

    /**
     * 根据部门ID获取子部门ID列表(包含当前部门)
     *
     * @param deptId 部门ID
     * @return 子部门ID列表
     */
    List<Long> getChildDeptIds(Long deptId);
}
