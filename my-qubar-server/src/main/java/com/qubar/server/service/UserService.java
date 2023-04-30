package com.qubar.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubar.server.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * 根据token从redis查询用户信息
 * return user
 */
@Service
public class UserService {

    @Autowired
    private RestTemplate restTemplate;
    @Value("${qubar.sso.url}")
    private String url;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     *调用SSO系统中的接口服务进行查询
     *
     * @param token
     * @return 如果查询到就返回user，未查询到返回null
     */
    public User queryUserByToken(String token) {
        String jsonData = this.restTemplate.getForObject(url + "/user/" + token, String.class);
        if (StringUtils.isNotEmpty(jsonData)) {
            try {
                return MAPPER.readValue(jsonData, User.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
