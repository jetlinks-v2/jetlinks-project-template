package org.jetlinks.pro.demo.memberfixture;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.ExtendableEntity;
import org.jetlinks.pro.assets.annotation.Asset;

import javax.persistence.Column;
import javax.persistence.Table;

/** 独立认证服务的用户投影，仅供本地权限查询回归使用。 */
@Getter
@Setter
@Table(name = "demo_member_test_user")
@Asset(type = "user")
public class DemoMemberUserEntity extends ExtendableEntity<String> {
    @Column(length = 64)
    private String name;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String creatorId;
}
