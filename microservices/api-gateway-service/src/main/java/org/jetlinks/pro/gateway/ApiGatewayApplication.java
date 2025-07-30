package org.jetlinks.pro.gateway;

import org.jetlinks.pro.configure.doc.SpringDocCustomizerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude ={
    R2dbcAutoConfiguration.class,
    SpringDocCustomizerConfiguration.class
})
@EnableDiscoveryClient
@EnableCaching
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
