package org.jetlinks.pro.example;

import io.scalecube.services.annotations.Service;
import io.scalecube.services.annotations.ServiceMethod;
import reactor.core.publisher.Mono;

@Service
public interface ExampleRpcApi {

    @ServiceMethod
    Mono<String> hello(String name);

}
