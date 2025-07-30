package org.jetlinks.pro.example.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.jetlinks.pro.example.enums.ExampleEnum;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author zhouhao
 * @see GenericReactiveCrudService#createQuery()
 * @see GenericReactiveCrudService#createUpdate()
 * @see GenericReactiveCrudService#createDelete()
 * @since 1.0
 */
@Service
public class ExampleService extends GenericReactiveCrudService<ExampleEntity, String> {


    public Flux<ExampleEntity> findByName(String name) {
        return createQuery()
            .where(ExampleEntity::getName, Objects.requireNonNull(name))
            .fetch();
    }

    public Mono<Integer> updateStatus(String id, ExampleEnum status) {
        return createUpdate()
            .set(ExampleEntity::getSingleEnum, status)
            .where(ExampleEntity::getId, status)
            .execute();
    }

    public Mono<Integer> deleteByStatus(ExampleEnum status) {
        return createDelete()
            .where(ExampleEntity::getSingleEnum, status)
            .execute();
    }

}
