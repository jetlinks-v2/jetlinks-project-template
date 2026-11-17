package org.jetlinks.pro.demo.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.crud.query.PagerQueryPolicy;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.pro.assets.AssetBindManager;
import org.jetlinks.pro.assets.AssetsHolder;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;
import org.jetlinks.pro.assets.dynamic.service.DynamicDimensionService;
import org.jetlinks.pro.assets.scope.AssetScope;
import org.jetlinks.pro.assets.scope.AssetScopeContext;
import org.jetlinks.pro.assets.scope.AssetScopeGrants;
import org.jetlinks.pro.assets.scope.AssetScopeMaterialization;
import org.jetlinks.pro.assets.scope.AssetScopeMaterializers;
import org.jetlinks.pro.assets.scope.AssetScopeStrategy;
import org.jetlinks.pro.authorize.BusinessApplicationDimension;
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.pro.demo.controller.response.DemoInspectionTeamMember;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.auth.AuthCommandSupportTypes;
import org.jetlinks.sdk.server.auth.DimensionUserBinding;
import org.jetlinks.sdk.server.auth.UserInfo;
import org.jetlinks.sdk.server.auth.cmd.BindDimensionUsersCommand;
import org.jetlinks.sdk.server.auth.cmd.UnbindDimensionUsersCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 班组成员示例通过正式认证命令访问独立认证服务，不读取认证库。
 * 命令沿用调用人的身份和活动 Scope，由认证服务继续校验平台用户查询与维度管理权限。
 */
@Service
public class DemoInspectionTeamMemberService {

    private static final String APPLICATION_SCOPE = BusinessApplicationDimension.TYPE;
    private static final String USER_ASSET_TYPE = "user";
    private static final String APPLICATION_MEMBER_TERM = "in-dimension$" + APPLICATION_SCOPE;
    private static final String TEAM_MEMBER_TERM = "in-dimension$" + DemoAssetType.TEAM_TYPE_ID;

    private final DemoInspectionTeamService teams;
    private final DynamicDimensionService dimensions;
    private final AssetBindManager bindings;
    private final AssetScopeMaterializers scopeMaterializers;

    public DemoInspectionTeamMemberService(DemoInspectionTeamService teams,
                                           DynamicDimensionService dimensions,
                                           AssetBindManager bindings,
                                           AssetScopeMaterializers scopeMaterializers) {
        this.teams = teams;
        this.dimensions = dimensions;
        this.bindings = bindings;
        this.scopeMaterializers = scopeMaterializers;
    }

    /** 固定应用与班组条件位于客户端筛选条件之外，OR 条件不能扩大成员集合。 */
    public Mono<PagerResult<DemoInspectionTeamMember>> query(String teamId,
                                                            QueryParamEntity query,
                                                            boolean candidates) {
        return assertTarget(teamId, Permission.ACTION_QUERY, CrudAssetPermission.read)
            .flatMap(applicationId -> Mono.deferContextual(context -> {
                QueryParamEntity scoped = PagerQueryPolicy
                    .clamp(PagerQueryPolicy.from(context).getMaxPageSize())
                    .normalize(query);
                scoped.setPageIndex(Math.max(0, scoped.getPageIndex()));
                useMemberProjection(scoped);
                if (scoped.getSorts().isEmpty()) {
                    scoped.toQuery().orderByAsc("name").orderByAsc("id");
                }
                scoped.toNestQuery()
                    .and("id", APPLICATION_MEMBER_TERM, applicationId)
                    .and("id", candidates ? TEAM_MEMBER_TERM + "$not" : TEAM_MEMBER_TERM, teamId);
                // 通用用户命令只校验 user/query；此入口还须取调用人的用户资产范围与应用成员集合的交集。
                Mono<PagerResult<UserInfo>> result = AssetsHolder
                    .injectQueryParam(scoped, USER_ASSET_TYPE, "id")
                    .flatMap(permitted -> CommandSupportManagerProviders
                        .getCommandSupport(SdkServices.authService, AuthCommandSupportTypes.user)
                        .flatMap(support -> support.execute(QueryPagerCommand.of(UserInfo.class).withQueryParam(permitted))));
                return QueryHelper.transformPageResult(result,
                    users -> Flux.fromIterable(users).map(DemoInspectionTeamMember::of).collectList());
            }));
    }

    /** 整批校验真实应用成员后发送一次增量命令，保留其他班组与应用关系。 */
    public Mono<Void> bind(String teamId, List<String> userIds) {
        return validateUserIds(userIds)
            .flatMap(ids -> assertTarget(teamId, Permission.ACTION_SAVE, CrudAssetPermission.save)
                .flatMap(applicationId -> assertApplicationMembers(applicationId, ids))
                .then(Mono.defer(() -> {
                    List<DimensionUserBinding> members = ids.stream().map(userId -> {
                        DimensionUserBinding binding = new DimensionUserBinding();
                        binding.setUserId(userId);
                        binding.setDimensionTypeId(DemoAssetType.TEAM_TYPE_ID);
                        binding.setDimensionId(teamId);
                        return binding;
                    }).toList();
                    return CommandSupportManagerProviders.getCommandSupport(SdkServices.authService)
                        .flatMap(support -> support.execute(new BindDimensionUsersCommand().setBindings(members)))
                        .then();
                })));
    }

