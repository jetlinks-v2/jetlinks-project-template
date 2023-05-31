package org.jetlinks.project.example;

import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.pro.cluster.Cluster;
import reactor.core.publisher.Mono;

public class LocalExampleRpcApi implements ExampleRpcApi {
    @Override
    public Mono<String> hello(String name) {
        return Mono
            .just("Hello '" + name + "',My name is '" + Cluster.id()+"'")
            .as(MonoTracer.create("/example/rpc/hello"));
    }
}
