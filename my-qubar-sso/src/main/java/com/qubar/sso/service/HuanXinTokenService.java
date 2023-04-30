package com.qubar.sso.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubar.sso.config.HuanXinConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class HuanXinTokenService {

    @Autowired
    private HuanXinConfig huanXinConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String tokenRedisKey= "HUANXIN_TOKEN";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String getToken() {

        // 先从Redis中命中
        String cacheData = redisTemplate.opsForValue().get(tokenRedisKey);
        if (StringUtils.isNotEmpty(cacheData)) {
            return cacheData;
        }

        String url = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/token";

        Map<String, Object> param = new HashMap<>();
        param.put("grant_type", "client_credentials");
        param.put("client_id", this.huanXinConfig.getClientId());
        param.put("client_secret", this.huanXinConfig.getClientSecret());

        ResponseEntity<String> stringResponseEntity = this.restTemplate.postForEntity(url, param, String.class);
        if (stringResponseEntity.getStatusCodeValue() != 200) {
            return null;
        }

        String body = stringResponseEntity.getBody();

        try {
            JsonNode jsonNode = MAPPER.readTree(body);
            String accessToken = jsonNode.get("access_token").asText();

            // 过期时间，提前6小时失效，单位秒
            long expiresIn = jsonNode.get("expires_in").asLong() - 21600;

            // 将token值存储到本地，存储到Redis中
            redisTemplate.opsForValue().set(tokenRedisKey, accessToken, expiresIn, TimeUnit.SECONDS);
            return accessToken;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