    /** 明确限定类型、班组和用户，也允许清理已经退出应用的班组成员。 */
    public Mono<Integer> unbind(String teamId, List<String> userIds) {
        return validateUserIds(userIds)
            .flatMap(ids -> assertTarget(teamId, Permission.ACTION_SAVE, CrudAssetPermission.save)
                .then(Mono.defer(() -> CommandSupportManagerProviders.getCommandSupport(SdkServices.authService)
                    .flatMap(support -> support.execute(new UnbindDimensionUsersCommand()
                        .setType(DemoAssetType.TEAM_TYPE_ID).setId(teamId).setUsers(ids))))));
    }

    private Mono<String> assertTarget(String teamId, String action, CrudAssetPermission permission) {
        return Authentication.currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(authentication -> AssetScopeContext.currentReactive().flatMap(scopes -> {
                AssetScopeGrants grants = AssetScopeGrants.resolve(authentication, scopes);
                AssetScope application = scopes.getScope(APPLICATION_SCOPE).orElse(null);
                if (application == null || !authentication.hasPermission(DemoAssetType.TEAM_TYPE_ID, action)) {
                    return Mono.error(AccessDenyException.NoStackTrace::new);
                }
                return teams.findById(teamId)
                    .switchIfEmpty(Mono.error(() -> new BusinessException("error.demo.inspection_team.invalid")))
                    .then(dimensions.validateUserBinding(DemoAssetType.TEAM_TYPE_ID, List.of(teamId)))
                    // 当前入口是班组资产权限的唯一 owner；远端用户查询不能代替目标班组授权。
                    .then(AssetsHolder.assertPermission(DemoAssetType.inspectionTeam, teamId, permission))
                    .then(assertTenantMembership(authentication, grants, teamId))
                    // allData 不能代替应用归属证明；使用稳定业务 ID 精确核对应用资产绑定。
                    .then(bindings.getBindings(APPLICATION_SCOPE, List.of(application.id()),
                        DemoAssetType.inspectionTeam, List.of(teamId)).hasElements())
                    .flatMap(owned -> owned ? Mono.just(application.id())
                        : Mono.error(AccessDenyException.NoStackTrace::new));
            }));
    }

    private Mono<Void> assertTenantMembership(Authentication authentication, AssetScopeGrants grants, String teamId) {
        AssetScopeGrants.ResolvedGrant tenant = grants.getGrant("tenant").orElse(null);
        if (tenant == null) {
            return Mono.empty();
        }
        AssetScopeStrategy strategy;
        try {
            strategy = tenant.grant().requireStrategy(DemoAssetType.TEAM_TYPE_ID);
        } catch (IllegalArgumentException error) {
            return Mono.error(AccessDenyException.NoStackTrace::new);
        }
        if (strategy == AssetScopeStrategy.IGNORE) {
            return Mono.error(AccessDenyException.NoStackTrace::new);
        }
        // 复用官方租户归属 materializer，不因普通管理员权限跳过租户范围，也不改写 Grant。
        return scopeMaterializers.materialize(new AssetScopeMaterialization(authentication,
                tenant.dimension(), tenant.grant(), DemoAssetType.inspectionTeam, strategy))
            .switchIfEmpty(Mono.error(AccessDenyException.NoStackTrace::new))
            .flatMap(constraint -> constraint.holder()
                .assertPermission(DemoAssetType.inspectionTeam, List.of(teamId), false));
    }

    private Mono<Void> assertApplicationMembers(String applicationId, List<String> userIds) {
        QueryParamEntity query = QueryParamEntity.of().noPaging();
        useMemberProjection(query);
        query.toQuery().in("id", userIds).and("id", APPLICATION_MEMBER_TERM, applicationId);
        // 与候选列表使用同一资产权限，不能通过直接提交不可见的用户 ID 绕过选择范围。
        return AssetsHolder.injectQueryParam(query, USER_ASSET_TYPE, "id")
            .flatMapMany(permitted -> CommandSupportManagerProviders
                .getCommandSupport(SdkServices.authService, AuthCommandSupportTypes.user)
                .flatMapMany(support -> support.execute(QueryListCommand.of(UserInfo.class).withQueryParam(permitted))))
            .map(UserInfo::getId)
            .collect(Collectors.toSet())
            .flatMap(found -> found.containsAll(userIds) ? Mono.empty()
                : Mono.error(() -> new ValidationException(
                    "userIds", "error.demo.inspection_team.member_not_in_application")));
    }

    private void useMemberProjection(QueryParamEntity query) {
        query.setIncludes(new LinkedHashSet<>(Set.of("id", "name", "username")));
        query.setExcludes(new LinkedHashSet<>());
    }

    private Mono<List<String>> validateUserIds(List<String> userIds) {
        return Mono.defer(() -> {
            // 空条件不能到达通用解绑命令；批量去重在校验和写入前统一完成。
            if (CollectionUtils.isEmpty(userIds) || userIds.stream().anyMatch(StringUtils::isBlank)) {
                return Mono.error(() -> new ValidationException(
                    "userIds", "error.demo.inspection_team.member_ids_required"));
            }
            return Mono.just(List.copyOf(new LinkedHashSet<>(userIds)));
        });
    }
}
