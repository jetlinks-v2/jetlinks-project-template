package org.jetlinks.pro.demo.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.assets.AssetPermission;
import org.jetlinks.pro.assets.EnumAssetType;
import org.jetlinks.pro.assets.crud.CrudAssetPermission;

import java.util.Arrays;
import java.util.List;

/**
 * 示例记录的资产契约；菜单资源、控制器与资产提供方使用同一标识。
 * 资产操作沿用平台 CRUD 权限，由资产提供方向平台注册。
 */
@AllArgsConstructor
@Getter
public enum DemoAssetType implements EnumAssetType {
    record("demo-record", "巡检计划", Arrays.asList(CrudAssetPermission.values())),
    inspectionTeam("demo-inspection-team", "巡检班组", Arrays.asList(CrudAssetPermission.values()));

    public static final String TYPE_ID = "demo-record";
    public static final String TEAM_TYPE_ID = "demo-inspection-team";

    private final String id;
    private final String name;
    private final List<AssetPermission> permissions;

}
