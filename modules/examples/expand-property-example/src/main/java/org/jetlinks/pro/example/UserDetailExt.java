package org.jetlinks.pro.example;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.auth.entity.UserDetail;

@Getter
@Setter
public class UserDetailExt extends UserDetail {

    @Schema(description = "企业名称")
    private String corpName;

}
