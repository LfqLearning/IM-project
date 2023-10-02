package com.qubar.sso.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubar.sso.config.HuanXinConfig;
import com.qubar.sso.vo.HuanXinUser;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class HuanXinService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    private HuanXinConfig huanXinConfig;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private HuanXinTokenService huanXinTokenService;

    /**
     * 注册环信用户
     *
     * @param userId 自己的id
     * @return
     */
    public boolean register(Long userId) {

        String url = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users";

        String token = this.huanXinTokenService.getToken();
        //请求头信息
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-type", "application/json");
        httpHeaders.add("Authorization", "Bearer " + token);

        List<HuanXinUser> huanXinUsers = new ArrayList<>();
        huanXinUsers.add(new HuanXinUser(userId.toString(), DigestUtils.md5Hex(userId + "_qubar")));//设置用户及密码

        try {
            HttpEntity<String> httpEntity = new HttpEntity(MAPPER.writeValueAsString(huanXinUsers), httpHeaders);

            // 发起请求
            ResponseEntity<String> stringResponseEntity = this.restTemplate.postForEntity(url, httpEntity, String.class);

            return (stringResponseEntity.getStatusCodeValue() == 200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 添加好友
     *
     * @param userId
     * @param friendId
     * @return
     */
    public boolean contactUsers(Long userId, Long friendId) {

        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/users/" +
                userId + "/contacts/users/" + friendId;
        try {
            String token = this.huanXinTokenService.getToken();
            // 请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/json");
            headers.add("Authorization", "Bearer " + token);
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, String.class);
            return responseEntity.getStatusCodeValue() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 添加失败
        return false;
    }

    public Boolean sendMsg(String target, String msg, String type) {

        String targetUrl = this.huanXinConfig.getUrl()
                + this.huanXinConfig.getOrgName() + "/"
                + this.huanXinConfig.getAppName() + "/messages";

        try {
            String token = this.huanXinTokenService.getToken();
            //请求头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");
            headers.add("Authorization", "Bearer" + token);

            Map<String, Object> requestParam = new HashMap<>();
            requestParam.put("target_type", "users");
            requestParam.put("target", Arrays.asList(target));

            Map<String, Object> msgParam = new HashMap<>();
            msgParam.put("msg", msg);
            msgParam.put("type", type);

            requestParam.put("msg", msgParam);
            // 表示消息发送者，若无此字段Server会默认设置 “from”：“admin”，有from字段但值为空串（“”）时请求失败
            //requestParam.put("from", null);

            // 包装一个http请求，包括字符串类型的请求体，与httpHeader类型的请求头
            HttpEntity<String> httpEntity = new HttpEntity<>(MAPPER.writeValueAsString(requestParam), headers);

            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(targetUrl, httpEntity, String.class);
            return stringResponseEntity.getStatusCodeValue() == 200;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
