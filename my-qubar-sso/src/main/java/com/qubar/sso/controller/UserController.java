package com.qubar.sso.controller;

import com.qubar.sso.pojo.User;
import com.qubar.sso.service.UserService;
import com.qubar.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("user")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("loginVerification")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> params) {
        try {
            String mobile = params.get("phone");
            String code = params.get("verificationCode");
            String token = this.userService.login(mobile, code);

            if (StringUtils.isNotEmpty(token)) {
                // 登录成功
                String[] ss = StringUtils.split(token, "|");
                Boolean isNew = Boolean.valueOf(ss[0]);
                String tokenStr = ss[1];

                Map<String, Object> result = new HashMap<>();
                result.put("isNew", isNew);
                result.put("token", tokenStr);

                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ErrorResult.ErrorResultBuilder builder = ErrorResult.builder().errCode("000000").errMessage("登录失败");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(builder.build());
    }

    /**
     * 根据token查询用户数据
     *
     * @param token
     * @return
     */
    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token) {
        return this.userService.queryUserByToken(token);
    }
}
