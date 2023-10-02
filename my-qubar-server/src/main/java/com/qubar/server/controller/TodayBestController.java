package com.qubar.server.controller;

import com.qubar.server.service.TodayBestService;
import com.qubar.server.vo.NearUserVo;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.RecommendUserQueryParam;
import com.qubar.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 *
 */
@RestController
@RequestMapping("qubar")
public class TodayBestController {

    @Autowired
    private TodayBestService todayBestService;

    @GetMapping("todayBest")
    private TodayBest queryTodayBest(@RequestHeader("Authorization") String token) {
        return this.todayBestService.queryTodayBest(token);
    }

    //TODO 待优化
    @GetMapping("recommendation")
    public PageResult queryRecommendUserList(RecommendUserQueryParam queryParam, @RequestHeader("Authorization") String token) {
        return this.todayBestService.queryRecommendUserList(queryParam, token);
    }

    /**
     * 查询最佳推荐用户的详情
     *
     * @param userId 被推荐用户id
     * @return
     */
    @GetMapping("{id}/personalInfo")
    public ResponseEntity<TodayBest> queryTodayBest(@PathVariable("id") Long userId) {
        try {
            TodayBest todayBest = this.todayBestService.queryTodayBest(userId);
            return ResponseEntity.ok(todayBest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询陌生人(指定用户)的提问
     *
     * @param userId 被查询的用户ID（即陌生人ID）
     * @return
     */
    @GetMapping("queryStrangerQuestions")
    public ResponseEntity<String> queryQuestion(@RequestParam("userId") Long userId) {
        try {
            String question = this.todayBestService.queryQuestion(userId);
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 回答陌生人的问题
     *
     * @param param
     * @return
     */
    @PostMapping("replyStrangerQuestion")
    public ResponseEntity<Void> replyQuestion(@RequestBody Map<String, Object> param) {

        try {
            Long userId = Long.valueOf(String.valueOf(param.get("userId")));
            String reply = String.valueOf(param.get("reply"));
            Boolean result = this.todayBestService.replyQuestion(userId, reply);
            if (result) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 搜附近
     *
     * @param gender
     * @param distance
     * @return
     */
    @GetMapping("search")
    public ResponseEntity<List<NearUserVo>> queryNearUser(@RequestParam(value = "gender", required = false) String gender,
                                                          @RequestParam(value = "distance", defaultValue = "2000") String distance) {
        try {
            List<NearUserVo> list = this.todayBestService.queryNearUser(gender, distance);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询推荐好友，同时用户进行二次筛选，即当前用户对推荐用户打标签（喜欢，不喜欢）
     *
     * @return
     */
    @GetMapping("cards")
    public ResponseEntity<List<TodayBest>> queryCardsList() {
        try {
            List<TodayBest> list = this.todayBestService.queryCardsList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 喜欢
     *
     * @param likeUserId
     * @return
     */
    @GetMapping("{id}/love")
    public ResponseEntity<Void> likeUser(@PathVariable("id") Long likeUserId) {
        try {
            this.todayBestService.likeUser(likeUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 不喜欢
     *
     * @param likeUserId
     * @return
     */
    @GetMapping("{id}/unlove")
    public ResponseEntity<Void> disLikeUser(@PathVariable("id") Long likeUserId) {
        try {
            this.todayBestService.disLikeUser(likeUserId);
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}