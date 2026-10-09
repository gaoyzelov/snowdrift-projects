package com.snowdrift.framework.log.aspect;

import com.snowdrift.framework.base.result.Result;
import com.snowdrift.framework.log.ILogService;
import com.snowdrift.framework.log.annotation.LoginLog;
import com.snowdrift.framework.log.holder.ApiLogHolder;
import com.snowdrift.framework.log.holder.LoginLogHolder;
import com.snowdrift.framework.log.holder.OperateLogHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoginLogAspectIntegrationTest
 *
 * @author gaoyzelov
 * @date 2026/10/9
 * @description 登录日志切面端到端测试：走真实 AspectJ 代理，验证形参名可用于提取登录账号
 * @since 1.0.0
 */
class LoginLogAspectIntegrationTest {

    public static class LoginDTO {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public static class LoginController {

        @LoginLog
        public Result<String> loginByDto(LoginDTO dto) {
            return Result.ok("token");
        }

        /** 常见写法：裸参数登录，账号只能靠形参名定位 */
        @LoginLog
        public Result<String> loginByScalar(String username, String password) {
            return Result.ok("token");
        }

        @LoginLog(accountField = "accountName")
        public Result<String> loginWithCustomField(String accountName, String password) {
            return Result.ok("token");
        }

        @LoginLog
        public Result<String> loginFailing(String username, String password) {
            throw new IllegalStateException("密码错误");
        }
    }

    static final List<LoginLogHolder> SAVED = new ArrayList<>();

    static final ILogService CAPTURE = new ILogService() {
        @Override
        public void saveApiLog(ApiLogHolder holder) {
        }

        @Override
        public void saveLoginLog(LoginLogHolder holder) {
            SAVED.add(holder);
        }

        @Override
        public void saveOperateLog(OperateLogHolder holder) {
        }
    };

    @Configuration
    @EnableAspectJAutoProxy
    static class Config {
        @Bean
        ILogService logService() {
            return CAPTURE;
        }

        @Bean
        LoginLogAspect loginLogAspect(ILogService logService) {
            return new LoginLogAspect(logService);
        }

        @Bean
        LoginController loginController() {
            return new LoginController();
        }
    }

    @AfterEach
    void clear() {
        SAVED.clear();
    }

    private static void withController(java.util.function.Consumer<LoginController> action) {
        new ApplicationContextRunner().withUserConfiguration(Config.class).run(context -> {
            assertThat(context).hasNotFailed();
            action.accept(context.getBean(LoginController.class));
        });
    }

    @Test
    @DisplayName("DTO 入参：按字段名提取账号")
    void extractsFromDto() {
        withController(controller -> {
            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            controller.loginByDto(dto);

            assertThat(SAVED).hasSize(1);
            assertThat(SAVED.get(0).getUsername()).isEqualTo("admin");
            assertThat(SAVED.get(0).getStatus()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("裸参数入参：按真实方法形参名提取账号（无需 -parameters 之外的额外配置）")
    void extractsFromScalarParameterName() {
        withController(controller -> {
            controller.loginByScalar("root", "P@ssw0rd!");

            assertThat(SAVED).hasSize(1);
            assertThat(SAVED.get(0).getUsername()).isEqualTo("root");
            assertThat(SAVED.get(0).getStatus()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("自定义 accountField 同样支持裸参数（按形参名匹配）")
    void extractsWithCustomAccountField() {
        withController(controller -> {
            controller.loginWithCustomField("alice", "pw");

            assertThat(SAVED).hasSize(1);
            assertThat(SAVED.get(0).getUsername()).isEqualTo("alice");
        });
    }

    @Test
    @DisplayName("异常路径经真实代理同样记录，账号仍可按形参名提取")
    void recordsFailureThroughRealProxy() {
        withController(controller -> {
            assertThatThrownBy(() -> controller.loginFailing("root", "pw"))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(SAVED).hasSize(1);
            LoginLogHolder holder = SAVED.get(0);
            assertThat(holder.getStatus()).isEqualTo(0);
            assertThat(holder.getMsg()).isEqualTo("密码错误");
            assertThat(holder.getUsername()).isEqualTo("root");
        });
    }
}
