package org.jetlinks.pro.demo;

import com.fasterxml.jackson.databind.JsonNode;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.crud.configuration.EasyormConfiguration;
import org.hswebframework.web.crud.configuration.EntityFactoryConfiguration;
import org.hswebframework.web.crud.configuration.R2dbcSqlExecutorConfiguration;
import org.jetlinks.pro.assets.Asset;
import org.jetlinks.pro.assets.AssetBindManager;
import org.jetlinks.pro.assets.AssetBinding;
import org.jetlinks.pro.assets.AssetManager;
import org.jetlinks.pro.assets.AssetSupplier;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.SimpleAssetBinding;
import org.jetlinks.pro.assets.aop.AopAssetsControllerHandler;
import org.jetlinks.pro.assets.impl.DefaultAssetManager;
import org.jetlinks.pro.assets.impl.DimensionAssetsHolderProvider;
import org.jetlinks.pro.assets.impl.ctx.LocalAssetAccessesHolderContext;
import org.jetlinks.pro.assets.impl.supports.BusinessApplicationAssetsAccessSupport;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetSupplier;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.DemoRecordController;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.service.DemoRecordService;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(DemoRecordTest.ConfigurationForTest.class)
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///demo-record;DB_CLOSE_DELAY=-1",
    "easyorm.dialect=h2",
    "easyorm.default-schema=PUBLIC"
})
class DemoRecordTest {

    @Autowired
    private DemoRecordController controller;
    @Autowired
    private DemoRecordService service;
    @Autowired
    private AssetManager assetManager;

    private WebTestClient client;

    @BeforeEach
    void prepare() {
        client = WebTestClient.bindToController(controller).build();
    }

