package org.jetlinks.pro.crud.service;

import org.hswebframework.web.crud.service.GenericCrudService;
import org.jetlinks.pro.crud.entity.ExampleEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExampleService extends GenericCrudService<ExampleEntity, String> {


    public List<ExampleEntity> queryByName(String name) {
        return createQuery()
            .where()
            .is(ExampleEntity::getName, name)
            .fetch();
    }
}
