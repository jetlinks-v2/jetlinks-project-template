package org.jetlinks.project.example.controller;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.web.ResponseMessage;
import org.jetlinks.pro.microservice.ServicesRequesterManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/example/webclient")
@AllArgsConstructor
public class WebClientExampleController {

    private final ServicesRequesterManager requesterManager;

    //  http://localhost:8800/example/webclient/test-me?:X_Access_Token={token}
    @GetMapping("/test-me")
    public Mono<ResponseMessage<Map<String, Object>>> webclient() {
        return requesterManager
            .getWebClient("authentication-service")
            .flatMap(client -> client
                .get()
                .uri("/authorize/me")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ResponseMessage<Map<String, Object>>>() {
                }));
    }
}
