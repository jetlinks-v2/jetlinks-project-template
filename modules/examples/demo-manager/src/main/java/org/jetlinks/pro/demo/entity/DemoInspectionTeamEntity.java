package org.jetlinks.pro.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.assets.annotation.Asset;
import org.jetlinks.pro.assets.dynamic.DynamicDimensionState;
import org.jetlinks.pro.demo.asset.DemoAssetType;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

/**
 * 班组业务数据；公共动态维度表只保存同事务生成的权限投影。
 */
@Getter
@Setter
@Table(name = "demo_inspection_team", indexes = @Index(name = "idx_demo_team_parent", columnList = "parent_id"))
@Asset(type = DemoAssetType.TEAM_TYPE_ID)
@EnableEntityEvent
@Schema(description = "巡检班组")
// 显式忽略创建字段，避免只读属性进入父类扩展属性后在响应中回显。
@JsonIgnoreProperties(value = {"creatorId", "createTime"}, allowGetters = true)
public class DemoInspectionTeamEntity extends ExtendableEntity<String> implements RecordCreationEntity {

    @Column(length = 64, nullable = false)
    @NotBlank(message = "{validation.demo-inspection-team.name.required}")
    @Size(max = 64, message = "{validation.demo-inspection-team.name.max_length}")
    @Schema(description = "班组名称")
    private String name;

    @Column(length = 512)
    @Size(max = 512, message = "{validation.demo-inspection-team.description.max_length}")
    @Schema(description = "班组职责说明")
    private String description;

    @Column(length = 64)
    @Size(max = 64, message = "{validation.demo-inspection-team.parent.max_length}")
    @Schema(description = "上级班组业务ID")
    private String parentId;

    @Column(length = 32, nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.VARCHAR)
    @DefaultValue("enabled")
    @Schema(description = "班组状态")
    private DynamicDimensionState state;

    // 标准创建信息同时为“自己创建的”资产范围提供持久化归属。
    @Column(length = 64, nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "创建人ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;
}
