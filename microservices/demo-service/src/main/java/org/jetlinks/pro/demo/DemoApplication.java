package org.jetlinks.pro.demo;

import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 应用示例的独立响应式启动入口，只装配示例业务及平台公共运行时。
 * 通过平台服务发现加入既有集群，不在本进程重新部署认证或网关业务。
 */
@SpringBootApplication(scanBasePackages = "org.jetlinks.pro", exclude = DataSourceAutoConfiguration.class)
@EnableCaching
@EnableAopAuthorize
@EnableEasyormRepository("org.jetlinks.pro.**.entity")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
