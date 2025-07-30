package org.jetlinks.pro.crud.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.pro.crud.enums.ExampleEnum;
import org.jetlinks.pro.io.excel.annotation.ExcelHeader;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Map;

@Table(name = "example_crud")
@Getter
@Setter
@Schema(description = "增删改查演示")
@EnableEntityEvent //开启实体类crud事件
public class ExampleEntity extends GenericEntity<String>
    //实现这2个接口标记此实体类需要记录创建人修改人信息，在crud时会自动填充对应的信息。
    implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @ExcelHeader("ID")
    public String getId() {
        return super.getId();
    }

    @Column(length = 64, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    @Schema(description = "名称")
    @ExcelHeader
    private String name;

    @Column
    @Schema(description = "说明")
    @ExcelHeader
    private String description;

    //平台ID长度统一64,创建人不为空,不可修改
    @Column(length = 64, nullable = false, updatable = false)
    @Schema(description = "创建人ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String creatorId;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    private Long createTime;

    @Column(length = 64, nullable = false)
    @Schema(description = "修改人ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String modifierId;

    @Column(nullable = false, updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "修改时间", accessMode = Schema.AccessMode.READ_ONLY)
    @ExcelHeader(dataType = CellDataType.DATE_TIME)
    private Long modifyTime;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "单选")
    @ExcelHeader
    private ExampleEnum singleEnum;

    @Column
    //使用枚举掩码来存储多选值,因此数据库中用bigint来存储
    @EnumCodec(toMask = true)
    @ColumnType(javaType = Long.class, jdbcType = JDBCType.BIGINT)
    @Schema(description = "多选")
    @ExcelHeader
    private ExampleEnum[] multiEnum;

    @Column
    //使用json字符串来存储map,因此数据库中使用LONGVARCHAR来存储
    @JsonCodec
    @ColumnType(javaType = String.class, jdbcType = JDBCType.LONGVARCHAR)
    @Schema(description = "其他信息")
    private Map<String, Object> others;
}
