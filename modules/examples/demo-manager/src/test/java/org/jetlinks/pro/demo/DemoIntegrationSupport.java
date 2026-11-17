package org.jetlinks.pro.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.pro.assets.AssetBindManager;
import org.jetlinks.pro.assets.AssetBindRequest;
import org.jetlinks.pro.assets.AssetManager;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.aop.AopAssetsControllerHandler;
import org.jetlinks.pro.assets.aop.AopAssetsControllerSupport;
import org.jetlinks.pro.assets.dynamic.entity.DynamicDimensionTypeEntity;
import org.jetlinks.pro.assets.dynamic.registry.DynamicDimensionTypeRegistry;
import org.jetlinks.pro.assets.dynamic.service.DynamicDimensionService;
import org.jetlinks.pro.assets.impl.DimensionAssetsHolderProvider;
import org.jetlinks.pro.assets.impl.ctx.LocalAssetAccessesHolderContext;
import org.jetlinks.pro.assets.impl.entity.DimensionClosureEntity;
import org.jetlinks.pro.assets.impl.entity.DimensionAssetBindingEntity;
import org.jetlinks.pro.assets.impl.supports.AllDataDimensionSupport;
import org.jetlinks.pro.assets.impl.supports.BusinessApplicationAssetsAccessSupport;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeResolverManager;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.DemoInspectionTeamController;
import org.jetlinks.pro.demo.controller.DemoRecordController;
import org.jetlinks.pro.demo.entity.DemoInspectionItem;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.service.DemoInspectionTeamService;
import org.jetlinks.pro.demo.service.DemoRecordService;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(DemoIntegrationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///demo-dimension;DB_CLOSE_DELAY=-1",
    "easyorm.dialect=h2", "easyorm.default-schema=PUBLIC",
    "jetlinks.assets-controller.enabled=false",
    "jetlinks.assets.direct.enabled=true",
    "jetlinks.assets.direct.tables[demo_record].asset-type=demo-record",
    "jetlinks.assets.direct.tables[demo_record].dimension-column[demo-inspection-team]=inspectionTeamId"
})
abstract class DemoIntegrationSupport {
    static final Duration TIMEOUT = Duration.ofSeconds(10);
    @Autowired DemoRecordController controller;
    @Autowired DemoRecordService service;
    @Autowired DemoInspectionTeamController teamController;
    @Autowired DemoInspectionTeamService teams;
    @Autowired AssetManager assetManager;
    @Autowired AssetBindManager bindings;
    @Autowired DynamicDimensionService dimensions;
    @Autowired DynamicDimensionTypeRegistry typeRegistry;
    @Autowired ReactiveRepository<DynamicDimensionTypeEntity, String> typeRepository;
    @Autowired ReactiveRepository<DimensionClosureEntity, String> closureRepository;
    @Autowired ReactiveRepository<DimensionAssetBindingEntity, String> bindingRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired TransactionalOperator transactions;
    @Autowired AssetScopeResolverManager scopeResolver;

