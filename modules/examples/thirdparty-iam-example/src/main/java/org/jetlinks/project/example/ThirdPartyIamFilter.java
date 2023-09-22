package org.jetlinks.project.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.net.HttpHeaders;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationSupplier;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.Duration;

public class ThirdPartyIamFilter implements WebFilter, ReactiveAuthenticationSupplier {

    private final WebClient client;

    @Setter
    private Duration expires = Duration.ofMillis(1);

    //系统直接访问IAM接口的认证key,不同IMA系统可能认证方式不同
    @Setter
    private String accessKey = "";

    private final Cache<String, Mono<Authentication>> tokenCache =
        Caffeine.newBuilder()
                .softValues()
                .build();

    public ThirdPartyIamFilter(WebClient client) {
        this.client = client;
        //注册到Holder
        ReactiveAuthenticationHolder.addSupplier(this);
    }

    @Override
    @Nonnull
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        String token = exchange
            .getRequest()
            .getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        //从header中获取token,格式: Authorization: Bearer {token}
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        token = token.substring(7);

        return this
            //根据token获取用户信息
            .requestUserInfoByToken(token)
            .flatMap(auth -> chain
                .filter(exchange)
                //传入上下文到上游
                .contextWrite(ctx -> ctx.put(ThirdPartyIamFilter.class, auth)));
    }

    private Mono<Authentication> requestUserInfoByToken(String token) {

        //缓存token的权限信息,避免每次请求都去IAM获取
        //缓存时间根据实际需求调整，缓存时间过长可能导致用户权限变更后无法及时生效
        return tokenCache.get(token, _token -> this
            .requestUserInfoByToken0(_token)
            .cache(v -> expires,
                   err -> Duration.ofSeconds(1),
                   () -> Duration.ofSeconds(1)));
    }

    private Mono<Authentication> requestUserInfoByToken0(String token) {
        return client
            .get()
            .uri("/user-info")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .retrieve()
            .bodyToMono(ThirdPartyUserInfo.class)
            .map(ThirdPartyUserInfo::toAuthentication);
    }

    @Override
    public Mono<Authentication> get(String userId) {
        return client
            .get()
            .uri("/user-info/" + userId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessKey)
            .retrieve()
            .bodyToMono(ThirdPartyUserInfo.class)
            .map(ThirdPartyUserInfo::toAuthentication);
    }

    @Override
    public Mono<Authentication> get() {
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(ThirdPartyIamFilter.class)));
    }

}
