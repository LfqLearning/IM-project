package com.qubar.server.controller;

import com.qubar.server.service.UsersService;
import com.qubar.server.vo.CountsVo;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.SettingsVo;
import com.qubar.server.vo.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("users")
public class UsersController {

    @Autowired
    private UsersService usersService;

    /**
     * 用户资料 - 读取
     *
     * @param userID
     * @param huanxinID
     * @return
     */
    @GetMapping
    public ResponseEntity<UserInfoVo> queryUserInfo(@RequestParam(value = "userID", required = false) String userID,
                                                    @RequestParam(value = "huanxinID", required = false) String huanxinID) {
        try {
            //用户第一次 接收到 环信推来的信息 ，不带用户信息
            UserInfoVo userInfoVo = this.usersService.queryUserInfo(userID, huanxinID);
            if (null != userInfoVo) {
                return ResponseEntity.ok(userInfoVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 更新用户信息
     *
     * @param userInfoVo
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateUserInfo(@RequestBody UserInfoVo userInfoVo) {
        try {
            Boolean bool = this.usersService.updateUserInfo(userInfoVo);
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 互相喜欢，喜欢，粉丝 - 统计
     *
     * @return
     */
    @GetMapping("counts")
    public ResponseEntity<CountsVo> queryCounts() {
        try {
            CountsVo countsVo = this.usersService.queryCounts();
            if (null != countsVo) {
                return ResponseEntity.ok(countsVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 互相关注、我关注、粉丝、谁访问过我 的综合列表。
     * 根据type类型，调用不同的API，实现对四种请求内容的查询！！！
     *
     * @param type     1 互相关注 2 我关注 3 粉丝 4 谁看过我
     * @param page
     * @param pageSize
     * @param nickname
     * @return
     */
    @GetMapping("friends/{type}")
    public ResponseEntity<PageResult> queryLikeList(@PathVariable("type") String type,
                                                    @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                    @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize,
                                                    @RequestParam(value = "nickname", required = false) String nickname) {
        try {
            page = Math.max(1, page);
            PageResult pageResult = this.usersService.queryLikeList(Integer.valueOf(type), page, pageSize, nickname);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消喜欢
     *
     * @param likeUserId
     * @return
     */
    @DeleteMapping("like/{uid}")
    public ResponseEntity<Void> disLike(@PathVariable("uid") Long likeUserId) {
        try {
            this.usersService.disLike(likeUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 关注粉丝
     *
     * @param fanUserId
     * @return
     */
    @PostMapping("fans/{uid}")
    public ResponseEntity<Void> likeFan(@PathVariable("uid") Long fanUserId) {
        try {
            this.usersService.likeFan(fanUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询通用设置 1.设置的陌生人问题   2.通知  3，用户手机号
     *
     * @return
     */
    @GetMapping("settings")
    public ResponseEntity<SettingsVo> querySettings() {
        try {
            SettingsVo settingsVo = this.usersService.querySettings();
            if (null != settingsVo) {
                return ResponseEntity.ok(settingsVo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 设置陌生人问题
     *
     * @return
     */
    @PostMapping("questions")
    public ResponseEntity<Void> saveQuestions(@RequestBody Map<String, String> param) {
        try {
            String content = param.get("content");
            this.usersService.saveQuestions(content);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询黑名单
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("blacklist")
    public ResponseEntity<PageResult> queryBlacklist(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                     @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        try {
            PageResult pageResult = this.usersService.queryBlacklist(page, pageSize);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 移除黑名单
     *
     * @return
     */
    @DeleteMapping("blacklist/{uid}")
    public ResponseEntity<Void> delBlacklist(@PathVariable("uid") Long userId) {
        try {
            this.usersService.delBlacklist(userId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 更新通知设置
     *
     * @param param
     * @return
     */
    @PostMapping("notifications/setting")
    public ResponseEntity<Void> updateNotification(@RequestBody Map<String, Boolean> param) {
        try {
            Boolean likeNotification = param.get("likeNotification");
            Boolean reviewNotification = param.get("reviewNotification");
            Boolean systemNotification = param.get("systemNotification");

            this.usersService.updateNotification(likeNotification, reviewNotification, systemNotification);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}