package org.jetlinks.project.command;

import com.google.common.collect.Lists;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.pro.test.spring.TestJetLinksController;
import org.jetlinks.project.example.command.ExampleCrudCommandSupport;
import org.jetlinks.project.example.command.ExampleExtendedCommandSupport;
import org.jetlinks.project.example.command.ExampleExtendedInfo;
import org.jetlinks.project.example.entity.ExampleEntity;
import org.jetlinks.project.example.entity.ExtendedEntity;
import org.jetlinks.sdk.server.commons.cmd.QueryByIdCommand;
import org.jetlinks.sdk.server.commons.cmd.SaveCommand;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gyl
 * @since 2.3
 */
@WebFluxTest({ExampleCrudCommandSupport.class, ExampleExtendedCommandSupport.class})
public class ExampleCommandSupportTest extends TestJetLinksController {

    @Test
    void testExtended() {
        {
            CommandSupportManagerProviders
                .getCommandSupport("exampleService:extended")
                .flatMapMany(CommandSupport::getCommandMetadata)
                .doOnNext(System.out::println)
                .as(StepVerifier::create)
                .expectNextCount(3)
                .verifyComplete();
        }

        //保存示例实体及拓展信息
        {
            SaveCommand<ExampleExtendedInfo> command = new SaveCommand<>();
            command.withData(
                new ExampleExtendedInfo("id1",
                                        "name1",
                                        Lists.newArrayList(new ExtendedEntity("id1", "ext1-1"),

                                                           new ExtendedEntity("id1", "ext1-2"))),
                new ExampleExtendedInfo("id2",
                                        "name2",
                                        Lists.newArrayList(new ExtendedEntity("id2", "ext2-1"))));
            CommandSupportManagerProviders
                .getCommandSupport("exampleService:extended")
                .flatMapMany(s -> s.execute(command))
                .doOnNext(System.out::println)
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
        }

        //查询拓展个数=2的示例实体信息
        {
            Map<String, Object> input = new HashMap<>();
            input.put("extendedSize", 2);
            CommandSupportManagerProviders
                .getCommandSupport("exampleService:extended")
                .flatMapMany(s -> s.executeToFlux("QueryExampleInfo", input))
                .doOnNext(System.out::println)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
        }

        //查询id=id2的示例实体
        {
            CommandSupportManagerProviders
                .getCommandSupport("exampleService:example")
                .flatMap(s -> s.executeToMono(new QueryByIdCommand<ExampleEntity>().withId("id2")))
                .doOnNext(System.out::println)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
        }


    }

}
