package org.jetlinks.pro.example;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.jetlinks.pro.gateway.external.SubscriptionProvider;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.service.ExampleEventHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * <pre>{@code
 * //同一个浏览器中应该只创建一次此websocket连接
 * //通过不同的订阅来获取不同的数据
 *  var ws = new WebSocket("ws://localhost:8080/messaging/{token}");
 *
 *  ws.onmessage= e=> console.log(e.data)
 *
 *  //发起订阅
 *  ws.send(JSON.stringify(
 *      {
 *           "id":"单个会话唯一ID",
 *           "topic":"/example/testid",
 *           "parameter":{"interval":"2s"},
 *           "type":"sub"
 *      }
 * );
 *
 *   //取消订阅，通常在切换页面时取消订阅
 *   ws.send(JSON.stringify(
 *     {
 *         id:"和请求的ID一致",
 *         "type":"unsub"
 *     }
 *   ))
 *
 *  }</pre>
 */
@Component
@AllArgsConstructor
public class WebsocketExample implements SubscriptionProvider {

    private final EventBus eventBus;

    private final ReactiveRepository<ExampleEntity, String> entityRepository;

    @Override
    public String id() {
        return "example";
    }

    @Override
    public String name() {
        return "Example";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/example/*"
        };
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String id = TopicUtils.getPathVariables("/example/{id}", request.getTopic())
                              .get("id");

        //定时查询
        Duration interval = request.getDuration("interval").orElse(null);
        if (null != interval) {
            return Flux
                .interval(Duration.ZERO, interval)
                .onBackpressureDrop()
                .concatMap(ignore -> entityRepository.findById(id));
        }

        //订阅事件总线来获取数据变更

        return Flux.concat(
            //查询数据库的最新数据
            entityRepository.findById(id),
            //订阅本地或者集群其他节点的保存事件
            eventBus.subscribe(
                Subscription
                    .builder()
                    .topics(ExampleEventHandler.TOPIC_CREATED)
                    //订阅者ID
                    .subscriberId("example-event-subscriber")
                    //订阅本地
                    .local()
                    //订阅集群
                    .broker()
                    .build(),
                ExampleEntity.class
            )
        );
    }
}
