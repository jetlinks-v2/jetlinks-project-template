package org.jetlinks.pro;

import org.hswebframework.web.id.IDGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notify/test")
public class TestController {


    @GetMapping
    public Mono<String> test(){
        return Mono.just(IDGenerator.RANDOM.generate());
    }
}
