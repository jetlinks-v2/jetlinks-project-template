package org.jetlinks.pro.example.asset;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.pro.assets.AssetSupplier;
import org.jetlinks.pro.assets.AssetType;
import org.jetlinks.pro.assets.DefaultAsset;
import org.jetlinks.pro.example.entity.ExampleEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@Component
public class ExampleAssetSupplier implements AssetSupplier {

    private final ReactiveRepository<ExampleEntity, String> repository;

    @Override
    public List<AssetType> getTypes() {
        return Arrays.asList(ExampleAssetType.values());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<DefaultAsset> getAssets(AssetType type, Collection<?> assetId) {
        return repository
            .findById((Collection<String>) assetId)
            .map(data -> new DefaultAsset(data.getId(), data.getName(), ExampleAssetType.example));
    }
}