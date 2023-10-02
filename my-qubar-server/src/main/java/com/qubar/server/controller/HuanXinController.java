package com.qubar.server.controller;

import com.qubar.server.pojo.User;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.HuanXinUser;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("huanxin")
public class HuanXinController {

    /**
     * 获取环信账号、密码
     *
     * @return
     */
    @GetMapping("user")
    public ResponseEntity<HuanXinUser> queryUser() {
        User user = UserThreadLocal.get();

        HuanXinUser huanXinUser = new HuanXinUser();
        huanXinUser.setUsername(user.getId().toString());
        huanXinUser.setPassword(DigestUtils.md5Hex(user.getId() + "_qubar"));

        return ResponseEntity.ok(huanXinUser);
    }
}
