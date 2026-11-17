package org.jetlinks.pro.demo;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.dynamic.DynamicDimensionState;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeActivation;
import org.jetlinks.pro.assets.scope.AssetScopeContext;
import org.jetlinks.pro.assets.scope.AssetScopeMaterializers;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.command.CommandSupportManagerProvider;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.DemoInspectionTeamMemberController;
import org.jetlinks.pro.demo.controller.response.DemoInspectionTeamMember;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.jetlinks.pro.demo.service.DemoInspectionTeamMemberService;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.auth.AuthCommandSupportTypes;
import org.jetlinks.sdk.server.auth.DimensionUserBinding;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 本地班组、投影及资产权限使用真实 H2；远端替身仅记录正式 SDK 命令和已核实的远端权限契约。 */
class DemoInspectionTeamMemberIntegrationTest extends DemoIntegrationSupport {
    private static final String APP = "inspection-app";
    private static final String FIRST = "application-member-1";
    private static final String SECOND = "application-member-2";
    private static final AssetScope APPLICATION = AssetScope.of(BusinessApplicationDimension.TYPE, APP);
    @Autowired AssetScopeMaterializers scopeMaterializers;
    private DemoInspectionTeamMemberService members;
    private DemoInspectionTeamEntity team;
    private RemoteCommands remote;
    private Disposable registration;

    @BeforeEach
    void prepareMembers() {
        members = new DemoInspectionTeamMemberService(teams, dimensions, bindings, scopeMaterializers);
        team = team(null);
        bind(BusinessApplicationDimension.TYPE, APP, DemoAssetType.TEAM_TYPE_ID, team.getId());
        remote = new RemoteCommands();
        registration = CommandSupportManagerProvider.supports.register(SdkServices.authService, remote);
    }

    @AfterEach
    void unregisterRemote() {
        registration.dispose();
    }

    @Test
    void pagedQueriesUseTypedApplicationAndTeamConditionsOutsideClientOrFilters() {
        TestAuthentication operator = administrator();
        QueryParamEntity requested = QueryParamEntity.of();
        requested.setPageIndex(2);
        requested.setPageSize(3);
        requested.setIncludes(Set.of("password", "salt", "creatorId"));
        requested.setExcludes(Set.of("name", "username"));
        requested.toQuery().is("name", "requested-name").or("username", "requested-login");
        remote.page.setPageIndex(2);
        remote.page.setPageSize(3);
        PagerResult<DemoInspectionTeamMember> result = run(operator, members.query(team.getId(), requested, false));
        assertEquals(7, result.getTotal());
        assertEquals(2, result.getPageIndex());
        assertEquals(3, result.getPageSize());
        assertEquals(FIRST, result.getData().get(0).getId());
        assertEquals("name-" + FIRST, result.getData().get(0).getName());
        assertEquals("login-" + FIRST, result.getData().get(0).getUsername());
        QueryParamEntity sent = query(remote.onlyCall().command());
        assertEquals(2, sent.getPageIndex());
        assertEquals(3, sent.getPageSize());
        assertEquals(Set.of("id", "name", "username"), sent.getIncludes());
        assertTrue(sent.getExcludes().isEmpty());
        assertTrue(leaves(sent).anyMatch(term -> "requested-name".equals(term.getValue())));
        assertTrue(leaves(sent).anyMatch(term -> "requested-login".equals(term.getValue()) && term.getType() == Term.Type.or));
        assertTerm(memberTerm(sent, BusinessApplicationDimension.TYPE), APP, BusinessApplicationDimension.TYPE);
        assertTerm(memberTerm(sent, DemoAssetType.TEAM_TYPE_ID), team.getId(), DemoAssetType.TEAM_TYPE_ID);
        assertContext(remote.onlyCall(), operator, Set.of(APPLICATION));

        remote.calls.clear();
        QueryParamEntity candidateQuery = QueryParamEntity.of();
        candidateQuery.setOrderBy("username desc");
        run(operator, members.query(team.getId(), candidateQuery, true));
        QueryParamEntity candidate = query(remote.onlyCall().command());
        assertTerm(memberTerm(candidate, BusinessApplicationDimension.TYPE), APP, BusinessApplicationDimension.TYPE);
        assertTerm(memberTerm(candidate, DemoAssetType.TEAM_TYPE_ID), team.getId(), DemoAssetType.TEAM_TYPE_ID, "not");
        assertEquals("username", candidate.getSorts().get(0).getName());
    }

