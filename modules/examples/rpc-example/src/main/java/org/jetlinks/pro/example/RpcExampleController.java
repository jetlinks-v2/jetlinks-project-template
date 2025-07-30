package org.jetlinks.pro.example;

import org.jetlinks.core.rpc.RpcManager;
import org.jetlinks.pro.cluster.Cluster;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/example/rpc")
public class RpcExampleController {

    private final RpcManager rpcManager;

    public RpcExampleController(RpcManager rpcManager) {
        this.rpcManager = rpcManager;
        rpcManager.registerService(new LocalExampleRpcApi());
    }

    //启动2个节点,调用 http://localhost:8800/example/rpc/hello-everyone试试
    @GetMapping("/hello-everyone")
    public Flux<String> helloAll() {
        return rpcManager
            .getServices(ExampleRpcApi.class)
            .flatMap(service -> service.service().hello(Cluster.id()));
    }

    @GetMapping("/hello/{serverNodeId}")
    public Mono<String> helloP2P(@PathVariable String serverNodeId) {
        return rpcManager
            .getService(serverNodeId, ExampleRpcApi.class)
            .flatMap(service -> service.hello(Cluster.id()));
    }

}
