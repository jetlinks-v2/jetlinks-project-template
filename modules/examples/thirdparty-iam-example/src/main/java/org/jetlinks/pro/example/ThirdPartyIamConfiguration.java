package org.jetlinks.pro.example;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
public class ThirdPartyIamConfiguration {


    @Bean
    public ThirdPartyIamFilter thirdPartyIamFilter(WebClient.Builder builder) {
        return new ThirdPartyIamFilter(
            builder
                .clone()
                .baseUrl("http://localhost:8080")
                .build());
    }

}
