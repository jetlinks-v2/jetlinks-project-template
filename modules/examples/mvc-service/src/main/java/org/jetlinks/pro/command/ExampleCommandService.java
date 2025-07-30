package org.jetlinks.pro.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.pro.annotation.command.CommandService;
import org.jetlinks.pro.command.crud.CrudCommandHandler;
import org.jetlinks.pro.crud.entity.ExampleEntity;
import org.jetlinks.pro.crud.service.ReactiveExampleService;
import org.springframework.stereotype.Component;

/**
 * 自定义服务命令支持
 * <p>
 * 在其他服务可通过 {@link CommandSupportManagerProviders#getCommandSupport(String)} 进行RPC调用
 * 获取并执行相关命令,具体定义方式<a href="https://hanta.yuque.com/px7kg1/dev/ew1xvzmlgbzkc0hy#lbPG7">查看文档</a>
 * @see CommandSupportManagerProviders#getCommandSupport(String)
 * @see CommandSupportManagerProviders#getCommandSupportNow(String)
 */
@Component
@Getter
@CommandService(id = "exampleService", name = "示例命令服务支持")
@AllArgsConstructor
public class ExampleCommandService implements CrudCommandHandler<ExampleEntity, String> {

    private final ReactiveExampleService service;


}
