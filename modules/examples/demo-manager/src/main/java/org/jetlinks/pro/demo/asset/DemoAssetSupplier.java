package org.jetlinks.pro.demo.asset;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.assets.AssetSupplier;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.assets.DefaultAsset;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
import org.jetlinks.pro.demo.entity.DemoInspectionTeamEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;

/**
 * 向平台注册示例记录资产，并按记录 ID 批量提供资产名称。
 * 此提供方仅描述资产，应用归属和访问校验由 AssetsHolder 负责。
 */
@Component
@AllArgsConstructor
public class DemoAssetSupplier implements AssetSupplier {

    private final ReactiveRepository<DemoRecordEntity, String> repository;
    private final ReactiveRepository<DemoInspectionTeamEntity, String> teamRepository;

    @Override
    public List<AssetType> getTypes() {
        return List.of(DemoAssetType.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        if (DemoAssetType.inspectionTeam.isSameType(type.getId())) {
            return teamRepository
                .findById((Collection<String>) assetId)
                // 租户按 ID 校验时从资产扩展字段读取归属。
                .map(team -> new DefaultAsset(team.getId(), team.getName(), DemoAssetType.inspectionTeam)
                    .withEntity(team));
        }
        if (!DemoAssetType.record.isSameType(type.getId())) {
            return Flux.empty();
        }
        return repository
            .findById((Collection<String>) assetId)
            .map(record -> new DefaultAsset(record.getId(), record.getName(), DemoAssetType.record)
                .withEntity(record));
    }
}
