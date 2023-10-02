package com.qubar.sso.service;

import com.qubar.sso.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UsersService {

    @Autowired
    private SmsService smsService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Boolean sendVerificationCode(String token) {
        //查询到用户的手机号
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        String mobile = user.getMobile();
        Map<String, Object> result = this.smsService.sendCheckCode(mobile);
        return (Integer) result.get("code") == 3;
    }

    /**
     * 对用户输入的验证码进行校验
     *
     * @param code
     * @param token
     * @return
     */
    public Boolean checkVerificationCode(String code, String token) {
        //查询到用户的手机号
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        String redisKey = SmsService.REDIS_KEY_PREFIX + user.getMobile();
        String value = this.redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.equals(value, code)) {
            // 验证码正确,把验证码从redis缓存中删除，一次性验证码
            this.redisTemplate.delete(redisKey);
            return true;
        }

        return false;
    }

    /**
     * 更改用户手机号
     *
     * @param token
     * @param newPhone
     * @return
     */
    public Boolean updateNewMobile(String token, String newPhone) {
        //查询到用户的手机号
        User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }

        return this.userService.updateNewMobile(user.getId(), newPhone);
    }
}
