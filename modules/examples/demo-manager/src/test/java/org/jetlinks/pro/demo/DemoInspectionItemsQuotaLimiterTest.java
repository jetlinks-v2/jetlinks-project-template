package org.jetlinks.pro.demo;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.crud.configuration.EasyormConfiguration;
import org.hswebframework.web.crud.configuration.EntityFactoryConfiguration;
import org.hswebframework.web.crud.configuration.R2dbcSqlExecutorConfiguration;
import org.jetlinks.core.cluster.ClusterManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.pro.demo.configuration.DemoQuotaConfiguration;
import org.jetlinks.pro.demo.entity.DemoInspectionItem;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.limiter.DemoInspectionItemsQuotaLimiter;
import org.jetlinks.pro.tenant.Tenant;
import org.jetlinks.pro.tenant.TenantInfo;
import org.jetlinks.pro.tenant.enums.TenantState;
import org.jetlinks.pro.tenant.exception.OutOfQuotaException;
import org.jetlinks.pro.tenant.holder.TenantHolder;
import org.jetlinks.pro.tenant.holder.TenantHolderConfiguration;
import org.jetlinks.pro.tenant.impl.DefaultTenantRegistry;
import org.jetlinks.pro.tenant.impl.limiter.InDatabaseQuotaLimiter;
import org.jetlinks.pro.tenant.impl.limiter.TenantMetricUsageEntity;
import org.jetlinks.pro.tenant.resource.MixedTenantResourceLimiter;
import org.jetlinks.pro.tenant.resource.TenantResourceLimiter;
import org.jetlinks.pro.tenant.resource.TenantResourceLimiterManager;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.jetlinks.supports.config.EventBusStorageManager;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(DemoInspectionItemsQuotaLimiterTest.QuotaDatabaseConfiguration.class)
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///demo-inspection-quota;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "easyorm.dialect=h2",
    "easyorm.default-schema=PUBLIC"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoInspectionItemsQuotaLimiterTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Autowired
    private ReactiveRepository<DemoRecordEntity, String> records;
    @Autowired
    private ReactiveRepository<TenantMetricUsageEntity, String> usages;
    @Autowired
    private TransactionalOperator transactions;
    @Autowired
    private DefaultTenantRegistry registry;
    @Autowired
    private DemoInspectionItemsQuotaLimiter meter;

    private String tenantId;

    @BeforeEach
    void prepareTenant() {
        tenantId = "quota-" + UUID.randomUUID();
        registerTenant(tenantId);
        configure(tenantId, 10);
    }

    @Test
    void realListSizesDriveCreateSaveModifyAndDelete() {
        DemoRecordEntity first = record("A", 4);
        DemoRecordEntity second = record("B", 5);
        write(tenantId, records.insert(first)).block(TIMEOUT);
        save(second).block(TIMEOUT);
        assertUsage(tenantId, 9);

        DemoRecordEntity rejected = record("C", 2);
        assertThrows(OutOfQuotaException.class, () -> save(rejected).block(TIMEOUT));
        assertNull(records.findById(rejected.getId()).block(TIMEOUT));
        assertUsage(tenantId, 9);

        second.setInspectionItems(items(6));
        save(second).block(TIMEOUT);
        assertUsage(tenantId, 10);
        second.setInspectionItems(items(5));
        save(second).block(TIMEOUT);
        assertUsage(tenantId, 9);

        write(tenantId, records.createUpdate()
            .set(DemoRecordEntity::getInspectionItems, items(6))
            .where(DemoRecordEntity::getId, second.getId()).execute()).block(TIMEOUT);
        assertUsage(tenantId, 10);
        write(tenantId, records.createUpdate()
            .set(DemoRecordEntity::getInspectionItems, items(5))
            .where(DemoRecordEntity::getId, second.getId()).execute()).block(TIMEOUT);
        assertUsage(tenantId, 9);

        write(tenantId, records.createUpdate()
            .set(DemoRecordEntity::getName, "B renamed")
            .set(DemoRecordEntity::getDescription, "只修改业务说明")
            .set(DemoRecordEntity::getInspectionTeamId, "another-team")
            .where(DemoRecordEntity::getId, second.getId()).execute()).block(TIMEOUT);
        assertUsage(tenantId, 9);

        write(tenantId, records.deleteById(first.getId())).block(TIMEOUT);
        assertUsage(tenantId, 5);
    }

    @Test
    void mixedSaveUsesPersistedOldListSizesAndRollsBackTheWholeBatch() {
        DemoRecordEntity first = record("existing", 4);
        save(first).block(TIMEOUT);
        first.setInspectionItems(items(3));
        DemoRecordEntity second = record("new", 5);
        write(tenantId, records.save(List.of(first, second))).block(TIMEOUT);
        assertUsage(tenantId, 8);

        first.setInspectionItems(items(4));
        DemoRecordEntity rejected = record("rejected", 3);
        assertThrows(OutOfQuotaException.class,
            () -> write(tenantId, records.save(List.of(first, rejected))).block(TIMEOUT));
        assertEquals(3, records.findById(first.getId()).block(TIMEOUT).getInspectionItems().size());
        assertNull(records.findById(rejected.getId()).block(TIMEOUT));
        assertUsage(tenantId, 8);
    }

    @Test
    void failureAfterSqlRollsBackRecordAndDatabaseUsageTogether() {
        DemoRecordEntity rejected = record("rollback-create", 4);
        assertThrows(IllegalStateException.class, () -> write(tenantId,
            records.save(rejected).then(Mono.error(() -> new IllegalStateException("business failed"))))
            .block(TIMEOUT));
        assertNull(records.findById(rejected.getId()).block(TIMEOUT));
        assertUsage(tenantId, 0);

        DemoRecordEntity existing = record("rollback-update", 4);
        save(existing).block(TIMEOUT);
        existing.setInspectionItems(items(6));
        assertThrows(IllegalStateException.class, () -> write(tenantId,
            records.save(existing).then(Mono.error(() -> new IllegalStateException("binding failed"))))
            .block(TIMEOUT));
        assertEquals(4, records.findById(existing.getId()).block(TIMEOUT).getInspectionItems().size());
        assertUsage(tenantId, 4);
    }

    @Test
    void configuredZeroUnlimitedAndMissingQuotaKeepPlatformSemantics() {
        configure(tenantId, 0);
        assertThrows(OutOfQuotaException.class, () -> save(record("zero", 1)).block(TIMEOUT));
        assertUsage(tenantId, 0);

        configure(tenantId, -1);
        save(record("unlimited", 12)).block(TIMEOUT);
        assertUsage(tenantId, 12);

        String unconfigured = "unconfigured-" + UUID.randomUUID();
        registerTenant(unconfigured);
        write(unconfigured, records.save(record("no-limit-row", 20))).block(TIMEOUT);
        assertUsage(unconfigured, 0);
        assertEquals(-1D, quota(unconfigured).getQuota().block(TIMEOUT));
    }

    @Test
    void tenantsHaveIndependentUsageAndConcurrentCreatesShareOneQuota() {
        String otherTenant = "other-" + UUID.randomUUID();
        registerTenant(otherTenant);
        configure(otherTenant, 10);
        write(otherTenant, records.save(record("other", 7))).block(TIMEOUT);
        save(record("base", 8)).block(TIMEOUT);

        // H2 的 CLOB 绑定内部使用阻塞迭代，并发测试须运行在允许该驱动行为的测试线程。
        List<Signal<Boolean>> results = Flux.merge(
            save(record("concurrent-a", 2)).thenReturn(true).materialize().subscribeOn(Schedulers.boundedElastic()),
            save(record("concurrent-b", 2)).thenReturn(true).materialize().subscribeOn(Schedulers.boundedElastic()))
            .collectList().block(TIMEOUT);
        assertNotNull(results);
        assertEquals(1, results.stream().filter(Signal::isOnNext).count(), results::toString);
        assertEquals(1, results.stream().filter(Signal::isOnError).count(), results::toString);
        assertTrue(results.stream().filter(Signal::isOnError)
            .allMatch(signal -> signal.getThrowable() instanceof OutOfQuotaException), results::toString);
        assertUsage(tenantId, 10);
        assertUsage(otherTenant, 7);
    }

    @Test
    void noTenantAndZeroDifferenceDoNotInventUsage() {
        DemoRecordEntity record = record("unchanged", 4);
        save(record).block(TIMEOUT);
        save(record).block(TIMEOUT);
        meter.handleBeforeSave(null, List.of(record)).block(TIMEOUT);
        assertUsage(tenantId, 4);
    }

    private Mono<Void> save(DemoRecordEntity record) {
        return write(tenantId, records.save(record));
    }

    private Mono<Void> write(String tenant, Mono<?> operation) {
        TestAuthentication creator = new TestAuthentication("quota-fixture-creator");
        creator.addDimension("tenant", tenant);
        return operation.as(transactions::transactional).then().contextWrite(TenantHolder.makeCurrent(tenant))
            .contextWrite(context -> context.put(Authentication.class, creator));
    }

    private void registerTenant(String id) {
        TenantInfo info = new TenantInfo();
        info.setId(id);
        info.setName(id);
        info.setState(TenantState.enabled);
        registry.register(info).block(TIMEOUT);
    }

    private void configure(String id, double amount) {
        TenantMetricUsageEntity usage = new TenantMetricUsageEntity();
        usage.setTenantId(id);
        usage.setMetricId(DemoInspectionItemsQuotaLimiter.METRIC_ID.toString());
        usage.setQuota(amount);
        usages.save(usage).block(TIMEOUT);
    }

    private InDatabaseQuotaLimiter quota(String id) {
        Tenant tenant = registry.getTenant(id).block(TIMEOUT);
        assertNotNull(tenant);
        return new InDatabaseQuotaLimiter(tenant, DemoInspectionItemsQuotaLimiter.METRIC_ID, usages);
    }

    private void assertUsage(String id, double expected) {
        assertEquals(expected, quota(id).getUsedQuota().block(TIMEOUT));
    }

    private DemoRecordEntity record(String name, int count) {
        DemoRecordEntity record = new DemoRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setName(name);
        record.setInspectionTeamId("quota-team");
        record.setInspectionItems(items(count));
        return record;
    }

    private List<DemoInspectionItem> items(int count) {
        List<DemoInspectionItem> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            DemoInspectionItem item = new DemoInspectionItem();
            item.setName("巡检项" + index);
            item.setRequirement("记录设备运行情况");
            items.add(item);
        }
        return items;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableEasyormRepository({"org.jetlinks.pro.demo.entity", "org.jetlinks.pro.tenant.impl.limiter"})
    @Import(DemoQuotaConfiguration.class)
    @ImportAutoConfiguration({R2dbcAutoConfiguration.class, R2dbcTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class, EasyormConfiguration.class,
        R2dbcSqlExecutorConfiguration.class, EntityFactoryConfiguration.class})
    static class QuotaDatabaseConfiguration {

        @Bean
        TenantResourceLimiterManager quotaLimiterManager(
            ReactiveRepository<TenantMetricUsageEntity, String> repository) {
            TenantResourceLimiterManager manager = mock(TenantResourceLimiterManager.class);
            when(manager.createLimiter(any(Tenant.class), eq(DemoInspectionItemsQuotaLimiter.METRIC_ID)))
                .thenAnswer(invocation -> Mono.just(new MixedTenantResourceLimiter(
                    new InDatabaseQuotaLimiter(invocation.getArgument(0),
                        DemoInspectionItemsQuotaLimiter.METRIC_ID, repository),
                    TenantResourceLimiter.unlimited().forConcurrency(),
                    TenantResourceLimiter.unlimited().forRate())));
            return manager;
        }

        @Bean
        DefaultTenantRegistry quotaTenantRegistry(TenantResourceLimiterManager manager) {
            InMemoryConfigStorageManager configurations = new InMemoryConfigStorageManager();
            // 只替换集群配置存储；租户注册、实体事件、额度 SQL 与事务均走正式公共实现。
            try (MockedConstruction<EventBusStorageManager> storage = mockConstruction(
                EventBusStorageManager.class, (mock, context) -> when(mock.getStorage(anyString()))
                    .thenAnswer(invocation -> configurations.getStorage(invocation.getArgument(0))))) {
                return new TenantHolderConfiguration().tenantRegistry(
                    mock(EventBus.class), mock(ClusterManager.class), manager);
            }
        }
    }
}