    @Test
    void entireApplicationMembershipBatchMustCompleteBeforeOneIncrementalBindCommand() throws Exception {
        TestAuthentication operator = administrator();
        Sinks.Many<UserInfo> verified = Sinks.many().unicast().onBackpressureBuffer();
        remote.validation = verified.asFlux();
        CompletableFuture<Void> result = scoped(operator,
            members.bind(team.getId(), List.of(FIRST, SECOND, FIRST)), APPLICATION).toFuture();
        assertEquals(1, remote.calls.size());
        RemoteCall preflight = remote.onlyCall();
        assertInstanceOf(QueryListCommand.class, preflight.command());
        QueryParamEntity sent = query(preflight.command());
        assertFalse(sent.isPaging());
        assertTrue(leaves(sent).anyMatch(term -> "id".equals(term.getColumn())
            && List.of(FIRST, SECOND).equals(term.getValue())));
        assertTerm(memberTerm(sent, BusinessApplicationDimension.TYPE), APP, BusinessApplicationDimension.TYPE);
        verified.tryEmitNext(user(FIRST)).orThrow();
        assertEquals(0, remote.writeCalls().size());
        verified.tryEmitNext(user(SECOND)).orThrow();
        assertEquals(0, remote.writeCalls().size());
        verified.tryEmitComplete().orThrow();
        result.get(10, TimeUnit.SECONDS);
        assertEquals(2, remote.calls.size());
        BindDimensionUsersCommand command = assertInstanceOf(BindDimensionUsersCommand.class,
            remote.writeCalls().get(0).command());
        List<DimensionUserBinding> payload = command.getBindings();
        assertEquals(List.of(FIRST, SECOND), payload.stream().map(DimensionUserBinding::getUserId).toList());
        for (DimensionUserBinding binding : payload) {
            assertEquals(DemoAssetType.TEAM_TYPE_ID, binding.getDimensionTypeId());
            assertEquals(team.getId(), binding.getDimensionId());
            assertNull(binding.getRelation());
            assertNull(binding.getFeatures());
        }
        assertEquals(Set.of("bindings"), command.readable().keySet());
        remote.calls.forEach(call -> assertContext(call, operator, Set.of(APPLICATION)));
    }

    @Test
    void incompleteApplicationBatchIsRejectedWithoutSendingAnyWrite() {
        remote.validation = Flux.just(user(FIRST));
        assertThrows(ValidationException.class,
            () -> run(administrator(), members.bind(team.getId(), List.of(FIRST, SECOND))));
        assertEquals(1, remote.calls.size());
        assertTrue(remote.writeCalls().isEmpty());
    }

    @Test
    void unbindSendsOnlyTheExplicitTeamAndUsersWithoutAnApplicationUserLookup() {
        TestAuthentication operator = administrator();
        assertEquals(2, run(operator, members.unbind(team.getId(), List.of(FIRST, SECOND, FIRST))));
        UnbindDimensionUsersCommand command = assertInstanceOf(UnbindDimensionUsersCommand.class, remote.onlyCall().command());
        assertEquals(DemoAssetType.TEAM_TYPE_ID, command.getType());
        assertEquals(team.getId(), command.getId());
        assertEquals(List.of(FIRST, SECOND), command.getUsers());
        assertEquals(Set.of("type", "id", "users"), command.readable().keySet());
        assertContext(remote.onlyCall(), operator, Set.of(APPLICATION));
    }

    @Test
    void emptyUserIdsAreRejectedBeforeAnyRemoteCommand() {
        TestAuthentication operator = administrator();
        for (List<String> invalid : List.of(List.<String>of(), List.of(" "))) {
            assertThrows(ValidationException.class, () -> run(operator, members.bind(team.getId(), invalid)));
            assertThrows(ValidationException.class, () -> run(operator, members.unbind(team.getId(), invalid)));
        }
        assertTrue(remote.calls.isEmpty());
    }

