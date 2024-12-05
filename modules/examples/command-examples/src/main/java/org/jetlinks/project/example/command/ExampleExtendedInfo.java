package org.jetlinks.project.example.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.entity.ExtendedEntity;

import java.util.List;

/**
 * @author gyl
 * @since 2.3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExampleExtendedInfo {

    @Schema(title = "id")
    private String id;

    @Schema(title = "名称")
    private String name;

    @Schema(title = "拓展信息")
    private List<ExtendedEntity> extList;

    public void setExtList(List<ExtendedEntity> extList) {
        this.extList = extList;
    }

    public ExampleEntity toEntity() {
        return FastBeanCopier.copy(this, new ExampleEntity());
    }
}
