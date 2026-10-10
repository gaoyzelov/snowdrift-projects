package com.snowdrift.framework.schedule.xxljob.consts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XxlJobApiConstTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 锁定 XXL-JOB Admin API 路径常量：这些是与 Admin 端约定的线路契约，变更会导致所有远程操作失效
 * @since 1.0.0
 */
class XxlJobApiConstTest {

    @Test
    @DisplayName("各 API 路径与 Admin JobInfoController/JobGroupController 约定一致")
    void apiPathsAreStable() {
        assertThat(XxlJobApiConst.LOGIN_PATH).isEqualTo("/auth/doLogin");
        assertThat(XxlJobApiConst.JOB_PAGE_PATH).isEqualTo("/jobinfo/pageList");
        assertThat(XxlJobApiConst.JOB_INSERT_PATH).isEqualTo("/jobinfo/insert");
        assertThat(XxlJobApiConst.JOB_UPDATE_PATH).isEqualTo("/jobinfo/update");
        assertThat(XxlJobApiConst.JOB_DELETE_PATH).isEqualTo("/jobinfo/delete");
        assertThat(XxlJobApiConst.JOB_STOP_PATH).isEqualTo("/jobinfo/stop");
        assertThat(XxlJobApiConst.JOB_START_PATH).isEqualTo("/jobinfo/start");
        assertThat(XxlJobApiConst.JOB_TRIGGER_PATH).isEqualTo("/jobinfo/trigger");
        assertThat(XxlJobApiConst.GROUP_PAGE_PATH).isEqualTo("/jobgroup/pageList");
    }
}
