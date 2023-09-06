package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.dubbo.server.pojo.RecommendUser;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.RecommendUserQueryParam;
import com.qubar.server.vo.TodayBest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TodayBestService {

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Value("${qubar.sso.default.user}")
    private Long defaultUserId;

    @Value("${qubar.sso.default.recommend.users}")
    private String defaultRecommendUsers;

    public TodayBest queryTodayBest(String token) {
        //根据token查询当前登录的用户信息
        User user = this.userService.queryUserByToken(token);
        if (user == null) {
            return null;
        }

        TodayBest todayBest = this.recommendUserService.queryTodayBest(user.getId());
        if (todayBest == null) {
            //未找到得分最高的推荐用户，给出一个默认推荐用户
            todayBest = new TodayBest();
            todayBest.setId(defaultUserId);
            todayBest.setFateValue(95L);
        }

        // 补全用户信息
        UserInfo userInfo = this.userInfoService.queryUserById(todayBest.getId());
        if (null != userInfo) {
            todayBest.setAge(userInfo.getAge());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setGender(userInfo.getSex().name().toLowerCase());
            // TODO 解决字段无法映射的问题
           /* String userSex;
            if(userInfo.getSex() == 1){
                userSex = "man";
            }else if (userInfo.getSex() == 2){
                userSex = "woman";
            }else{
                userSex = "unknown";
            }
            todayBest.setGender(userSex);*/
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
        }
        return todayBest;
    }

    /**
     *  查询推荐用户列表
     *
     * @param queryParam
     * @param token
     * @return
     */
    public PageResult queryRecommendUserList(RecommendUserQueryParam queryParam, String token) {
        // 根据token查询当前登录的用户信息

            User user = UserThreadLocal.get();

        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), queryParam.getPage(), queryParam.getPagesize());
        List<RecommendUser> records = pageInfo.getRecords();

        // 如果未查询到，需要使用默认推荐列表
        if (CollectionUtils.isEmpty(records)) {
            String[] defaultRecommendUsers = StringUtils.split(this.defaultRecommendUsers, ",");
            for (String defaultRecommendUser : defaultRecommendUsers) {
                RecommendUser recommendUser = new RecommendUser();
                recommendUser.setUserId(Long.valueOf(defaultRecommendUser));
                recommendUser.setToUserId(user.getId());
                recommendUser.setScore(RandomUtils.nextDouble(70, 95));

                records.add(recommendUser);
            }
        }
        List<Long> userIds = new ArrayList<>();
        for (RecommendUser recommendUser : records) {
            userIds.add(recommendUser.getUserId());
        }
//使用queryWrapper设置多条件查询
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);//用户Id

        if (queryParam.getAge() != null){
            queryWrapper.lt("age", queryParam.getAge());//年龄
        }

        if (StringUtils.isNotEmpty(queryParam.getCity())) {
            queryWrapper.eq("city", queryParam.getCity());
        }
        // 需要查询用户的信息，并且按照条件查询
        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);
        List<TodayBest> todayBests = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            TodayBest todayBest = new TodayBest();

            todayBest.setId(userInfo.getId());
            todayBest.setAge(userInfo.getAge());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setGender(userInfo.getSex().name().toLowerCase());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ","));
            for (RecommendUser recommendUser : records) {
                if (recommendUser.getUserId().longValue() == todayBest.getId().longValue()) {
                    todayBest.setFateValue(Double.valueOf(Math.floor(recommendUser.getScore())).longValue());//将小数转换成Long
                }
            }

            todayBests.add(todayBest);
        }

        // 对todayBests 集合排序：按照缘分值倒序排序
        Collections.sort(todayBests, ((o1, o2) -> Long.valueOf(o2.getFateValue() - o1.getFateValue()).intValue()));
        return new PageResult(0, queryParam.getPagesize(), 0, queryParam.getPage(), todayBests);
    }
}
