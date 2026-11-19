package org.jetlinks.pro.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.assets.annotation.Asset;
import org.jetlinks.pro.demo.asset.DemoAssetType;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.List;

/**
 * 应用示例的独立业务记录，由标准 CRUD 维护并发布实体事件。
 * 应用及租户范围由平台资产权限管理，实体不维护另一套归属过滤字段。
 */
@Getter
@Setter
@Table(name = "demo_record")
@Asset(type = DemoAssetType.TYPE_ID)
@EnableEntityEvent
@Schema(description = "巡检计划")
// 显式忽略创建字段，避免只读属性进入父类扩展属性后在响应中回显。
@JsonIgnoreProperties(value = {"creatorId", "createTime"}, allowGetters = true)
public class DemoRecordEntity extends ExtendableEntity<String> implements RecordCreationEntity {

    @Column(length = 64, nullable = false)
    @NotBlank(message = "{validation.demo-record.name.required}")
    @Size(max = 64, message = "{validation.demo-record.name.max_length}")
    @Schema(description = "名称")
    private String name;

    @Column(length = 512)
    @Size(max = 512, message = "{validation.demo-record.description.max_length}")
    @Schema(description = "说明")
    private String description;

    @Column(length = 64, nullable = false)
    @NotBlank(message = "{validation.demo-record.inspection_team.required}")
    @Size(max = 64, message = "{validation.demo-record.inspection_team.max_length}")
    @Schema(description = "负责巡检班组ID")
    private String inspectionTeamId;

    @Column(nullable = false)
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Valid
    @NotEmpty(message = "{validation.demo-record.inspection_items.required}")
    @Schema(description = "巡检清单，清单项数量计入租户巡检项保有量")
    private List<@NotNull(message = "{validation.demo-record.inspection_item.required}") DemoInspectionItem> inspectionItems;

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