    @BeforeEach
    void initializePublishedDimensionType() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(
            "/auto-init/DynamicDimensionTypeEntity.entity.json")) {
            assertNotNull(resource);
            List<DynamicDimensionTypeEntity> types = objectMapper.readValue(resource,
                new TypeReference<List<DynamicDimensionTypeEntity>>() { });
            typeRepository.save(types).then(typeRegistry.refresh()).block(TIMEOUT);
        }
        // 最小上下文不启动整套 Web/授权自动装配，显式复用平台相同 Advisor 与完整权限处理器。
        AopAssetsControllerSupport advisor = new AopAssetsControllerSupport();
        advisor.setAdvice(new AopAssetsControllerHandler(transactions, null, bindings));
        ProxyFactory recordFactory = new ProxyFactory(controller);
        recordFactory.setProxyTargetClass(true);
        recordFactory.addAdvisor(advisor);
        controller = (DemoRecordController) recordFactory.getProxy();
        ProxyFactory teamFactory = new ProxyFactory(teamController);
        teamFactory.setProxyTargetClass(true);
        teamFactory.addAdvisor(advisor);
        teamController = (DemoInspectionTeamController) teamFactory.getProxy();
    }

    DemoInspectionTeamEntity team(String parent) {
        DemoInspectionTeamEntity entity = new DemoInspectionTeamEntity();
        entity.setName("班组-" + UUID.randomUUID());
        entity.setDescription("负责消防设施与通道巡检");
        entity.setParentId(parent);
        return teamController.add(Mono.just(entity))
            .contextWrite(context -> context.put(Authentication.class, fixtureCreator())).block(TIMEOUT);
    }

    DemoRecordEntity record(String name, String teamId) {
        DemoRecordEntity entity = recordInput(name, teamId);
        return controller.add(Mono.just(entity))
            .contextWrite(context -> context.put(Authentication.class, fixtureCreator())).block(TIMEOUT);
    }

    static TestAuthentication fixtureCreator() {
        return new TestAuthentication("demo-fixture-creator");
    }

    static DemoRecordEntity recordInput(String name, String teamId) {
        DemoRecordEntity entity = new DemoRecordEntity();
        entity.setName(name);
        entity.setDescription("日常消防巡检计划");
        entity.setInspectionTeamId(teamId);
        DemoInspectionItem item = new DemoInspectionItem();
        item.setName("消防通道检查");
        item.setRequirement("通道无堆放，安全出口可正常开启");
        entity.setInspectionItems(List.of(item));
        return entity;
    }

    TestAuthentication member(String memberTeam) {
        return member(memberTeam, "ignore");
    }

    TestAuthentication member(String memberTeam, String teamPolicy) {
        TestAuthentication authentication = new TestAuthentication("user-" + UUID.randomUUID());
        if (memberTeam != null) {
            authentication.addDimension(DemoAssetType.TEAM_TYPE_ID, memberTeam);
        }
        authentication.addDimension(BusinessApplicationDimension.TYPE, "inspection-app", Map.of(
            AssetScopeSources.POLICY_OPTION_KEY, Map.of(DemoAssetType.TYPE_ID, "SELECT_AND_BIND",
                DemoAssetType.TEAM_TYPE_ID, "IGNORE")));
        authentication.addDimension("role", "inspection-role", Map.of("dataAccess", List.of(
            Map.of("assetType", DemoAssetType.TYPE_ID, "accesses",
                List.of(Map.of("supportId", "dynamic_" + DemoAssetType.TEAM_TYPE_ID))),
            Map.of("assetType", DemoAssetType.TEAM_TYPE_ID, "accesses",
                List.of(Map.of("supportId", teamPolicy))))));
        return authentication;
    }

    AssetsHolder holder(Authentication authentication) {
        DimensionAssetsHolderProvider provider = new DimensionAssetsHolderProvider(assetManager, bindings,
            List.of(new BusinessApplicationAssetsAccessSupport(), new AllDataDimensionSupport()),
            new LocalAssetAccessesHolderContext());
        return provider.createHolder(authentication).single().block(TIMEOUT);
    }

    <T> Mono<T> as(Authentication authentication, Mono<T> action) {
        AssetsHolder holder = holder(authentication);
        return action.contextWrite(context -> context.put(Authentication.class, authentication)
            .put(AssetsHolder.class, holder));
    }

    <T> Mono<T> scoped(Authentication authentication, Mono<T> action, AssetScope... scopes) {
        AssetsHolder ordinary = holder(authentication);
        return scopeResolver.resolve(List.of(scopes), authentication)
            .flatMap(activation -> action.contextWrite(context -> context.put(AssetsHolder.class, ordinary))
                .contextWrite(activation.write()));
    }

    void bind(String dimensionType, String dimensionId, String assetType, String assetId) {
        bindings.bindAssets(Mono.just(AssetBindRequest.builder()
            .targetType(dimensionType).targetId(dimensionId).assetType(assetType)
            .assetIdList(List.of(assetId)).allPermission(true).build())).block(TIMEOUT);
    }
}
