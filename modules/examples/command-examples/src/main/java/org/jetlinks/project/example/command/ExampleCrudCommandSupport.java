package org.jetlinks.project.example.command;

import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.pro.command.crud.CrudCommandHandler;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.springframework.stereotype.Component;

/**
 * @author gyl
 * @since 2.3
 */
@Component
@CommandService(id = "exampleService:example", name = "示例服务", description = "示例实体增删改查能力提供")
public class ExampleCrudCommandSupport implements CrudCommandHandler<ExampleEntity, String> {

    private final ReactiveCrudService<ExampleEntity, String> service;

    public ExampleCrudCommandSupport(ReactiveCrudService<ExampleEntity, String> service) {
        this.service = service;
    }

    @Override
    public ReactiveCrudService<ExampleEntity, String> getService() {
        return service;
    }
}
