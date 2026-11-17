package org.jetlinks.pro.demo.service;

import org.aopalliance.intercept.MethodInterceptor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Direct 直接使用实体仓储，需在其原始 DSL 执行链外建立事务，使班组行锁覆盖归属写入。
 * 仅装饰本应用的计划仓储，继续复用平台 Direct 和正式实体生命周期。
 */
@Component
public class DemoRecordRepositoryTransactionPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<TransactionalOperator> transactions;

    public DemoRecordRepositoryTransactionPostProcessor(ObjectProvider<TransactionalOperator> transactions) {
        this.transactions = transactions;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof ReactiveRepository<?, ?>)
            || !beanName.equals(DemoRecordEntity.class.getSimpleName() + "ReactiveRepository")) {
            return bean;
        }
        ProxyFactory proxy = new ProxyFactory(bean);
        proxy.addAdvice((MethodInterceptor) invocation -> {
            Object result = invocation.proceed();
            if (invocation.getMethod().getName().equals("createUpdate")) {
                ReactiveUpdate<?> update = (ReactiveUpdate<?>) result;
                return update.onExecute((dsl, execute) -> transactions.getObject().transactional(execute));
            }
            return result;
        });
        return proxy.getProxy();
    }
}
