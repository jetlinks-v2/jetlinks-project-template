package org.jetlinks.pro;

import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.crud.configuration.EasyormConfiguration;
import org.jetlinks.pro.configure.doc.SpringDocCustomizerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(
    scanBasePackages = {"org.jetlinks.pro.web"},
    exclude = {
        R2dbcAutoConfiguration.class,
        SpringDocCustomizerConfiguration.class,
        EasyormConfiguration.class
    })
@EnableCaching
@EnableAopAuthorize
public class UiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UiApplication.class, args);
    }

}
