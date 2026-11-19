package org.jetlinks.pro.demo.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.sdk.server.auth.UserInfo;

/** 班组成员选择器只展示识别用户所需的信息。 */
@Getter
@Setter
@Schema(description = "巡检班组成员")
public class DemoInspectionTeamMember {

    private String id;
    private String name;
    private String username;

    public static DemoInspectionTeamMember of(UserInfo user) {
        DemoInspectionTeamMember member = new DemoInspectionTeamMember();
        member.setId(user.getId());
        member.setName(user.getName());
        member.setUsername(user.getUsername());
        return member;
    }
}
