package org.jetlinks.pro.demo.asset;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.assets.AssetSupplier;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.assets.DefaultAsset;
import org.jetlinks.pro.demo.entity.DemoRecordEntity;
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

    @Override
    public List<AssetType> getTypes() {
        return List.of(DemoAssetType.record);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        return repository
            .findById((Collection<String>) assetId)
            .map(record -> new DefaultAsset(record.getId(), record.getName(), DemoAssetType.record));
    }
}