    @Test
    void realLocalTeamAndItsApplicationOwnershipAreRequiredBeforeRemoteAccess() {
        TestAuthentication operator = administrator();
        DemoInspectionTeamEntity foreign = team(null);
        bind(BusinessApplicationDimension.TYPE, "other-application", DemoAssetType.TEAM_TYPE_ID, foreign.getId());
        deny(operator, members.query(foreign.getId(), QueryParamEntity.of(), false));
        deny(operator, members.bind(foreign.getId(), List.of(FIRST)));
        deny(operator, members.unbind(foreign.getId(), List.of(FIRST)));
        assertThrows(BusinessException.class,
            () -> run(operator, members.query("missing-team", QueryParamEntity.of(), false)));
        teams.createUpdate().set(DemoInspectionTeamEntity::getState, DynamicDimensionState.disabled)
            .where(DemoInspectionTeamEntity::getId, team.getId()).execute().block(TIMEOUT);
        assertThrows(BusinessException.class,
            () -> run(operator, members.query(team.getId(), QueryParamEntity.of(), false)));
        assertTrue(remote.calls.isEmpty());
    }

    @Test
    void missingTeamActionOrOrdinaryAssetPermissionDoesNotReachTheRemote() {
        TestAuthentication missingAction = platformPermissions(member(null), true, true, true);
        deny(missingAction, members.query(team.getId(), QueryParamEntity.of(), false));
        deny(missingAction, members.bind(team.getId(), List.of(FIRST)));
        TestAuthentication limited = platformPermissions(member(null, BusinessApplicationDimension.TYPE), true, true, true);
        limited.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        DemoInspectionTeamEntity outsideRange = team(null);
        // 目标属于另一个应用，同时真实普通 holder 只有当前应用范围。
        bind(BusinessApplicationDimension.TYPE, "other-application", DemoAssetType.TEAM_TYPE_ID, outsideRange.getId());
        deny(limited, members.query(outsideRange.getId(), QueryParamEntity.of(), false));
        assertTrue(remote.calls.isEmpty());
    }

