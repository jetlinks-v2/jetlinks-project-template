package org.jetlinks.pro.example;

import org.hswebframework.web.cache.configuration.ReactiveCacheManagerConfiguration;
import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@ImportAutoConfiguration({
    CustomCodecsAutoConfiguration.class,
    ReactiveCacheManagerConfiguration.class
})
@Configuration
public class ExampleTestConfiguration {
}