    @Test
    void crudUsesServerCreationTimeAndPreservesItOnUpdate() {
        long started = System.currentTimeMillis();
        DemoRecordEntity created = client.post().uri("/demo/record")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "test-record", "description", "初始说明", "createTime", 1))
            .exchange().expectStatus().is2xxSuccessful()
            .expectBody(DemoRecordEntity.class).returnResult().getResponseBody();
        assertNotNull(created);
        assertNotNull(created.getId());

        DemoRecordEntity persisted = service.findById(created.getId()).block(Duration.ofSeconds(5));
        assertNotNull(persisted);
        assertNotNull(persisted.getCreateTime());
        assertTrue(persisted.getCreateTime() >= started);
        long createTime = persisted.getCreateTime();

        client.put().uri("/demo/record/{id}", created.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("name", "test-updated", "description", "更新说明", "createTime", 2))
            .exchange().expectStatus().is2xxSuccessful().expectBody(Boolean.class).isEqualTo(true);

        DemoRecordEntity updated = service.findById(created.getId()).block(Duration.ofSeconds(5));
        assertNotNull(updated);
        assertEquals("test-updated", updated.getName());
        assertEquals("更新说明", updated.getDescription());
        assertEquals(createTime, updated.getCreateTime());

        JsonNode page = client.post().uri("/demo/record/_query")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("pageIndex", 0, "pageSize", 10,
                              "terms", List.of(Map.of("column", "name", "termType", "eq", "value", "test-updated"))))
            .exchange().expectStatus().is2xxSuccessful()
            .expectBody(JsonNode.class).returnResult().getResponseBody();
        assertNotNull(page);
        assertEquals(1, page.path("total").asInt());
        assertEquals(created.getId(), page.path("data").get(0).path("id").asText());

        client.delete().uri("/demo/record/{id}", created.getId())
            .exchange().expectStatus().is2xxSuccessful();
        assertNull(service.findById(created.getId()).block(Duration.ofSeconds(5)));
    }

    @Test
    void registeredAssetSupplierResolvesPersistedRecord() {
        DemoRecordEntity created = record("test-asset");
        Asset resolved = assetManager.getAsset(DemoAssetType.record, created.getId()).block(Duration.ofSeconds(5));
        assertNotNull(resolved);
        assertEquals(created.getId(), resolved.getId());
        assertEquals("test-asset", resolved.getName());
        assertEquals(DemoAssetType.TYPE_ID, resolved.getType().getId());
        assertSame(DemoAssetType.record, AssetType.of(DemoAssetType.TYPE_ID));
    }

    @Test
    void realApplicationAssetsHolderAllowsBoundUpdateAndRejectsUnboundUpdate() {
        DemoRecordEntity allowed = record("test-allowed");
        DemoRecordEntity denied = record("test-denied");

        // 仅替换绑定存储，范围解析、资产过滤、操作权限与控制器 AOP 都使用平台实现。
        AssetBindManager bindings = mock(AssetBindManager.class);
        when(bindings.getBindings(anyString(), anyCollection(), any(AssetType.class), anyCollection()))
            .thenAnswer(invocation -> {
                Collection<String> applications = invocation.getArgument(1);
                Collection<?> ids = invocation.getArgument(3);
                if (!applications.contains("test-application") || !ids.contains(allowed.getId())) {
                    return Flux.empty();
                }
                AssetBinding binding = new SimpleAssetBinding(BusinessApplicationDimension.INSTANCE,
                    "test-application", DemoAssetType.record, allowed.getId(),
                    DemoAssetType.record.createAllPermission(), null);
                return Flux.just(binding);
            });

        TestAuthentication authentication = new TestAuthentication("test-user")
            .addDimension(BusinessApplicationDimension.TYPE, "test-application")
            .addDimension("role", "test-role", Map.of("dataAccess", List.of(Map.of(
                "assetType", DemoAssetType.TYPE_ID,
                "accesses", List.of(Map.of("supportId", BusinessApplicationDimension.TYPE))))));
        DimensionAssetsHolderProvider provider = new DimensionAssetsHolderProvider(assetManager, bindings,
            List.of(new BusinessApplicationAssetsAccessSupport()), new LocalAssetAccessesHolderContext());
        AssetsHolder holder = provider.createHolder(authentication).single().block(Duration.ofSeconds(5));
        assertNotNull(holder);

        ProxyFactory factory = new ProxyFactory(controller);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new AopAssetsControllerHandler(null, null, bindings));
        DemoRecordController protectedController = (DemoRecordController) factory.getProxy();

        DemoRecordEntity acceptedUpdate = new DemoRecordEntity();
        acceptedUpdate.setName("test-permitted");
        assertEquals(true, protectedController.update(allowed.getId(), Mono.just(acceptedUpdate))
            .contextWrite(context -> context.put(AssetsHolder.class, holder)
                .put(Authentication.class, authentication)).block(Duration.ofSeconds(5)));

        DemoRecordEntity rejectedUpdate = new DemoRecordEntity();
        rejectedUpdate.setName("test-forbidden");
        assertThrows(AccessDenyException.class, () -> protectedController.update(denied.getId(), Mono.just(rejectedUpdate))
            .contextWrite(context -> context.put(AssetsHolder.class, holder)
                .put(Authentication.class, authentication)).block(Duration.ofSeconds(5)));
        assertEquals("test-permitted", service.findById(allowed.getId()).block(Duration.ofSeconds(5)).getName());
        assertEquals("test-denied", service.findById(denied.getId()).block(Duration.ofSeconds(5)).getName());
    }

    private DemoRecordEntity record(String name) {
        DemoRecordEntity entity = new DemoRecordEntity();
        entity.setName(name);
        return controller.add(Mono.just(entity)).block(Duration.ofSeconds(5));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableEasyormRepository("org.jetlinks.pro.demo.entity")
    @Import({DemoRecordService.class, DemoRecordController.class, DemoAssetSupplier.class})
    @ImportAutoConfiguration({R2dbcAutoConfiguration.class, R2dbcTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class, EasyormConfiguration.class,
        R2dbcSqlExecutorConfiguration.class, EntityFactoryConfiguration.class})
    static class ConfigurationForTest {
        @Bean
        AssetManager assetManager(ObjectProvider<AssetSupplier> suppliers) {
            return new DefaultAssetManager(suppliers);
        }
    }
}
