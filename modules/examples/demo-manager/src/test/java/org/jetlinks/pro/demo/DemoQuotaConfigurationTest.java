package org.jetlinks.pro.demo;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.demo.configuration.DemoQuotaConfiguration;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.limiter.DemoInspectionItemsQuotaLimiter;
import org.jetlinks.pro.tenant.resource.AssetResourceMetric;
import org.jetlinks.pro.tenant.resource.ResourceLimitType;
import org.jetlinks.pro.tenant.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoQuotaConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(DemoQuotaConfiguration.class, Dependencies.class);

    @Test
    void oneBeanProvidesBothMetricAndEntityMetering() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DemoInspectionItemsQuotaLimiter.class);
            assertThat(context).hasSingleBean(AssetResourceMetric.class);
            DemoInspectionItemsQuotaLimiter limiter = context.getBean(DemoInspectionItemsQuotaLimiter.class);
            assertThat(context.getBean(AssetResourceMetric.class)).isSameAs(limiter);
            assertThat(limiter.getAssetType().getId()).isEqualTo("demo-record");
            assertThat(limiter.getId()).isEqualTo("inspectionItems");
            assertThat(limiter.getName()).isEqualTo("巡检项保有量");
            assertThat(limiter.getDescription()).contains("清单项总数");
            assertThat(limiter.getUnit()).isEqualTo("项");
            assertThat(limiter.getType()).isEqualTo(ResourceType.entity);
            assertThat(limiter.getLimitType()).isEqualTo(ResourceLimitType.quotaLimit);
        });
    }

    @Test
    void noSaasRuntimeDoesNotLoadOptionalLimiterSuperclass() {
        runner.withClassLoader(new FilteredClassLoader("org.jetlinks.pro.tenant"))
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.containsBean("demoInspectionItemsQuotaLimiter")).isFalse();
            });
    }

    @Configuration(proxyBeanMethods = false)
    static class Dependencies {

        @Bean
        @SuppressWarnings("unchecked")
        ReactiveRepository<DemoRecordEntity, String> quotaRecordRepository() {
            return mock(ReactiveRepository.class);
        }

        @Bean
        TransactionalOperator quotaTransactionalOperator() {
            return mock(TransactionalOperator.class);
        }
    }
}
