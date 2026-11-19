package org.jetlinks.pro.demo.configuration;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.limiter.DemoInspectionItemsQuotaLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * 租户运行时存在时注册一次指标和计量监听器，非 SaaS 应用不加载可选依赖的子类。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.jetlinks.pro.tenant.impl.limiter.EntityEventQuotaLimiter")
public class DemoQuotaConfiguration {

    @Bean
    public DemoInspectionItemsQuotaLimiter demoInspectionItemsQuotaLimiter(
        ReactiveRepository<DemoRecordEntity, String> repository,
        TransactionalOperator transactionalOperator) {
        return new DemoInspectionItemsQuotaLimiter(repository, transactionalOperator);
    }
}
