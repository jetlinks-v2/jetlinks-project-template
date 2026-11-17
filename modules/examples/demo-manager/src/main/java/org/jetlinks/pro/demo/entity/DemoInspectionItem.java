package org.jetlinks.pro.demo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "巡检清单项")
public class DemoInspectionItem {

    @NotBlank(message = "{validation.demo-inspection-item.name.required}")
    @Size(max = 64, message = "{validation.demo-inspection-item.name.max_length}")
    @Schema(description = "巡检项目，如消防通道检查")
    private String name;

    @NotBlank(message = "{validation.demo-inspection-item.requirement.required}")
    @Size(max = 512, message = "{validation.demo-inspection-item.requirement.max_length}")
    @Schema(description = "检查要求，如通道无堆放、出口可正常开启")
    private String requirement;
}
