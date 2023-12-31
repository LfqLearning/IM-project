package com.qubar.server.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.time.Duration;

/**
 * TODO 将查询数据写入redis缓存中！！！ 很重要
 * TODO 拦截器相关知识，重点回顾与掌握！！！
 * <p>
 * 将响应体内容做缓存，如果是相同的请求，直接返回结果
 */
@ControllerAdvice
public class MyResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {

        //支持的处理类型，这里仅对get和post进行处理
        return returnType.hasMethodAnnotation(GetMapping.class) || returnType.hasMethodAnnotation(PostMapping.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        try {
            String redisKey = RedisCacheInterceptor.createRedisKey(((ServletServerHttpRequest) request).getServletRequest());
            String redisValue;
            if (body instanceof String) {
                redisValue = (String) body;
            } else {
                redisValue = mapper.writeValueAsString(body);
            }

            //存储到redis中
            this.redisTemplate.opsForValue().set(redisKey, redisValue, Duration.ofHours(5));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }
}