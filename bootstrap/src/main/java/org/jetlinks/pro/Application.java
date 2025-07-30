package org.jetlinks.pro;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.basic.configuration.EnableAopAuthorize;
import org.hswebframework.web.authorization.events.AuthorizingHandleBeforeEvent;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.logging.aop.EnableAccessLogger;
import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SpringBootApplication(scanBasePackages = {
    "org.jetlinks.pro"
}, exclude = {
    DataSourceAutoConfiguration.class,
    KafkaAutoConfiguration.class,
    RabbitAutoConfiguration.class,
    MongoReactiveAutoConfiguration.class,
})
@EnableCaching
@EnableEasyormRepository({
    "org.jetlinks.pro.**.entity"
})
@EnableAopAuthorize
@EnableAccessLogger
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }

    //admin 有全部接口权限
    @Component
    @Slf4j
    public static class AdminAllAccess {

        @EventListener
        public void handleAuthEvent(AuthorizingHandleBeforeEvent e) {
            if (e.getContext().getAuthentication().getUser().getUsername().equals("admin")) {
                e.setAllow(true);
            }
        }

        @EventListener
        public void handleAccessLogger(AccessLoggerAfterEvent event) {

            log.info("{}=>{} {}-{}", event.getLogger().getIp(), event.getLogger().getUrl(), event.getLogger().getDescribe(), event.getLogger().getAction());

        }
    }
}
