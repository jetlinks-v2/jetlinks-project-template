package org.jetlinks.project.example;

import org.hswebframework.web.crud.entity.factory.EntityMappingCustomizer;
import org.hswebframework.web.crud.entity.factory.MapperEntityFactory;
import org.jetlinks.pro.auth.entity.UserDetail;
import org.jetlinks.pro.auth.entity.UserDetailEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDetailExtCustomizer implements EntityMappingCustomizer {
    @Override
    public void custom(MapperEntityFactory factory) {

        factory.addMapping(UserDetailEntity.class, UserDetailExtEntity::new);
        factory.addMapping(UserDetail.class, UserDetailExt::new);

    }
}