    @Test
    void tenantMembershipAndItsOriginalGrantRemainMandatoryForAdministrators() {
        TestAuthentication operator = administrator();
        operator.addDimension("tenant", "tenant-a", Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TEAM_TYPE_ID, "SELECT")));
        bind("tenant", "tenant-a", DemoAssetType.TEAM_TYPE_ID, team.getId());
        AssetScope tenant = AssetScope.of("tenant", "tenant-a");
        PagerResult<DemoInspectionTeamMember> result = scoped(operator,
            members.query(team.getId(), QueryParamEntity.of(), false), APPLICATION, tenant).block(TIMEOUT);
        assertNotNull(result);
        assertContext(remote.onlyCall(), operator, Set.of(APPLICATION, tenant));
        remote.calls.clear();
        TestAuthentication otherTenant = administrator();
        otherTenant.addDimension("tenant", "tenant-b");
        assertThrows(AccessDenyException.class, () -> scoped(otherTenant,
            members.query(team.getId(), QueryParamEntity.of(), false), APPLICATION,
            AssetScope.of("tenant", "tenant-b")).block(TIMEOUT));
        TestAuthentication ignoredTenant = administrator();
        ignoredTenant.addDimension("tenant", "tenant-a", Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of(DemoAssetType.TEAM_TYPE_ID, "IGNORE")));
        assertThrows(AccessDenyException.class, () -> scoped(ignoredTenant,
            members.bind(team.getId(), List.of(FIRST)), APPLICATION, tenant).block(TIMEOUT));
        assertTrue(remote.calls.isEmpty());
    }

    @Test
    void existingRemoteGlobalPermissionsCannotBeBypassedByTheDemoService() {
        TestAuthentication noUserQuery = member(null);
        noUserQuery.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        platformPermissions(noUserQuery, false, true, true);
        deny(noUserQuery, members.query(team.getId(), QueryParamEntity.of(), false));
        assertInstanceOf(QueryPagerCommand.class, remote.onlyCall().command());
        remote.calls.clear();
        TestAuthentication noDimensionSave = member(null);
        noDimensionSave.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        platformPermissions(noDimensionSave, true, false, true);
        deny(noDimensionSave, members.bind(team.getId(), List.of(FIRST)));
        assertEquals(1, remote.writeCalls().size());
        assertContext(remote.writeCalls().get(0), noDimensionSave, Set.of(APPLICATION));
        remote.calls.clear();
        TestAuthentication noDimensionDelete = member(null);
        noDimensionDelete.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        platformPermissions(noDimensionDelete, true, true, false);
        deny(noDimensionDelete, members.unbind(team.getId(), List.of(FIRST)));
        assertInstanceOf(UnbindDimensionUsersCommand.class, remote.onlyCall().command());
        assertContext(remote.onlyCall(), noDimensionDelete, Set.of(APPLICATION));
    }

    @Test
    void remoteWriteFailureIsPropagatedWithoutRetryOrChangingTheCallingIdentity() {
        TestAuthentication operator = administrator();
        IllegalStateException failure = new IllegalStateException("remote command rejected");
        remote.writeFailure = failure;
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> run(operator, members.bind(team.getId(), List.of(FIRST)))));
        assertEquals(1, remote.writeCalls().size());
        assertContext(remote.writeCalls().get(0), operator, Set.of(APPLICATION));
    }

    @Test
    void allFourDemoHttpRoutesUseSdkCommandsAndOnlyExposeMemberIdentityFields() {
        TestAuthentication operator = administrator();
        // fixture 的 holder 构造包含同步等待，必须先在 JUnit 线程完成。
        AssetsHolder ordinary = holder(operator);
        AssetScopeActivation activation = scopeResolver.resolve(APPLICATION, operator).block(TIMEOUT);
        assertNotNull(activation);
        WebTestClient client = WebTestClient.bindToController(new DemoInspectionTeamMemberController(members))
            .webFilter((exchange, chain) -> chain.filter(exchange)
                .contextWrite(context -> context.put(AssetsHolder.class, ordinary))
                .contextWrite(activation.write())).build();
        String path = "/demo/inspection-team/" + team.getId() + "/members";
        client.post().uri(path + "/_query").bodyValue(QueryParamEntity.of()).exchange()
            .expectStatus().isOk().expectBody()
            .jsonPath("$.data[0].id").isEqualTo(FIRST)
            .jsonPath("$.data[0].name").isEqualTo("name-" + FIRST)
            .jsonPath("$.data[0].username").isEqualTo("login-" + FIRST)
            .jsonPath("$.data[0].type").doesNotExist()
            .jsonPath("$.data[0].creatorId").doesNotExist();
        client.post().uri(path + "/_candidates/_query").bodyValue(QueryParamEntity.of()).exchange().expectStatus().isOk();
        client.post().uri(path + "/_bind").bodyValue(List.of(FIRST)).exchange().expectStatus().is2xxSuccessful();
        client.post().uri(path + "/_unbind").bodyValue(List.of(FIRST)).exchange()
            .expectStatus().isOk().expectBody(Integer.class).isEqualTo(1);
        assertEquals(2, remote.writeCalls().size());
        remote.calls.forEach(call -> assertContext(call, operator, Set.of(APPLICATION)));
    }

    private TestAuthentication administrator() {
        TestAuthentication operator = member(null);
        operator.addPermission(DemoAssetType.TEAM_TYPE_ID, "query", "save");
        return platformPermissions(operator, true, true, true);
    }

    private static TestAuthentication platformPermissions(TestAuthentication operator, boolean userQuery,
                                                           boolean dimensionSave, boolean dimensionDelete) {
        // 这组用例保留原管理员演示范围；普通应用用户的真实资产约束由 MemberPermissionTest 执行 SQL 验证。
        operator.addDimension("role", "platform-user-directory", Map.of("dataAccess", List.of(
            Map.of("assetType", "user", "accesses", List.of(Map.of("supportId", "ignore"))))));
        if (userQuery) operator.addPermission("user", "query");
        List<String> dimensionActions = new ArrayList<>();
        if (dimensionSave) dimensionActions.add("save");
        if (dimensionDelete) dimensionActions.add("delete");
        if (!dimensionActions.isEmpty()) operator.addPermission("dimension", dimensionActions.toArray(String[]::new));
        return operator;
    }

    private <T> T run(Authentication operator, Mono<T> operation) {
        return scoped(operator, operation, APPLICATION).block(TIMEOUT);
    }

    private void deny(Authentication operator, Mono<?> operation) {
        assertThrows(AccessDenyException.class, () -> scoped(operator, operation, APPLICATION).block(TIMEOUT));
    }

    private static QueryParamEntity query(Command<?> command) {
        return assertInstanceOf(QueryCommand.class, command).asQueryParam();
    }

    private static Term memberTerm(QueryParamEntity query, String type) {
        return leaves(query).filter(term -> "in-dimension".equals(term.getTermType()) && term.getOptions().contains(type))
            .findFirst().orElseThrow();
    }

    private static java.util.stream.Stream<Term> leaves(QueryParamEntity query) {
        return query.getTerms().stream().flatMap(DemoInspectionTeamMemberIntegrationTest::leaves);
    }

    private static java.util.stream.Stream<Term> leaves(Term term) {
        return term.getTerms().isEmpty() ? java.util.stream.Stream.of(term) : term.getTerms().stream()
            .flatMap(DemoInspectionTeamMemberIntegrationTest::leaves);
    }

    private static void assertTerm(Term term, String id, String... options) {
        assertEquals("id", term.getColumn());
        assertEquals("in-dimension", term.getTermType());
        assertEquals(List.of(options), term.getOptions());
        assertEquals(id, term.getValue());
        assertInstanceOf(String.class, term.getValue());
        assertEquals(Term.Type.and, term.getType());
    }

    private static void assertContext(RemoteCall call, Authentication original, Set<AssetScope> scopes) {
        assertEquals(original.getUser().getId(), call.authentication().getUser().getId());
        assertEquals(scopes, Set.copyOf(call.scopes().getScopes()));
        assertTrue(call.authentication().getDimensions().stream().anyMatch(dimension -> "role".equals(dimension.getType().getId())));
        assertEquals(call.command() instanceof QueryCommand ? AuthCommandSupportTypes.user : SdkServices.authService,
            call.module());
    }

    private static UserInfo user(String id) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setName("name-" + id);
        user.setUsername("login-" + id);
        user.setType("private-user-type");
        user.setCreatorId("private-creator");
        return user;
    }

    private record RemoteCall(String module, Command<?> command, Authentication authentication, AssetScopeContext scopes) { }

    private static class RemoteCommands implements CommandSupportManagerProvider {
        private final List<RemoteCall> calls = new ArrayList<>();
        private final PagerResult<UserInfo> page = new PagerResult<>(7, List.of(user(FIRST)));
        private Flux<UserInfo> validation = Flux.just(user(FIRST), user(SECOND));
        private RuntimeException writeFailure;

        public String getProvider() { return SdkServices.authService; }

        public Mono<? extends CommandSupport> getCommandSupport(String module, Map<String, Object> options) {
            assertTrue(options.isEmpty());
            assertTrue(module.equals(AuthCommandSupportTypes.user) || module.equals(SdkServices.authService));
            return Mono.just(new AbstractCommandSupport() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Command<R> command) {
                    if (command instanceof QueryPagerCommand<?>) {
                        return (R) Mono.deferContextual(context -> {
                            RemoteCall call = capture(module, command, context);
                            return allowed(call, "user", "query").thenReturn(page);
                        });
                    }
                    if (command instanceof QueryListCommand<?>) {
                        return (R) Flux.deferContextual(context -> {
                            RemoteCall call = capture(module, command, context);
                            return allowed(call, "user", "query").thenMany(validation);
                        });
                    }
                    if (command instanceof BindDimensionUsersCommand bind) {
                        return (R) Mono.deferContextual(context -> {
                            RemoteCall call = capture(module, command, context);
                            return allowed(call, "dimension", "save").then(Mono.defer(() -> writeFailure == null
                                ? Mono.just(bind.getBindings().size()) : Mono.error(() -> writeFailure)));
                        });
                    }
                    if (command instanceof UnbindDimensionUsersCommand unbind) {
                        return (R) Mono.deferContextual(context -> {
                            RemoteCall call = capture(module, command, context);
                            return allowed(call, "dimension", "delete").thenReturn(unbind.getUsers().size());
                        });
                    }
                    throw new AssertionError("Unexpected SDK command: " + command.getClass());
                }
            });
        }

        private RemoteCall capture(String module, Command<?> command, ContextView context) {
            RemoteCall call = new RemoteCall(module, command, context.get(Authentication.class), AssetScopeContext.current(context));
            calls.add(call);
            return call;
        }

        private Mono<Void> allowed(RemoteCall call, String resource, String action) {
            // 远端 UserCommandSupport 与 AuthCommandSupportManagerProvider 已有的功能权限检查。
            return call.authentication().hasPermission(resource, action) ? Mono.empty()
                : Mono.error(AccessDenyException.NoStackTrace::new);
        }

        private RemoteCall onlyCall() {
            assertEquals(1, calls.size());
            return calls.get(0);
        }

        private List<RemoteCall> writeCalls() {
            return calls.stream().filter(call -> !(call.command() instanceof QueryCommand)).collect(Collectors.toList());
        }
    }
}
