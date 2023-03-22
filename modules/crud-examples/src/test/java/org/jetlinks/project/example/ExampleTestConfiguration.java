package org.jetlinks.project.example;

import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@ImportAutoConfiguration({
    CustomCodecsAutoConfiguration.class
})
@Configuration
public class ExampleTestConfiguration {
}
