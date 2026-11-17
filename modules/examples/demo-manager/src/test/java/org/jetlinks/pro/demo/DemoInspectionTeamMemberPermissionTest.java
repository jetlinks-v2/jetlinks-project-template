package org.jetlinks.pro.demo;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.pro.assets.AssetSupplier;
import org.jetlinks.pro.assets.AssetManager;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.assets.DefaultAsset;
import org.jetlinks.pro.assets.impl.supports.BusinessApplicationAssetsAccessSupport;
import org.jetlinks.pro.assets.impl.supports.CreatorAssetsAccessSupport;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.assets.scope.AssetScopeWebFilter;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.command.CommandSupportManagerProvider;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.response.DemoInspectionTeamMember;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.memberfixture.DemoMemberUserEntity;
import org.jetlinks.pro.demo.service.DemoInspectionTeamMemberService;
import org.jetlinks.pro.tenant.holder.TenantHolder;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.auth.AuthCommandSupportTypes;
import org.jetlinks.sdk.server.auth.UserInfo;
import org.jetlinks.sdk.server.auth.cmd.BindDimensionUsersCommand;
import org.jetlinks.sdk.server.auth.cmd.UnbindDimensionUsersCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** 用户资产条件使用正式 holder 生成，经 JSON 往返后由 H2 执行；只替代远端命令与用户维度关系。 */
@SpringJUnitConfig({DemoIntegrationTestConfiguration.class,
    DemoInspectionTeamApplicationScopeTest.TenantFixtures.class, DemoInspectionTeamMemberPermissionTest.UserFixtures.class})
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///demo-team-member-permission;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoInspectionTeamMemberPermissionTest extends DemoIntegrationSupport {
    private static final String TENANT = "inspection-tenant";
    private static final String APP_TYPE = BusinessApplicationDimension.TYPE;
    private static final AssetScope APP = AssetScope.of(APP_TYPE, "member-permission-app");
    private static final String OTHER_APP = "member-permission-other-app";

    @Autowired ReactiveRepository<DemoMemberUserEntity, String> users;
    @Autowired org.jetlinks.pro.assets.scope.AssetScopeMaterializers materializers;
    private final Map<String, Set<String>> applicationMembers = new HashMap<>();
    private final Map<String, Set<String>> teamMembers = new HashMap<>();
    private final List<Command<?>> commands = new ArrayList<>();
    private DemoInspectionTeamMemberService members;
    private DemoInspectionTeamEntity team;
    private TestAuthentication operator;
    private Disposable registration;

    @BeforeEach
    void prepareOrdinaryApplicationOperator() {
        members = new DemoInspectionTeamMemberService(teams, dimensions, bindings, materializers);
        operator = applicationOperator();
        DemoInspectionTeamEntity input = new DemoInspectionTeamEntity();
        input.setName("应用内消防巡检班");
        team = inApplication(operator, teamController.add(Mono.just(input))).block(TIMEOUT);
        assertNotNull(team);
        assertEquals(1L, bindings.getBindings(APP_TYPE, List.of(APP.id()), DemoAssetType.inspectionTeam,
            List.of(team.getId())).count().block(TIMEOUT));
        registration = CommandSupportManagerProvider.supports.register(SdkServices.authService, new LocalAuthCommands());
    }

    @AfterEach
    void unregisterCommands() {
        if (registration != null) registration.dispose();
    }

    @Test
    void ordinaryApplicationOperatorQueriesBindsAndUnbindsExistingApplicationUsers() {
        String first = applicationUser(APP.id());
        String second = applicationUser(APP.id());
        teamMembers.put(team.getId(), new HashSet<>(Set.of(first)));
        assertFalse(operator.hasPermission("*", "*"));
        assertEquals(Set.of(first), memberIds(query(false, QueryParamEntity.of())));
        assertEquals(Set.of(second), memberIds(query(true, QueryParamEntity.of())));

        inApplication(operator, members.bind(team.getId(), List.of(second))).block(TIMEOUT);
        assertEquals(Set.of(first, second), memberIds(query(false, QueryParamEntity.of())));
        assertEquals(1, inApplication(operator, members.unbind(team.getId(), List.of(second))).block(TIMEOUT));
        assertEquals(Set.of(first), memberIds(query(false, QueryParamEntity.of())));
        assertTrue(applicationMembers.get(APP.id()).containsAll(List.of(first, second)));
    }

    @Test
    void clientOrCannotExposeOrBindSameTenantUsersOwnedByAnotherApplication() {
        String allowed = applicationUser(APP.id());
        String foreign = applicationUser(OTHER_APP);
        // 同租户的用户虽然是真实应用成员，但未绑定到当前应用的资产范围。
        applicationMembers.get(APP.id()).add(foreign);
        QueryParamEntity query = QueryParamEntity.of();
        query.toQuery().is("id", allowed).or("id", foreign);
        assertEquals(Set.of(allowed), memberIds(query(true, query)));

        assertThrows(ValidationException.class,
            () -> inApplication(operator, members.bind(team.getId(), List.of(allowed, foreign))).block(TIMEOUT));
        assertTrue(commands.stream().noneMatch(BindDimensionUsersCommand.class::isInstance));
        assertEquals(Set.of(), teamMembers.getOrDefault(team.getId(), Set.of()));
    }

    @Test
    void userQueryFunctionPermissionDoesNotGrantUserAssetsOutsideTheOrdinaryRange() {
        String notGranted = applicationUser(APP.id());
        operator = applicationOperator("creator");
        assertTrue(operator.hasPermission("user", "query"));
        assertThrows(ValidationException.class,
            () -> inApplication(operator, members.bind(team.getId(), List.of(notGranted))).block(TIMEOUT));
        assertTrue(commands.stream().noneMatch(BindDimensionUsersCommand.class::isInstance));
    }

    @Test
    void anotherApplicationsTeamIsRejectedBeforeSendingAnyRemoteCommand() {
        DemoInspectionTeamEntity foreign = team(null);
        bind(APP_TYPE, OTHER_APP, DemoAssetType.TEAM_TYPE_ID, foreign.getId());
        assertThrows(AccessDenyException.class,
            () -> inApplication(operator, members.query(foreign.getId(), QueryParamEntity.of(), false)).block(TIMEOUT));
        assertTrue(commands.isEmpty());
    }

    private PagerResult<DemoInspectionTeamMember> query(boolean candidates, QueryParamEntity query) {
        return inApplication(operator, members.query(team.getId(), query, candidates)).block(TIMEOUT);
    }

    private static Set<String> memberIds(PagerResult<DemoInspectionTeamMember> result) {
        assertNotNull(result);
        return result.getData().stream().map(DemoInspectionTeamMember::getId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private TestAuthentication applicationOperator() {
        return applicationOperator(APP_TYPE);
    }

    private TestAuthentication applicationOperator(String userAccess) {
        TestAuthentication authentication = new TestAuthentication("ordinary-application-manager");
        authentication.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        authentication.addPermission("user", "query");
        authentication.addPermission("dimension", "save", "delete");
        authentication.addDimension("tenant", TENANT);
        authentication.addDimension("role", "ordinary-application-role", Map.of("tenantId", TENANT, "dataAccess", List.of(
            Map.of("assetType", DemoAssetType.TEAM_TYPE_ID, "accesses", List.of(Map.of("supportId", "tenant"))),
            Map.of("assetType", "user", "accesses", List.of(Map.of("supportId", userAccess))))));
        authentication.addDimension(APP_TYPE, APP.id(), Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TEAM_TYPE_ID, "SELECT_AND_BIND", "user", "SELECT")));
        return authentication;
    }

    private String applicationUser(String applicationId) {
        DemoMemberUserEntity user = new DemoMemberUserEntity();
        user.setId("member-" + UUID.randomUUID());
        user.setName("巡检员-" + user.getId());
        user.setUsername(user.getId());
        user.setCreatorId("project-user-directory-owner");
        users.save(user).contextWrite(TenantHolder.makeCurrent(TENANT)).block(TIMEOUT);
        bind(APP_TYPE, applicationId, "user", user.getId());
        applicationMembers.computeIfAbsent(applicationId, ignored -> new HashSet<>()).add(user.getId());
        return user.getId();
    }

    private <T> Mono<T> inApplication(Authentication authentication, Mono<T> operation) {
        return Mono.defer(() -> {
            AtomicReference<T> result = new AtomicReference<>();
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/demo/inspection-team")
                .header(AssetScopeWebFilter.HEADER, APP.type() + ':' + APP.id()).build());
            return new AssetScopeWebFilter(scopeResolver)
                .filter(exchange, ignored -> operation.doOnNext(result::set).then())
                .contextWrite(context -> context.put(Authentication.class, authentication))
                .contextWrite(TenantHolder.makeCurrent(TENANT))
                .then(Mono.defer(() -> Mono.justOrEmpty(result.get())));
        });
    }

    private class LocalAuthCommands implements CommandSupportManagerProvider {
        public String getProvider() { return SdkServices.authService; }

        public Mono<? extends CommandSupport> getCommandSupport(String module, Map<String, Object> options) {
            assertTrue(module.equals(AuthCommandSupportTypes.user) || module.equals(SdkServices.authService));
            return Mono.just(new AbstractCommandSupport() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Command<R> command) {
                    if (command instanceof QueryPagerCommand<?> query) {
                        return (R) queryUsers(query).collectList().map(data -> new PagerResult<>(data.size(), data));
                    }
                    if (command instanceof QueryListCommand<?> query) return (R) queryUsers(query);
                    if (command instanceof BindDimensionUsersCommand bind) {
                        return (R) permitted(command, "dimension", "save").then(Mono.fromSupplier(() -> {
                            bind.getBindings().forEach(binding -> teamMembers
                                .computeIfAbsent(binding.getDimensionId(), ignored -> new HashSet<>()).add(binding.getUserId()));
                            return bind.getBindings().size();
                        }));
                    }
                    if (command instanceof UnbindDimensionUsersCommand unbind) {
                        return (R) permitted(command, "dimension", "delete").then(Mono.fromSupplier(() -> {
                            Set<String> current = teamMembers.getOrDefault(unbind.getId(), new HashSet<>());
                            int before = current.size();
                            current.removeAll(unbind.getUsers());
                            return before - current.size();
                        }));
                    }
                    throw new AssertionError("Unexpected SDK command " + command.getClass());
                }
            });
        }

        private Mono<Void> permitted(Command<?> command, String resource, String action) {
            return Authentication.currentReactive().flatMap(authentication -> {
                assertEquals(operator.getUser().getId(), authentication.getUser().getId());
                commands.add(command);
                return authentication.hasPermission(resource, action) ? Mono.empty()
                    : Mono.error(AccessDenyException.NoStackTrace::new);
            });
        }

        private Flux<UserInfo> queryUsers(QueryCommand<?, ?> command) {
            return permitted(command, "user", "query").thenMany(Flux.defer(() -> {
                QueryParamEntity transferred;
                try {
                    transferred = objectMapper.readValue(objectMapper.writeValueAsBytes(command.asQueryParam()), QueryParamEntity.class);
                } catch (java.io.IOException error) {
                    return Flux.error(() -> error);
                }
                // 认证维度关系不在 demo 库；仅翻译这两个未改变的业务成员条件，资产条件由正式 SQL Builder 处理。
                translateMembershipTerms(transferred.getTerms());
                transferred.setPaging(false);
                return users.createQuery().setParam(transferred).fetch().map(user -> {
                    UserInfo result = new UserInfo();
                    result.setId(user.getId());
                    result.setName(user.getName());
                    result.setUsername(user.getUsername());
                    return result;
                });
            }));
        }

        private void translateMembershipTerms(List<Term> terms) {
            for (Term term : terms) {
                if ("in-dimension".equals(term.getTermType())) {
                    String type = term.getOptions().get(0);
                    Map<String, Set<String>> memberships = APP_TYPE.equals(type) ? applicationMembers : teamMembers;
                    Set<String> ids = memberships.getOrDefault(String.valueOf(term.getValue()), Set.of());
                    term.setTermType(term.getOptions().contains("not") ? "nin" : "in");
                    term.setOptions(List.of());
                    term.setValue(ids.isEmpty() ? List.of("missing-member") : new ArrayList<>(ids));
                }
                translateMembershipTerms(term.getTerms());
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableEasyormRepository("org.jetlinks.pro.demo.memberfixture")
    static class UserFixtures {
        @Bean
        BusinessApplicationAssetsAccessSupport applicationAssetAccess() {
            return new BusinessApplicationAssetsAccessSupport();
        }

        @Bean
        CreatorAssetsAccessSupport creatorAssetAccess(AssetManager assets) {
            return new CreatorAssetsAccessSupport(assets);
        }

        @Bean
        AssetSupplier memberUsers(ReactiveRepository<DemoMemberUserEntity, String> users) {
            return new AssetSupplier() {
                public List<AssetType> getTypes() { return List.of(AssetType.of("user")); }

                public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> ids) {
                    return users.findById(ids.stream().map(String::valueOf).toList())
                        .map(user -> new DefaultAsset(user.getId(), user.getName(), AssetType.of("user")).withEntity(user));
                }
            };
        }
    }
}
