package org.jetlinks.pro.demo;

import io.r2dbc.spi.ConnectionFactory;
import org.hswebframework.ezorm.rdb.operator.DatabaseOperator;
import org.hswebframework.web.api.crud.entity.TransactionManagers;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.crud.configuration.EasyormConfiguration;
import org.hswebframework.web.crud.configuration.EntityFactoryConfiguration;
import org.hswebframework.web.crud.configuration.R2dbcSqlExecutorConfiguration;
import org.hswebframework.web.crud.query.DefaultQueryHelper;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.starter.jackson.CustomCodecsAutoConfiguration;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.pro.assets.AssetsConfiguration;
import org.jetlinks.pro.assets.dynamic.DynamicDimensionConfiguration;
import org.jetlinks.pro.assets.impl.DimensionAssetBindManager;
import org.jetlinks.pro.assets.impl.direct.DirectAssetBindConfiguration;
import org.jetlinks.pro.assets.impl.terms.DimensionAssetsTermBuilder;
import org.jetlinks.pro.demo.asset.DemoAssetSupplier;
import org.jetlinks.pro.demo.controller.DemoInspectionTeamController;
import org.jetlinks.pro.demo.controller.DemoRecordController;
import org.jetlinks.pro.demo.event.DemoInspectionTeamLifecycleHandler;
import org.jetlinks.pro.demo.event.DemoRecordLifecycleHandler;
import org.jetlinks.pro.demo.service.DemoInspectionTeamService;
import org.jetlinks.pro.demo.service.DemoRecordService;
import org.jetlinks.pro.demo.service.DemoRecordRepositoryTransactionPostProcessor;
import org.jetlinks.supports.event.InternalEventBus;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/** 仅装配示例业务及真实资产基础设施，绑定、目录和闭包均使用测试数据库。 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement(proxyTargetClass = true)
@EnableEasyormRepository({"org.jetlinks.pro.demo.entity", "org.jetlinks.pro.assets.dynamic.entity",
    "org.jetlinks.pro.assets.impl.entity"})
@Import({DemoRecordService.class, DemoRecordController.class, DemoInspectionTeamService.class,
    DemoInspectionTeamController.class, DemoAssetSupplier.class, DemoRecordLifecycleHandler.class,
    DemoInspectionTeamLifecycleHandler.class, AssetsConfiguration.class, DimensionAssetBindManager.class,
    DynamicDimensionConfiguration.class, DirectAssetBindConfiguration.class, DimensionAssetsTermBuilder.class,
    DemoRecordRepositoryTransactionPostProcessor.class, DemoDimensionIntegrationTest.FailureProbe.class})
@ImportAutoConfiguration({R2dbcAutoConfiguration.class, TransactionAutoConfiguration.class,
    EasyormConfiguration.class, R2dbcSqlExecutorConfiguration.class, EntityFactoryConfiguration.class,
    JacksonAutoConfiguration.class, CustomCodecsAutoConfiguration.class})
class DemoIntegrationTestConfiguration {
    @Bean(TransactionManagers.reactiveTransactionManager)
    ReactiveTransactionManager transactionManager(ConnectionFactory factory) {
        return new R2dbcTransactionManager(factory);
    }

    @Bean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager manager) {
        return TransactionalOperator.create(manager);
    }

    @Bean
    QueryHelper queryHelper(DatabaseOperator database) {
        return new DefaultQueryHelper(database);
    }

    @Bean
    EventBus eventBus() {
        return new InternalEventBus();
    }

    @Bean
    DatabaseClient databaseClient(ConnectionFactory factory) {
        return DatabaseClient.create(factory);
    }
}
