package org.jetlinks.pro;


import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {"org.jetlinks.pro"})
@EnableCaching
@EnableAopAuthorize
@EnableEasyormRepository("org.jetlinks.pro.**.entity")
public class MvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MvcApplication.class, args);
    }

}
