package org.jetlinks.project.example.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.crud.query.QueryHelper;
import org.jetlinks.core.annotation.command.CommandHandler;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.entity.ExtendedEntity;
import org.jetlinks.sdk.server.commons.cmd.QueryPagerCommand;
import org.jetlinks.sdk.server.commons.cmd.SaveCommand;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author gyl
 * @since 2.3
 */
@Component
@AllArgsConstructor
@CommandService(id = "exampleService:extended", name = "示例拓展服务", description = "填写对应描述")
public class ExampleExtendedCommandSupport {

    private final ReactiveRepository<ExampleEntity, String> exampleRepository;
    private final ReactiveRepository<ExtendedEntity, String> extRepository;
    private final QueryHelper queryHelper;


    //已有命令
    @CommandHandler(outputSpec = QueryPagerOutputSpec.class)
    public Mono<PagerResult<ExtendedEntity>> queryInfo(QueryPagerCommand<ExtendedEntity> command) {
        return QueryHelper.queryPager(
            command.asQueryParam(),
            extRepository::createQuery
        );
    }

    @CommandHandler
    public Flux<ExampleExtendedInfo> saveInfo(SaveCommand<ExampleExtendedInfo> command) {
        SaveCommand.InputSpec<ExampleExtendedInfo> input = command.as(SaveCommand.InputSpec.class);
        if (input.getData() == null) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(input.getData())
            .collectMap(ExampleExtendedInfo::toEntity, ExampleExtendedInfo::getExtList)
            .flatMap(maps -> Flux
                .merge(
                    exampleRepository.save(maps.keySet()),
                    Flux
                        .fromIterable(maps.values())
                        .flatMapIterable(Function.identity())
                        .as(extRepository::save)
                )
                .then())
            .thenMany(Flux.fromIterable(input.getData()));

    }


    //自定义命令
    @CommandHandler
    public Flux<ExampleExtendedInfo> queryExampleInfo(QueryExampleInfoCommand command) {
        QueryExampleInfoCommand.InputSpec inputSpec = command.getInputSpec();
        return queryHelper
            .select(ExampleExtendedInfo.class)
            .all(ExampleEntity.class)
            .all(ExtendedEntity.class, ExampleExtendedInfo::setExtList)
            .from(ExampleEntity.class)
            .leftJoin(ExtendedEntity.class, spec -> spec.is(ExtendedEntity::getExampleId, ExampleEntity::getId))
            .where(inputSpec.toQueryParam())
            .fetch()
            .filter(info -> info.getExtList() != null
                && inputSpec.getExtendedSize() == info.getExtList().size());
    }


    @Getter
    @Setter
    public static class QueryPagerOutputSpec extends QueryPagerCommand.OutputSpec<ExtendedEntity> {
    }


}
