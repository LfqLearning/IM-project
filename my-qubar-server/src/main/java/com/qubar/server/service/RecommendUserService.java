package com.qubar.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qubar.dubbo.server.api.RecommendUserApi;
import com.qubar.dubbo.server.pojo.RecommendUser;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.vo.TodayBest;
import org.springframework.stereotype.Service;


/**
 * dubbo服务，从mongoDB中查询最佳user
 */
@Service
public class RecommendUserService {

    @Reference(version = "1.0.0")
    private RecommendUserApi recommendUserApi;

    public TodayBest queryTodayBest(Long userId) {

        RecommendUser recommendUser = this.recommendUserApi.queryWithMaxScore(userId);
        if (null == recommendUser) {
            return null;
        }

        TodayBest todayBest = new TodayBest();
        //如果得分为小数，保留整数：98.2->98
        double score = Math.floor(recommendUser.getScore());
        todayBest.setFateValue(Double.valueOf(score).longValue()); //缘分值

        todayBest.setId(recommendUser.getUserId()); //被推荐用户ID
        return todayBest;
    }

    /**
     * 按照当前用户ID查询推荐用户
     *
     * @param id
     * @param page
     * @param pageSize
     * @return
     */
    public PageInfo<RecommendUser> queryRecommendUserList(Long id, Integer page, Integer pageSize) {

        return this.recommendUserApi.queryPageInfo(id, page, pageSize);
    }

    /**
     * 查询推荐用户得分
     *
     * @param userId
     * @param toUserId
     * @return
     */
    public double queryScore(Long userId, Long toUserId) {
        return this.recommendUserApi.queryScore(userId, toUserId);
    }
}
