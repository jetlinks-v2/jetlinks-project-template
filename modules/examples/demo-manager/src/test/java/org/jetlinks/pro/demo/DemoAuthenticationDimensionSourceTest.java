package org.jetlinks.pro.demo;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.core.command.AbstractCommandSupport;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeContext;
import org.jetlinks.pro.assets.scope.AssetScopeSources;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.authorize.dimension.AuthenticationDimensionConfiguration;
import org.jetlinks.pro.authorize.dimension.AuthenticationDimensionSource;
import org.jetlinks.pro.authorize.dimension.AuthenticationDimensions;
import org.jetlinks.pro.authorize.dimension.SystemAuthenticationDimensionSource;
import org.jetlinks.pro.command.CommandSupportManagerProvider;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.configuration.DemoAuthenticationDimensionSource;
import org.jetlinks.pro.demo.memberfixture.DemoMemberUserEntity;
import org.jetlinks.pro.test.web.TestAuthentication;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.auth.AuthCommandSupportTypes;
import org.jetlinks.sdk.server.auth.UserInfo;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 正式来源注册与快照求交不替换；远端命令 fixture 经 JSON 往返后由 H2 执行用户条件。 */
@SpringJUnitConfig({DemoIntegrationTestConfiguration.class, DemoInspectionTeamMemberPermissionTest.UserFixtures.class})
@TestPropertySource(properties = "spring.r2dbc.url=r2dbc:h2:mem:///demo-authentication-dimension-source;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoAuthenticationDimensionSourceTest extends DemoIntegrationSupport {
    private static final AssetScope APP = AssetScope.of(BusinessApplicationDimension.TYPE, "source-demo-app");
    private static final String TEAM_TYPE = DemoAssetType.TEAM_TYPE_ID;
    @Autowired
    private ReactiveRepository<DemoMemberUserEntity, String> users;
    private final Map<Membership, Set<String>> memberships = new HashMap<>();
    private final List<RemoteCall> calls = new ArrayList<>();
    private final RemoteCommands remote = new RemoteCommands();
    private AnnotationConfigApplicationContext sourceContext;
    private DemoAuthenticationDimensionSource source;
    private Disposable commandRegistration;
    private TestAuthentication operator;

    @BeforeEach
    void registerExplicitRemoteSource() {
        sourceContext = new AnnotationConfigApplicationContext(
            AuthenticationDimensionConfiguration.class, DemoAuthenticationDimensionSource.class);
        source = sourceContext.getBean(DemoAuthenticationDimensionSource.class);
        commandRegistration = CommandSupportManagerProvider.supports.register(SdkServices.authService, remote);
        operator = new TestAuthentication("source-user-" + UUID.randomUUID());
        operator.addPermission("user", "query");
        operator.addDimension(APP.type(), APP.id(), Map.of(AssetScopeSources.POLICY_OPTION_KEY,
            Map.of("user", "SELECT")));
        operator.addDimension("role", "source-test-role", Map.of("dataAccess", List.of(Map.of(
            "assetType", "user", "accesses", List.of(Map.of("supportId", APP.type()))))));
        createUser(operator.getUser().getId());
    }

    @AfterEach
    void releaseRegistrations() {
        if (commandRegistration != null) {
            commandRegistration.dispose();
        }
        if (sourceContext != null && sourceContext.isActive()) {
            sourceContext.close();
        }
    }

    @Test
    void realResolutionIntersectsCurrentTypedMembershipWithSnapshotOnEverySubscription() {
        String first = "team-first";
        String second = "team-second";
        String newTeam = "team-after-login";
        snapshot(TEAM_TYPE, first);
        snapshot(TEAM_TYPE, second);
        memberships.put(new Membership(TEAM_TYPE, first), new HashSet<>(Set.of(operator.getUser().getId())));
        memberships.put(new Membership(TEAM_TYPE, second), new HashSet<>(Set.of("another-user")));
        // 其他类型下同 ID 的关系不能让第二个巡检班组变成有效成员。
        memberships.put(new Membership("organization", second), Set.of(operator.getUser().getId()));
        Mono<Set<String>> sameResolution = resolved(TEAM_TYPE);
        assertEquals(Set.of(first), inApplication(sameResolution).block(TIMEOUT));
        assertEquals(2, calls.size());
        calls.forEach(this::assertTypedCurrentUserRequest);

        memberships.get(new Membership(TEAM_TYPE, first)).clear();
        memberships.get(new Membership(TEAM_TYPE, second)).add(operator.getUser().getId());
        memberships.put(new Membership(TEAM_TYPE, newTeam), Set.of(operator.getUser().getId()));
        assertEquals(Set.of(second), inApplication(sameResolution).block(TIMEOUT));
        assertEquals(4, calls.size());
        Set<String> queried = calls.stream().map(call -> String.valueOf(
            call.query().asQueryParam().getTerms().get(1).getValue())).collect(Collectors.toSet());
        assertEquals(Set.of(first, second), queried);
        calls.forEach(this::assertTypedCurrentUserRequest);
    }

    @Test
    void emptySnapshotDoesNotCallRemoteAndOtherSystemTypesFailClosed() {
        assertEquals(List.of(), inApplication(source.getDimensionIds(
            operator.getUser().getId(), TEAM_TYPE, List.of()).collectList()).block(TIMEOUT));
        assertTrue(calls.isEmpty());
        snapshot("organization", "same-business-id");
        memberships.put(new Membership(TEAM_TYPE, "same-business-id"), Set.of(operator.getUser().getId()));
        assertThrows(IllegalStateException.class, () -> inApplication(resolved("organization")).block(TIMEOUT));
        assertTrue(calls.isEmpty());
    }

    @Test
    void responseMustContainTheRequestedUserBeforeReturningSnapshotId() {
        remote.override = List.of(user("another-user"));
        assertEquals(List.of(), inApplication(source.getDimensionIds(operator.getUser().getId(), TEAM_TYPE,
            List.of("team-target")).collectList()).block(TIMEOUT));
        remote.override = List.of(user(operator.getUser().getId()));
        assertEquals(List.of("team-target"), inApplication(source.getDimensionIds(operator.getUser().getId(),
            TEAM_TYPE, List.of("team-target")).collectList()).block(TIMEOUT));
        assertEquals(2, calls.size());
        calls.forEach(this::assertTypedCurrentUserRequest);
    }

    @Test
    void permissionAndRpcFailuresPropagateWithoutIdentityChangesOrRetries() {
        snapshot(TEAM_TYPE, "error-team");
        for (RuntimeException failure : List.of(
            new AccessDenyException.NoStackTrace(), new IllegalStateException("remote unavailable"))) {
            calls.clear();
            remote.failure = failure;
            RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> inApplication(resolved(TEAM_TYPE)).block(TIMEOUT));
            assertSame(failure, thrown);
            assertEquals(1, calls.size());
            assertTypedCurrentUserRequest(calls.get(0));
        }
    }

    @Test
    void explicitAssemblyRegistersOnlyOneSourceAndPublicLifecycleReleasesIt() {
        assertEquals(SystemAuthenticationDimensionSource.ID, source.getId());
        assertEquals(1, sourceContext.getBeansOfType(AuthenticationDimensionSource.class).size());
        assertSame(source, AuthenticationDimensionSource.sources.get(source.getId()).orElseThrow());
        assertTrue(new ClassPathScanningCandidateComponentProvider(true)
            .findCandidateComponents("org.jetlinks.pro.demo.configuration").stream()
            .noneMatch(candidate -> DemoAuthenticationDimensionSource.class.getName().equals(candidate.getBeanClassName())));
        sourceContext.close();
        assertTrue(AuthenticationDimensionSource.sources.get(source.getId()).isEmpty());
    }

    private void snapshot(String type, String id) {
        operator.addDimension(type, id, Map.of(AuthenticationDimensions.SOURCE_OPTION, SystemAuthenticationDimensionSource.ID));
    }

    private Mono<Set<String>> resolved(String type) {
        return AuthenticationDimensions.resolve(operator, type).map(Dimension::getId).collect(Collectors.toSet());
    }

    private <T> Mono<T> inApplication(Mono<T> operation) {
        return scoped(operator, Mono.deferContextual(context -> operation.contextWrite(current ->
            current.put(CallerAuthentication.class, new CallerAuthentication(context.get(Authentication.class))))), APP);
    }

    private void createUser(String id) {
        DemoMemberUserEntity user = new DemoMemberUserEntity();
        user.setId(id);
        user.setName("巡检员");
        user.setUsername(id);
        users.save(user).block(TIMEOUT);
    }

    private static UserInfo user(String id) {
        UserInfo user = new UserInfo();
        user.setId(id);
        return user;
    }

    private void assertTypedCurrentUserRequest(RemoteCall call) {
        assertEquals(AuthCommandSupportTypes.user, call.module());
        assertEquals(operator.getUser().getId(), call.authentication().getUser().getId());
        assertSame(call.callerAuthentication(), call.authentication());
        assertEquals(APP, call.scopes().getScope(APP.type()).orElseThrow());
        QueryParamEntity query = call.query().asQueryParam();
        assertFalse(query.isPaging());
        assertEquals(Set.of("id"), query.getIncludes());
        assertEquals(2, query.getTerms().size());
        Term user = query.getTerms().get(0);
        Term team = query.getTerms().get(1);
        assertEquals(Term.Type.and, user.getType());
        assertEquals(Term.Type.and, team.getType());
        assertEquals("id", user.getColumn());
        assertEquals("eq", user.getTermType());
        assertEquals(operator.getUser().getId(), user.getValue());
        assertEquals("id", team.getColumn());
        assertEquals("in-dimension", team.getTermType());
        assertEquals(List.of(TEAM_TYPE), team.getOptions());
    }

    private record Membership(String type, String id) { }
    private record CallerAuthentication(Authentication value) { }
    private record RemoteCall(String module, QueryListCommand<?> query,
                              Authentication callerAuthentication, Authentication authentication,
                              AssetScopeContext scopes) { }

    private class RemoteCommands implements CommandSupportManagerProvider {
        private RuntimeException failure;
        private List<UserInfo> override;

        public String getProvider() {
            return SdkServices.authService;
        }

        public Mono<? extends CommandSupport> getCommandSupport(String module, Map<String, Object> options) {
            assertEquals(AuthCommandSupportTypes.user, module);
            assertTrue(options.isEmpty());
            return Mono.just(new AbstractCommandSupport() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Command<R> command) {
                    assertInstanceOf(QueryListCommand.class, command);
                    QueryListCommand<?> query = (QueryListCommand<?>) command;
                    return (R) Flux.deferContextual(context -> {
                        calls.add(new RemoteCall(module, query, context.get(CallerAuthentication.class).value(),
                            context.get(Authentication.class), AssetScopeContext.current(context)));
                        if (failure != null) {
                            return Flux.error(() -> failure);
                        }
                        if (override != null) {
                            return Flux.fromIterable(override);
                        }
                        QueryParamEntity transferred;
                        try {
                            transferred = objectMapper.readValue(objectMapper.writeValueAsBytes(query.asQueryParam()),
                                QueryParamEntity.class);
                        } catch (java.io.IOException error) {
                            return Flux.error(() -> error);
                        }
                        // 与现有命令 fixture 相同：仅替代远端成员关系，两个 AND 条件仍交 H2 执行。
                        for (Term term : transferred.getTerms()) {
                            if ("in-dimension".equals(term.getTermType())) {
                                Set<String> memberIds = memberships.getOrDefault(
                                    new Membership(term.getOptions().get(0), String.valueOf(term.getValue())), Set.of());
                                term.setTermType("in");
                                term.setOptions(List.of());
                                term.setValue(memberIds.isEmpty() ? List.of("missing-member") : new ArrayList<>(memberIds));
                            }
                        }
                        return users.createQuery().setParam(transferred).fetch().map(found -> user(found.getId()));
                    });
                }
            });
        }
    }
}
