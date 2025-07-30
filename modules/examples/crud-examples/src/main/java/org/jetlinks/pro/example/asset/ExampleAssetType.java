package org.jetlinks.pro.example.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.assets.AssetPermission;
import org.jetlinks.pro.assets.EnumAssetType;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum ExampleAssetType implements EnumAssetType {
    example("自定义资产类型", Arrays.asList(CrudAssetPermission.values()));

    //常量定义,其他地方直接使用此常量
    public static final String TYPE_ID = "example";

    private final String name;

    private final List<AssetPermission> permissions;

    @Override
    public String getId() {
        return name();
    }
}