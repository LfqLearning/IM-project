package com.qubar.sso.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubar.sso.mapper.UserMapper;
import com.qubar.sso.pojo.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * User登录相关逻辑
 */
@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false) //userMapper注入报错，解决方案网址——https://blog.csdn.net/ybsgsg/article/details/118936552
    private UserMapper userMapper;

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private HuanXinService huanXinService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 短信验证码登录校验
     *
     * @param mobile
     * @param code
     * @return 如果验证成功返回token，失败返回null
     */
    public String login(String mobile, String code) {
// 校验验证码是否正确
        String redisKey = "CHECK_CODE_" + mobile;
        String value = this.redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isEmpty(value)) {
            // 验证码失效
            return null;
        }
        if (!StringUtils.equals(value, code)) {
            // 验证码输入错误
            return null;
        }

        Boolean isNew = false; // 默认是已注册

// 验证该手机号是否已经注册，如果没有注册，需要注册一个，如果已经注册，直接登录
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mobile", mobile);
        User user = this.userMapper.selectOne(queryWrapper); //在数据库查询mobile，返回user
        if (null == user) {
            // 该手机号未注册
            user = new User();
            user.setMobile(mobile);
            // 默认密码:123456
            user.setPassword(DigestUtils.md5Hex("123456"));
            this.userMapper.insert(user); //插入数据库后，ID会回写到userMapper中?
            isNew = true;

            //注册用户到环信平台
            this.huanXinService.register(user.getId());
        }

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("mobile", mobile);
        claims.put("id", user.getId());
// 生成token
        String token = Jwts.builder()
                .setClaims(claims) //设置响应数据体
                .signWith(SignatureAlgorithm.HS256, secret) //设置加密方法和加密盐
                .compact();

// 将token存储到redis中
        try {
            String redisTokenKey = "TOKEN_" + token;
            String redisTokenValue = MAPPER.writeValueAsString(user);
            this.redisTemplate.opsForValue().set(redisTokenKey, redisTokenValue, Duration.ofHours(6));//token有效期6小时
        } catch (Exception e) {
            LOGGER.error("存储token出错", e);
            return null;
        }

// RocketMQ发送消息
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("id", user.getId());
            msg.put("mobile", mobile);
            msg.put("date", new Date());
            this.rocketMQTemplate.convertAndSend("qubar-sso-login", msg);
        } catch (Exception e) {
            LOGGER.error("RocketMQ发送消息出错", e);
        }

        return isNew + "|" + token;
    }

    /**
     * 从redis中查询token，返回用户信息(id,phone)
     *
     * @param token
     * @return User
     */
    public User queryUserByToken(String token) {
        try {
            String redisTokenKey = "TOKEN_" + token;
            String cacheData = this.redisTemplate.opsForValue().get(redisTokenKey);
            if (StringUtils.isEmpty(cacheData)) {
                return null;
            }
            //刷新时间6小时
            this.redisTemplate.expire(redisTokenKey, 6, TimeUnit.HOURS);
            return MAPPER.readValue(cacheData, User.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
