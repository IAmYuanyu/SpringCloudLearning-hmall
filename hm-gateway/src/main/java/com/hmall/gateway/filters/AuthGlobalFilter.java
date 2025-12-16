package com.hmall.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 拿到需要登录拦截的路径
    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    // 创建路径匹配器
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取request
        ServerHttpRequest request = exchange.getRequest();

        // 2. 判断是否需要做登录拦截
        if (isExclude(request.getPath().toString())) {
            // 放行
            return chain.filter(exchange);
        }

        // 3. 获取token
        String token = null;
        List<String> tokenList = request.getHeaders().get("Authorization");
        if (tokenList != null && !tokenList.isEmpty()) {
            token = tokenList.get(0); // token就只有一个，所以直接取第一个
        }

        // 4. 校验并解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            // 拦截并设置响应状态码为401
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete(); // setComplete()表示响应完成
        }

        // TODO 一会再实现 5. 传递用户信息
        System.out.println("userId = " + userId);

        // 6. 放行
        return chain.filter(exchange);
    }

    // 判断是否需要登录拦截
    private boolean isExclude(String path) {
        // 遍历可放行路径，判断是否与传入路径(当前请求访问路径)匹配
        // 可放行路径中含有通配符，使用AntPathMatcher进行匹配
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
