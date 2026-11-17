package org.jetlinks.pro.demo;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.pro.assets.AssetAccessContext;
import org.jetlinks.pro.assets.AssetAccessDescription;
import org.jetlinks.pro.assets.AssetDimensionProvider;
import org.jetlinks.pro.assets.AssetManager;
import org.jetlinks.pro.assets.AssetsAccessSupport;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.AssetsHolderHolder;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.assets.scope.AssetScopeWebFilter;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.tenant.TenantInfo;
import org.jetlinks.pro.tenant.TenantRegistry;
import org.jetlinks.pro.tenant.assets.TenantAssetProvider;
import org.jetlinks.pro.tenant.assets.TenantAssetsAccessSupport;
import org.jetlinks.pro.tenant.configuration.TenantAssetsProperties;
import org.jetlinks.pro.tenant.crud.TenantColumnEventListener;
import org.jetlinks.pro.tenant.crud.TenantColumnTableCustomizer;
import org.jetlinks.pro.tenant.enums.TenantState;
import org.jetlinks.pro.tenant.holder.TenantHolder;
import org.jetlinks.pro.tenant.impl.DefaultTenantRegistry;
import org.jetlinks.pro.tenant.resource.TenantResourceLimiterManager;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.jetlinks.supports.config.InMemoryConfigStorageManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig({DemoIntegrationTestConfiguration.class, DemoInspectionTeamApplicationScopeTest.TenantFixtures.class})
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///demo-team-application-scope;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoInspectionTeamApplicationScopeTest extends DemoIntegrationSupport {

    private static final String TENANT = "inspection-tenant";
    private static final String OTHER_TENANT = "other-inspection-tenant";
    private static final String APP_TYPE = BusinessApplicationDimension.TYPE;
    private static final AssetScope APP = AssetScope.of(APP_TYPE, "fire-inspection");
    private static final AssetScope OTHER_APP = AssetScope.of(APP_TYPE, "power-inspection");

    @Autowired
    private DatabaseClient database;

    @Test
    void tenantOwnerCreatesTeamVisibleOnlyWithinSelectedApplication() {
        TestAuthentication owner = tenantOwner();
        AssetAccessContext access = inApplication(owner, APP,
            AssetsHolderHolder.currentAccessContext(DemoAssetType.inspectionTeam)).block(TIMEOUT);
        assertNotNull(access);
        assertEquals(AssetAccessDescription.State.RESTRICTED, access.ordinary().state());
        assertEquals(List.of("tenant"), access.ordinary().rules().stream()
            .map(AssetAccessDescription.Rule::supportId).toList());
        assertEquals(List.of(new AssetAccessContext.Scope(APP_TYPE, APP.id(), false, true)), access.scopes());

        DemoInspectionTeamEntity input = new DemoInspectionTeamEntity();
        input.setName("消防设施巡检班组");
        DemoInspectionTeamEntity created = inApplication(owner, APP, teamController.add(Mono.just(input)))
            .block(TIMEOUT);
        assertNotNull(created);
        assertEquals(TENANT, database.sql("select tenant_id from demo_inspection_team where id = :id")
            .bind("id", created.getId())
            .map((row, metadata) -> row.get("tenant_id", String.class)).one().block(TIMEOUT));
        assertTrue(inTenant(owner, teamIds()).block(TIMEOUT).contains(created.getId()));
        assertEquals(1L, bindings.getBindings(APP_TYPE, List.of(APP.id()), DemoAssetType.inspectionTeam,
            List.of(created.getId())).count().block(TIMEOUT));
        assertTrue(inApplication(owner, APP, teamIds()).block(TIMEOUT).contains(created.getId()));
        assertEquals(List.of(), inApplication(owner, OTHER_APP, teamIds()).block(TIMEOUT));
        inApplication(owner, APP, AssetsHolder.assertPermission(
            DemoAssetType.inspectionTeam, created.getId(), CrudAssetPermission.read)).block(TIMEOUT);
        assertThrows(AccessDenyException.class, () -> inApplication(owner, OTHER_APP, AssetsHolder.assertPermission(
            DemoAssetType.inspectionTeam, created.getId(), CrudAssetPermission.read)).block(TIMEOUT));
    }

    @Test
    void recordDetailUsesPersistedTenantOwnershipForPointReadPermission() {
        TestAuthentication owner = tenantOwner();
        DemoInspectionTeamEntity input = new DemoInspectionTeamEntity();
        input.setName("机电巡检班");
        DemoInspectionTeamEntity createdTeam = inApplication(owner, APP, teamController.add(Mono.just(input))).block(TIMEOUT);
        assertNotNull(createdTeam);
        DemoRecordEntity created = inApplication(owner, APP,
            controller.add(Mono.just(recordInput("机房设备巡检计划", createdTeam.getId())))).block(TIMEOUT);
        assertNotNull(created);
        DemoRecordEntity visible = inApplication(owner, APP, controller.getById(created.getId())).block(TIMEOUT);
        assertNotNull(visible);
        assertEquals(created.getId(), visible.getId());
        assertThrows(AccessDenyException.class, () -> inApplication(tenantOwner(OTHER_TENANT), APP, OTHER_TENANT,
            controller.getById(created.getId())).block(TIMEOUT));
    }

    private TestAuthentication tenantOwner() {
        return tenantOwner(TENANT);
    }

    private TestAuthentication tenantOwner(String tenantId) {
        TestAuthentication owner = new TestAuthentication("project-owner");
        owner.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        owner.addPermission(DemoAssetType.TYPE_ID, "query", "save");
        owner.addDimension("tenant", tenantId);
        owner.addDimension("role", "project-owner-role", Map.of("tenantId", tenantId, "dataAccess", List.of(
            Map.of("assetType", DemoAssetType.TEAM_TYPE_ID,
                "accesses", List.of(Map.of("supportId", "tenant"))),
            Map.of("assetType", DemoAssetType.TYPE_ID,
                "accesses", List.of(Map.of("supportId", "tenant"))))));
        Map<String, Object> policy = Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TEAM_TYPE_ID, "SELECT_AND_BIND", DemoAssetType.TYPE_ID, "SELECT_AND_BIND"));
        owner.addDimension(APP_TYPE, APP.id(), policy);
        owner.addDimension(APP_TYPE, OTHER_APP.id(), policy);
        return owner;
    }

    private <T> Mono<T> inApplication(Authentication owner, AssetScope app, Mono<T> action) {
        return inApplication(owner, app, TENANT, action);
    }

    private <T> Mono<T> inApplication(Authentication owner, AssetScope app, String tenantId, Mono<T> action) {
        // X-Tenant-Domain 解析后的租户选择是业务上下文；活动 Scope 只有应用。
        return Mono.defer(() -> {
            AtomicReference<T> result = new AtomicReference<>();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/demo/inspection-team")
                .header(AssetScopeWebFilter.HEADER, app.type() + ':' + app.id()).build());
            return new AssetScopeWebFilter(scopeResolver)
                    .filter(exchange, ignored -> action.doOnNext(result::set).then())
                .contextWrite(context -> context.put(Authentication.class, owner))
                .contextWrite(TenantHolder.makeCurrent(tenantId))
                .then(Mono.defer(() -> Mono.justOrEmpty(result.get())));
        });
    }

    private <T> Mono<T> inTenant(Authentication owner, Mono<T> action) {
        return action.contextWrite(context -> context.put(Authentication.class, owner))
            .contextWrite(TenantHolder.makeCurrent(TENANT));
    }

    private Mono<List<String>> teamIds() {
        return teamController.query(QueryParamEntity.of().noPaging()).map(DemoInspectionTeamEntity::getId).collectList();
    }

    @Configuration(proxyBeanMethods = false)
    static class TenantFixtures {
        @Bean(destroyMethod = "dispose")
        Disposable tenantRegistryRegistration() {
            // 只替换集群租户注册存储；初始化动态维度前，租户字段监听器就需要已装配的注册表。
            TenantRegistry registry = new DefaultTenantRegistry(new InMemoryConfigStorageManager(),
                mock(TenantResourceLimiterManager.class));
            TenantInfo info = new TenantInfo();
            info.setId(TENANT);
            info.setName("巡检项目");
            info.setState(TenantState.enabled);
            assertNotNull(registry.register(info).block(TIMEOUT));
            TenantInfo other = new TenantInfo();
            other.setId(OTHER_TENANT);
            other.setName("其他巡检项目");
            other.setState(TenantState.enabled);
            assertNotNull(registry.register(other).block(TIMEOUT));
            Object previous = ReflectionTestUtils.getField(TenantHolder.class, "registry");
            Optional<AssetDimensionProvider> previousDimension = AssetDimensionProvider.supports.get("tenant");
            Optional<AssetsAccessSupport> previousAccess = AssetsAccessSupport.supports.get("tenant");
            ReflectionTestUtils.setField(TenantHolder.class, "registry", registry);
            return () -> {
                ReflectionTestUtils.setField(TenantHolder.class, "registry", previous);
                previousDimension.ifPresentOrElse(provider -> AssetDimensionProvider.supports.register("tenant", provider),
                    () -> AssetDimensionProvider.supports.unregister("tenant"));
                previousAccess.ifPresentOrElse(support -> AssetsAccessSupport.supports.register("tenant", support),
                    () -> AssetsAccessSupport.supports.unregister("tenant"));
            };
        }

        @Bean
        TenantColumnTableCustomizer tenantColumns() {
            return new TenantColumnTableCustomizer(new TenantAssetsProperties(), List.of());
        }

        @Bean
        TenantColumnEventListener tenantColumnEvents() {
            return new TenantColumnEventListener();
        }

        @Bean
        @DependsOn("tenantRegistryRegistration")
        TenantAssetProvider tenantAssetProvider(TenantColumnTableCustomizer columns) {
            return new TenantAssetProvider(columns);
        }

        @Bean
        @DependsOn("tenantRegistryRegistration")
        TenantAssetsAccessSupport tenantAssetsAccessSupport(AssetManager assets) {
            return new TenantAssetsAccessSupport(assets);
        }
    }
}
