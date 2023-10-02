package com.qubar.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qubar.dubbo.server.api.UserLikeApi;
import com.qubar.dubbo.server.api.UserLocationApi;
import com.qubar.dubbo.server.pojo.RecommendUser;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.dubbo.server.vo.UserLocationVo;
import com.qubar.server.enums.SexEnum;
import com.qubar.server.pojo.Question;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.NearUserVo;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.RecommendUserQueryParam;
import com.qubar.server.vo.TodayBest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TodayBestService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference(version = "1.0.0")
    private UserLocationApi userLocationApi;

    @Reference(version = "1.0.0")
    private UserLikeApi userLikeApi;


    @Autowired
    private UserService userService;

    @Autowired
    private RecommendUserService recommendUserService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private IMService imService;


    @Value("${qubar.sso.default.user}")
    private Long defaultUserId;

    @Value("${qubar.sso.default.recommend.users}")
    private String defaultRecommendUsers;

    @Value("${qubar.sso.url}")
    private String ssoUrl;


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
     * 查询推荐用户列表
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
                recommendUser.setScore(RandomUtils.nextDouble(70, 98));

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

        if (queryParam.getAge() != null) {
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

    /**
     * 查询最佳推荐用户详情
     *
     * @param userId
     * @return
     */
    public TodayBest queryTodayBest(Long userId) {

        User user = UserThreadLocal.get();

        TodayBest todayBest = new TodayBest();
        //补全信息
        UserInfo userInfo = this.userInfoService.queryUserById(userId);
        todayBest.setId(userId);
        todayBest.setAge(userInfo.getAge());
        todayBest.setAvatar(userInfo.getLogo());
        todayBest.setGender(userInfo.getSex().name().toLowerCase());
        todayBest.setNickname(userInfo.getNickName());
        todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

        double score = this.recommendUserService.queryScore(userId, user.getId());
        if (score == 0) {
            score = 98; //默认分值
        }

        todayBest.setFateValue(Double.valueOf(score).longValue());
        return todayBest;
    }

    public String queryQuestion(Long userId) {
        Question question = this.questionService.queryQuestion(userId);
        if (null != question) {
            return question.getTxt();
        }
        return "";
    }

    /**
     * 回复陌生人问题，发送消息给对方
     *
     * @param userId
     * @param reply
     * @return
     */
    public Boolean replyQuestion(Long userId, String reply) {
        User user = UserThreadLocal.get();
        UserInfo userInfo = this.userInfoService.queryUserById(user.getId());

        //构建消息内容
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", user.getId().toString());
        msg.put("nickname", userInfo.getNickName());
        msg.put("strangerQuestion", this.queryQuestion(userId));
        msg.put("reply", reply);

        try {
            String msgStr = MAPPER.writeValueAsString(msg);

            // 服务器将用户回答的问题发送到环信服务器，所需要的服务地址，即需要调用sso模块中的环信消息发送服务
            String targetUrl = this.ssoUrl + "/user/huanxin/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);//请求体以表单格式提交数据

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("target", userId.toString());
            params.add("msg", msgStr);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);

            ResponseEntity<Void> responseEntity = this.restTemplate.postForEntity(targetUrl, httpEntity, Void.class);

            return responseEntity.getStatusCodeValue() == 200;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 查询用户附近的其他用户
     *
     * @param gender
     * @param distance
     * @return
     */
    public List<NearUserVo> queryNearUser(String gender, String distance) {

        User user = UserThreadLocal.get();
        // 查询用户所在位置的经纬度
        UserLocationVo userLocationVo = this.userLocationApi.queryByUserId(user.getId());
        Double longitude = userLocationVo.getLongitude();
        Double latitude = userLocationVo.getLatitude();

        // 查询附近好友
        List<UserLocationVo> userLocationVosList = this.userLocationApi.queryUserFromLocation(longitude, latitude, Integer.valueOf(distance));
        if (CollectionUtils.isEmpty(userLocationVosList)) {
            return Collections.emptyList();
        }

        List<Long> userIds = new ArrayList<>();
        for (UserLocationVo locationVo : userLocationVosList) {
            userIds.add(userLocationVo.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);

        // 进行性别筛选
        if (StringUtils.equalsIgnoreCase(gender, "man")) {
            queryWrapper.eq("sex", SexEnum.MAN);
        } else if (StringUtils.equalsIgnoreCase(gender, "woman")) {
            queryWrapper.eq("sex", SexEnum.WOMAN);
        }
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<NearUserVo> nearUserVoList = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            // 排除自己的坐标
            if (userInfo.getUserId().longValue() == user.getId().longValue()) {
                continue;
            }

            NearUserVo nearUserVo = new NearUserVo();
            nearUserVo.setUserId(userInfo.getUserId());
            nearUserVo.setNickname(userInfo.getNickName());
            nearUserVo.setAvatar(userInfo.getLogo());

            nearUserVoList.add(nearUserVo);
        }
        return nearUserVoList;
    }

    /**
     * 查询推荐用户
     *
     * @return
     */
    public List<TodayBest> queryCardsList() {
        User user = UserThreadLocal.get();

        PageInfo<RecommendUser> pageInfo = this.recommendUserService.queryRecommendUserList(user.getId(), 1, 50);
        List<RecommendUser> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            //使用默认的推荐列表
            String[] ss = StringUtils.split(defaultRecommendUsers, ',');
            for (String s : ss) {
                RecommendUser recommendUser = new RecommendUser();
                recommendUser.setUserId(Long.valueOf(s));
                recommendUser.setToUserId(user.getId());
                records.add(recommendUser);
            }
        }

        List<RecommendUser> newRecommendUserList = new ArrayList<>();

        int showCount = Math.min(10, records.size());
        for (int i = 0; i < showCount; i++) {
            //将查询到的数据打乱！！！
            createRecommendUser(newRecommendUserList, records);
        }

        List<Long> userIds = new ArrayList<>();
        for (RecommendUser recommendUser : newRecommendUserList) {
            userIds.add(recommendUser.getUserId());//推荐用户ID
        }

        // 查询推荐用户信息
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<TodayBest> result = new ArrayList<>();
        for (UserInfo userInfo : userInfoList) {
            TodayBest todayBest = new TodayBest();

            todayBest.setId(userInfo.getUserId());
            todayBest.setAge(userInfo.getAge());
            todayBest.setAvatar(userInfo.getLogo());
            todayBest.setGender(userInfo.getSex().name().toLowerCase());
            todayBest.setNickname(userInfo.getNickName());
            todayBest.setTags(StringUtils.split(userInfo.getTags(), ','));

            result.add(todayBest);
        }

        return result;
    }

    /**
     * 递归随机生成推荐好友，保证不重复
     */
    private void createRecommendUser(List<RecommendUser> newRecommendUserList,
                                     List<RecommendUser> records) {
        RecommendUser recommendUser = records.get(RandomUtils.nextInt(0, records.size() - 1));
        if (!newRecommendUserList.contains(recommendUser)) {
            newRecommendUserList.add(recommendUser);
        } else {
            // 开始递归
            createRecommendUser(newRecommendUserList, records);
        }
    }

    /**
     * 喜欢推荐用户动作
     *
     * @param likeUserId
     * @return
     */
    public Boolean likeUser(Long likeUserId) {
        User user = UserThreadLocal.get();
        String id = this.userLikeApi.saveUserLike(user.getId(), likeUserId);
        if (StringUtils.isEmpty(id)) {
            return false;
        }

        if (this.userLikeApi.isMutualLike(user.getId(), likeUserId)) {
            //相互喜欢成为好友，写入mongodb好友库中，并在环信中注册彼此好友关系
            this.imService.contactUser(likeUserId);
        }
        return true;
    }

    /**
     * 不喜欢推荐用户动作
     *
     * @param likeUserId
     * @return
     */
    public Boolean disLikeUser(Long likeUserId) {
        User user = UserThreadLocal.get();
        return this.userLikeApi.deleteUserLike(user.getId(), likeUserId);
    }
}
