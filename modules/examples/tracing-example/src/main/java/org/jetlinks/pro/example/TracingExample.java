package org.jetlinks.pro.example;

import io.opentelemetry.api.common.AttributeKey;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TracingExample {

    static final AttributeKey<Long> entities = AttributeKey.longKey("entities");

    @EventListener
    public void handleEvent(EntitySavedEvent<ExampleEntity> event) {

        event.async(
            doSomething(event.getEntity())
                //链路埋点
                .as(MonoTracer.create(
                    "/_example/eventHandler",
                    builder -> builder.setAttribute(entities, (long) event.getEntity().size())))
        );

    }

    private Mono<Void> doSomething(ExampleEntity e) {
        //随机延迟1-500ms，模拟业务处理耗时
        return Mono
            .delay(Duration.ofMillis(ThreadLocalRandom.current().nextInt(1, 500)))
            .then();
    }

    private Mono<Void> doSomething(List<ExampleEntity> entities) {
        return Flux
            .fromIterable(entities)
            .flatMap(this::doSomething)
            .then();
    }

}
