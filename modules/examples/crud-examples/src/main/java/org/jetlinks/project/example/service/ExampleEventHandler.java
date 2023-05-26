package org.jetlinks.project.example.service;


import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.pro.gateway.annotation.Subscribe;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExampleEventHandler {

    private final EventBus eventBus;

    static final String TOPIC_CREATED = "/example/created";

    @EventListener
    public void handleEvent(EntitySavedEvent<ExampleEntity> event) {

        event.async(this.sendNotify(event.getEntity()));

    }

    public Mono<Void> sendNotify(List<ExampleEntity> entities) {

        log.info("sent notify:{}", entities);

        return Flux
            .fromIterable(entities)
            //发布到事件总线
            .flatMap(e -> eventBus.publish(TOPIC_CREATED, e))
            .then();
    }


    //注解方式订阅事件总线消息
    @Subscribe(TOPIC_CREATED)
    public Mono<Void> handleExampleCreated(ExampleEntity message) {
        log.info("收到注解方式订阅的消息 :{}", message);
        return Mono.empty();
    }

    //也可以编程方式进行订阅。
    private Disposable disposable;

    @PostConstruct
    public void init() {
        //通过编程方式来订阅消息

        //可通过调用 Disposable#dispose来取消订阅
        disposable =
            eventBus.subscribe(
                Subscription
                    .builder()
                    .topics(TOPIC_CREATED)
                    //订阅者ID
                    .subscriberId("example-event-handler")
                    //订阅本地
                    .local()
                    //订阅集群
                    .broker()
                    .build(),
                payload -> {
                    ExampleEntity data = (ExampleEntity) payload.decode();
                    log.info("收到编程方式订阅的消息 :{}", data);
                    return Mono.empty();
                }
            );
    }

    @PreDestroy
    public void shutdown() {
        //取消订阅
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
