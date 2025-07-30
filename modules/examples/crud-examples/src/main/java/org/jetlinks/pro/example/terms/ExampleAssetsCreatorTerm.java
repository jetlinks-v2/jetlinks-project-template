package org.jetlinks.pro.example.terms;

import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.jetlinks.pro.assets.impl.terms.AssetsCreatorTermFragmentBuilder;
import org.jetlinks.pro.example.asset.ExampleAssetType;
import org.springframework.stereotype.Component;

@Component
public class ExampleAssetsCreatorTerm extends AssetsCreatorTermFragmentBuilder {
    public ExampleAssetsCreatorTerm() {
        super(ExampleAssetType.example);
    }

    @Override
    public String getTable(RDBColumnMetadata column) {
        return "example_crud";
    }
}