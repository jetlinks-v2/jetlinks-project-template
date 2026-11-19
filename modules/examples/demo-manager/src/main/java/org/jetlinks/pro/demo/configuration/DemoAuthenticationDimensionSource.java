package org.jetlinks.pro.demo.configuration;

import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.pro.authorize.dimension.AuthenticationDimensionSource;
import org.jetlinks.pro.authorize.dimension.SystemAuthenticationDimensionSource;
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.pro.demo.asset.DemoAssetType;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.auth.AuthCommandSupportTypes;
import org.jetlinks.sdk.server.auth.UserInfo;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Set;

/**
 * 独立示例服务通过认证服务核实巡检班组的实时成员关系。
 * 仅由独立启动入口导入，避免单体服务扫描后覆盖本地 system 来源。
 * 查询保留当前调用身份与 Scope；不注入用户资产范围，避免维度解析递归。
 */
public class DemoAuthenticationDimensionSource implements AuthenticationDimensionSource {
    @Override
    public String getId() {
        return SystemAuthenticationDimensionSource.ID;
    }

    @Override
    public Flux<String> getDimensionIds(String userId, String typeId, Collection<String> snapshotIds) {
        return Flux.defer(() -> {
            if (snapshotIds.isEmpty()) {
                return Flux.empty();
            }
            if (!DemoAssetType.TEAM_TYPE_ID.equals(typeId)) {
                return Flux.error(() -> new IllegalStateException(
                    "Demo authentication dimension source only supports " + DemoAssetType.TEAM_TYPE_ID));
            }
            return Flux.fromIterable(snapshotIds).concatMap(dimensionId -> {
                QueryParamEntity query = QueryParamEntity.of().noPaging();
                query.setIncludes(Set.of("id"));
                query.toQuery().is("id", userId)
                    .and("id", "in-dimension$" + DemoAssetType.TEAM_TYPE_ID, dimensionId);
                return CommandSupportManagerProviders
                    .getCommandSupport(SdkServices.authService, AuthCommandSupportTypes.user)
                    .flatMapMany(support -> support.execute(QueryListCommand.of(UserInfo.class).withQueryParam(query)))
                    .any(user -> userId.equals(user.getId()))
                    .filter(Boolean::booleanValue)
                    .map(ignored -> dimensionId);
            });
        });
    }
}
