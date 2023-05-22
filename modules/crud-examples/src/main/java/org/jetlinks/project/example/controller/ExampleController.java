package org.jetlinks.project.example.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.entity.ExtendedEntity;
import org.jetlinks.project.example.enums.ExampleEnum;
import org.jetlinks.project.example.service.ExampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.persistence.Column;
import java.sql.JDBCType;

@RestController
@RequestMapping("/example/crud")
@AllArgsConstructor
@Getter
@Resource(id = "example", name = "增删改查演示")//资源定义,用于权限控制
@Tag(name = "增删改查演示") //swagger
public class ExampleController implements ReactiveServiceCrudController<ExampleEntity, String> {

    private final ExampleService service;

    private final QueryHelper queryHelper;

    @GetMapping("/_detail/_query")
    public Mono<PagerResult<ExampleInfo>> joinExample(QueryParamEntity query) {
        return queryHelper
            .select(ExampleInfo.class)
            .all(ExampleEntity.class)
            .all(ExtendedEntity.class, ExampleInfo::setExt)
            .from(ExampleEntity.class)
            .leftJoin(ExtendedEntity.class, spec -> spec.is(ExtendedEntity::getId, ExampleEntity::getId))
            .where(query)
            .fetchPaged();
    }

    @GetMapping("/_detail/_query_native")
    public Mono<PagerResult<ExampleInfo>> nativeJoinExample(QueryParamEntity query) {
        return queryHelper
            .select("select t.*,ext.* from example_crud t" +
                        " left join example_crud_ext ext on ext.example_id = t.id",
                    ExampleInfo::new)
            .where(query)
            .fetchPaged()
            .doOnNext(c->{
                System.out.println(c);
            });
    }


    @Getter
    @Setter
    public static class ExampleInfo {
        private String id;

        private String name;

        private ExampleEnum singleEnum;

        private ExampleEnum[] multiEnum;

        private ExtendedEntity ext;
    }
}
