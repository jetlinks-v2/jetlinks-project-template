package org.jetlinks.project.auth;

import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.logging.aop.EnableAccessLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(
    scanBasePackages = {"org.jetlinks.pro","org.jetlinks.project"},
    exclude = {
        DataSourceAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class
    })
@EnableCaching
@EnableEasyormRepository({
    "org.jetlinks.pro.**.entity",
    "org.jetlinks.project.**.entity"
})
@EnableAopAuthorize
@EnableAccessLogger
public class AuthenticationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationApplication.class, args);
    }

}
