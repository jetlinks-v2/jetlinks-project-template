package org.jetlinks.pro;

import io.swagger.v3.oas.annotations.Operation;
import org.hswebframework.web.authorization.Authentication;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.device.DeviceCommandSupportTypes;
import org.jetlinks.sdk.server.device.DeviceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/mvc-test")
public class TestController {


    // 获取当前用户权限信息
    @GetMapping("/me")
    public Authentication me() {
        return Authentication.current().orElse(null);
    }

    // 响应式方式获取当前用户权限信息
    @GetMapping("/reactive-me")
    public Mono<Authentication> reactiveMe() {
        return Authentication.currentReactive();
    }


    // 返回响应式流 , 前端可通过 application/x-ndjson 接收流式响应.
    @GetMapping("/stream")
    @Operation(summary = "Stream")
    public Flux<Object> stream() {
        return Flux.interval(Duration.ofSeconds(1))
                   .map(l -> "hello " + l);
    }

    // 响应式方式执行命令
    @GetMapping("/command")
    @Operation(summary = "Command")
    public Flux<Object> command() {
        return CommandSupportManagerProviders
            // 使用命令的方式进行RPC调用.
            // https://hanta.yuque.com/px7kg1/dev/ew1xvzmlgbzkc0hy
            .getCommandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            .flatMapMany(cmd -> cmd.execute(QueryListCommand.of(DeviceInfo.class)));
    }

    // 同步阻塞方式执行命令
    @GetMapping("/command-blocking")
    @Operation(summary = "CommandStream")
    public List<DeviceInfo> commandBlocking() {
        return CommandSupportManagerProviders
            .getCommandSupportNow(SdkServices.deviceService, DeviceCommandSupportTypes.device)
            .executeToList(QueryListCommand.of(DeviceInfo.class));
    }
}
