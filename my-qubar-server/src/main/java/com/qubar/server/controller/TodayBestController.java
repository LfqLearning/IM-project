package com.qubar.server.controller;

import com.qubar.server.service.TodayBestService;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.RecommendUserQueryParam;
import com.qubar.server.vo.TodayBest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO tanhua 保持url一致，后续修改成qubar
 */
@RestController
//@RequestMapping("qubar")
@RequestMapping("tanhua")
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
}