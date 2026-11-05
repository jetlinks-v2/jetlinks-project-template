package org.jetlinks.pro.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.pro.assets.annotation.Asset;
import org.jetlinks.pro.demo.asset.DemoAssetType;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * 应用示例的独立业务记录，由标准 CRUD 维护并发布实体事件。
 * 应用及租户范围由平台资产权限管理，实体不维护另一套归属过滤字段。
 */
@Getter
@Setter
@Table(name = "demo_record")
@Asset(type = DemoAssetType.TYPE_ID)
@EnableEntityEvent
@Schema(description = "示例记录")
public class DemoRecordEntity extends ExtendableEntity<String> {

    @Column(length = 64, nullable = false)
    @NotBlank(message = "{validation.demo-record.name.required}")
    @Size(max = 64, message = "{validation.demo-record.name.max_length}")
    @Schema(description = "名称")
    private String name;

    @Column(length = 512)
    @Size(max = 512, message = "{validation.demo-record.description.max_length}")
    @Schema(description = "说明")
    private String description;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Long createTime;
}
