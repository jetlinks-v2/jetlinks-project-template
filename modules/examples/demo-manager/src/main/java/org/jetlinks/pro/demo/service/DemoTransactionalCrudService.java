package org.jetlinks.pro.demo.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.reactivestreams.Publisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 在正式 CRUD 读取旧快照前锁定业务记录，实体事件及其副作用共用原事务。
 * DSL 的事务在执行时建立，不能只给返回 DSL 对象的工厂方法加事务注解。
 */
public abstract class DemoTransactionalCrudService<E extends ExtendableEntity<String>>
    extends GenericReactiveCrudService<E, String> {

    private final TransactionalOperator transactionalOperator;

    protected DemoTransactionalCrudService(ReactiveRepository<E, String> repository,
                                           TransactionalOperator transactionalOperator) {
        super(repository);
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<SaveResult> save(Publisher<E> entities) {
        return Flux.from(entities).collectList()
            .flatMap(list -> lockIds(list.stream().map(E::getId).filter(Objects::nonNull).toList())
                .then(Mono.defer(() -> getRepository().save(list))))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<SaveResult> save(E entity) {
        return save(Mono.just(entity));
    }

    @Override
    public Mono<SaveResult> save(Collection<E> entities) {
        return save(Flux.fromIterable(entities));
    }

    @Override
    public Mono<Integer> updateById(String id, Mono<E> entity) {
        return lockIds(List.of(id))
            .then(Mono.defer(() -> getRepository().updateById(id, entity)))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Integer> updateById(String id, E entity) {
        return updateById(id, Mono.just(entity));
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> ids) {
        return Flux.from(ids).distinct().collectList()
            .flatMap(list -> lockIds(list)
                .then(Mono.defer(() -> getRepository().deleteById(Flux.fromIterable(list)))))
            .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Integer> deleteById(String id) {
        return deleteById(Mono.just(id));
    }

    @Override
    public ReactiveUpdate<E> createUpdate() {
        return getRepository().createUpdate().onExecute((update, execute) -> getRepository()
            .createQuery().setParam(update.toQueryParam())
            .orderBy(SortOrder.asc("id")).forUpdate().fetch().then(execute)
            .as(transactionalOperator::transactional));
    }

    @Override
    public ReactiveDelete createDelete() {
        return getRepository().createDelete().onExecute((delete, execute) -> getRepository()
            .createQuery().setParam(delete.toQueryParam())
            .orderBy(SortOrder.asc("id")).forUpdate().fetch().then(execute)
            .as(transactionalOperator::transactional));
    }

    private Mono<Void> lockIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Mono.empty();
        }
        // 与额度限制器保持“业务记录 -> 租户额度”的稳定锁顺序。
        return getRepository().createQuery().in("id", ids)
            .orderBy(SortOrder.asc("id")).forUpdate().fetch().then();
    }
}
