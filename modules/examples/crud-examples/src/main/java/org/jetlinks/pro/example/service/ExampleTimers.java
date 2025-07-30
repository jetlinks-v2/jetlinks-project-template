package org.jetlinks.pro.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.pro.cluster.reactor.FluxCluster;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExampleTimers implements CommandLineRunner {

    private final ExampleService service;


    //删除指定时间前未修改过的数据
    private Mono<Integer> delete(Duration time) {
        return service
            .createDelete()
            .where()
            .lt(ExampleEntity::getModifyTime, System.currentTimeMillis() - time.toMillis())
            .execute();
    }

    private Disposable disposable;

    @PreDestroy
    public void shutdown() {
        //停止定时任务
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        disposable =
            FluxCluster
                //不同的任务名不能相同
                .schedule("example-test", Duration.ofSeconds(30), Mono.defer(() -> this.delete(Duration.ofDays(1))));
    }
}
