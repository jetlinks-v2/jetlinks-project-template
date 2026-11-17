package org.jetlinks.pro.demo.asset;

import jakarta.annotation.Nonnull;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.TermFragmentBuilder;
import org.jetlinks.pro.assets.AssetBindRequest;
import org.jetlinks.pro.assets.AssetTermBuilderProvider;
import org.jetlinks.pro.assets.AssetUnbindRequest;
import org.jetlinks.pro.assets.impl.direct.DirectAssetBindProvider;
import org.jetlinks.pro.assets.impl.direct.SimpleDirectAssetsTermFragmentBuilder;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 将班组资产 ID 映射到同名成员维度，供“所在巡检班组”控制班组自身。
 * Spring 注册到公共资产绑定和条件构造扩展点，成员、层级及应用 Scope 仍由平台求值。
 * 自身关系随实体存在且不可修改，只复用 Direct 的查询能力，不允许绑定操作改写主键。
 *
 * @see DirectAssetBindProvider
 * @see AssetTermBuilderProvider
 */
@Component
public class DemoInspectionTeamAssetBindProvider extends DirectAssetBindProvider implements AssetTermBuilderProvider {

    private static final Map<String, String> SELF_DIMENSION = Map.of(DemoAssetType.TEAM_TYPE_ID, "id");

    public DemoInspectionTeamAssetBindProvider(ReactiveRepository<DemoInspectionTeamEntity, String> repository) {
        super(repository, DemoAssetType.TEAM_TYPE_ID, SELF_DIMENSION);
    }

    @Override
    public Collection<TermFragmentBuilder> createTermBuilders(String tableName, String assetType) {
        if (!DemoAssetType.TEAM_TYPE_ID.equals(assetType)) {
            return List.of();
        }
        return List.of(new SimpleDirectAssetsTermFragmentBuilder(assetType, tableName, SELF_DIMENSION));
    }

    // 与组织自身的虚拟归属一致，绑定和解绑不能改变班组身份；其他维度仍走各自的绑定后端。
    @Override
    public Mono<Void> bindAssets(@Nonnull Publisher<AssetBindRequest> request) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> unbindAssets(@Nonnull Publisher<AssetUnbindRequest> requests) {
        return Mono.empty();
    }

    @Override
    public Mono<Integer> unbindAllAssets(@Nonnull Collection<?> assetIds) {
        return Mono.just(0);
    }
}
