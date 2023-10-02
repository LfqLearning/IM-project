package com.qubar.server.service;


import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.dubbo.server.api.QuanZiApi;
import com.qubar.dubbo.server.api.VisitorsApi;
import com.qubar.dubbo.server.pojo.Publish;
import com.qubar.dubbo.server.pojo.Visitors;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.RelativeDateFormat;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.Movements;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.PicUploadResult;
import com.qubar.server.vo.VisitorsVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class MovementsService {

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    /*@Autowired
    private UserService userService;*/

    @Autowired
    private PicUploadService picUploadService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Reference(version = "1.0.0")
    private VisitorsApi visitorsApi;

    /**
     * 发布动态
     *
     * @param textContent
     * @param location
     * @param longitude
     * @param latitude
     * @param multipartFiles
     * @return
     */
    public String saveMovements(String textContent,
                                String location,
                                String longitude,
                                String latitude,
                                MultipartFile[] multipartFiles) {
       /* User user = this.userService.queryUserByToken(token);
        if (null == user) {
            return false;
        }*/
        //TODO 使用拦截器对token进行统一拦截，做了优化处理

        User user = UserThreadLocal.get();

        Publish publish = new Publish();
        publish.setUserId(user.getId());
        publish.setText(textContent);
        publish.setLocationName(location);
        publish.setLongitude(longitude);
        publish.setLatitude(latitude);

        // 图片上传
        List<String> imageUrls = new ArrayList<>();

        if (multipartFiles != null && multipartFiles.length != 0) {
            for (MultipartFile multipartFile : multipartFiles) {
                PicUploadResult uploadResult = this.picUploadService.upload(multipartFile);
                imageUrls.add(uploadResult.getName());
            }
        }

        publish.setMedias(imageUrls);

        return this.quanZiApi.savePublish(publish);
    }

    /**
     * 查询publish列表——具体由传入的User决定
     *
     * @param user
     * @param page
     * @param pageSize
     * @return
     */
    private PageResult queryPublishList(User user, Integer page, Integer pageSize) {
        PageResult pageResult = new PageResult();
        Long userId = null; //默认查询推荐动态

        PageInfo<Publish> pageInfo = null;
        if (user == null) {
            // 查询推荐动态
            String redisKey = "QUANZI_PUBLISH_RECOMMEND_" + UserThreadLocal.get().getId();
            String value = this.redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isNotEmpty(value)) {
                // 命中数据
                String[] pids = StringUtils.split(value, ',');
                int startIndex = (page - 1) * pageSize;
                if (startIndex < pids.length) {
                    int endIndex = startIndex + pageSize - 1;
                    if (endIndex >= pids.length) {
                        endIndex = pids.length - 1;
                    }

                    List<Long> pidList = new ArrayList<>();
                    for (int i = startIndex; i <= endIndex; i++) {
                        pidList.add(Long.valueOf(pids[i]));
                    }

                    List<Publish> publishList = this.quanZiApi.queryPublishByPIds(pidList);
                    pageInfo = new PageInfo<>();
                    pageInfo.setRecords(publishList);
                }

            }
        }

        if (pageInfo == null) {
            if (user != null) {
                userId = user.getId();
            }
            pageInfo = this.quanZiApi.queryPublishList(userId, page, pageSize);
        }

        // 用户查询自己时间线表，获取动态信息

        user = UserThreadLocal.get();

        pageResult.setCounts(0);
        pageResult.setPages(0);
        pageResult.setPagesize(pageSize);
        pageResult.setPage(page);

        List<Publish> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            //没有查询到动态数据
            return pageResult;
        }

        List<Movements> movementsList = this.fillValueToMovements(records);

        pageResult.setItems(movementsList);
        return pageResult;
    }

    /**
     * 动态内数据填充
     *
     * @param records
     * @return
     */
    private List<Movements> fillValueToMovements(List<Publish> records) {

        User user = UserThreadLocal.get();
        List<Movements> movementsList = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        for (Publish record : records) {
            Movements movements = new Movements();

            movements.setId(record.getId().toHexString());
            movements.setUserId(record.getUserId());

            if (!userIds.contains(record.getUserId())) {
                userIds.add(record.getUserId());
            }

            //TODO 点赞标记处理,
            String likeCommentKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + movements.getId();//movements.getId()就是publishId
            movements.setHasLiked(this.redisTemplate.hasKey(likeCommentKey) ? 1 : 0);
            /*if (Boolean.TRUE.equals(this.redisTemplate.hasKey(likeCommentKey))) {
                movements.setHasLiked(1);//是否点赞
            } else {
                Long likeCount = this.quanZiApi.queryCommentCountByUserIdAndPublishId(user.getId(), movements.getId(), 1);
                if (likeCount > 0) {
                    movements.setHasLiked(likeCount.intValue());
                } else {
                    movements.setHasLiked(0);
                }
            }*/

            String likeCommentCountKey = "QUANZI_COMMENT_LIKE_" + movements.getId();
            String value = this.redisTemplate.opsForValue().get(likeCommentCountKey);
            if (StringUtils.isNotEmpty(value)) {
                movements.setLikeCount(Integer.valueOf(value)); //点赞数
            } else {
                movements.setLikeCount(0); //点赞数
            }

           /* String likeCommentCountInRedis = this.redisTemplate.opsForValue().get(likeCommentCountKey);
           if (StringUtils.isNotEmpty(likeCommentCountInRedis)) {
                movements.setLikeCount(Integer.valueOf(likeCommentCountInRedis));
            }  else {
                Long commentCount = this.quanZiApi.queryCommentCount(movements.getId(), 1);
                if (commentCount > 0) {
                    movements.setLoveCount(commentCount.intValue()); //点赞数
                } else {
                    movements.setLoveCount(0);
                }
            }*/

            // 喜欢标记处理
            String lovedCommentKey = "QUANZI_COMMENT_LOVE_USER_" + user.getId() + "_" + movements.getId();//movements.getId()就是publishId
            movements.setHasLoved(this.redisTemplate.hasKey(lovedCommentKey) ? 1 : 0); //是否喜欢
            /*if (Boolean.TRUE.equals(this.redisTemplate.hasKey(lovedCommentKey))) {
                movements.setHasLoved(1);//是否喜欢
            } else {
                Long loveCount = this.quanZiApi.queryCommentCountByUserIdAndPublishId(user.getId(), movements.getId(), 3);
                if (loveCount > 0) {
                    movements.setHasLiked(loveCount.intValue());
                } else {
                    movements.setHasLiked(0);
                }
            }*/

            String lovedCommentCountKey = "QUANZI_COMMENT_LOVE_" + movements.getId();
            String loveValue = this.redisTemplate.opsForValue().get(lovedCommentCountKey);
            if (StringUtils.isNotEmpty(loveValue)) {
                movements.setLoveCount(Integer.valueOf(loveValue)); //喜欢数
            } else {
                movements.setLoveCount(0); //喜欢数
            }
            /*String lovedCommentCountInRedis = this.redisTemplate.opsForValue().get(lovedCommentCountKey);
            if (StringUtils.isNotEmpty(lovedCommentCountInRedis)) {
                movements.setLoveCount(Integer.valueOf(lovedCommentCountInRedis));
            } else {
                Long commentCount = this.quanZiApi.queryCommentCount(movements.getId(), 3);
                if (commentCount > 0) {
                    movements.setLoveCount(commentCount.intValue()); //喜欢数
                } else {
                    movements.setLoveCount(0);
                }
            }*/

            movements.setDistance("1.2公里"); //TODO 距离
            movements.setCommentCount(30); //TODO 评论数
            movements.setCreateDate(RelativeDateFormat.format(new Date(record.getCreated()))); //发布时间，10分钟前
            movements.setTextContent(record.getText());
            movements.setImageContent(record.getMedias().toArray(new String[]{}));

            movementsList.add(movements);
        }

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(userInfoQueryWrapper);

        for (Movements movements : movementsList) {
            for (UserInfo userInfo : userInfoList) {
                if (movements.getUserId().longValue() == userInfo.getUserId().longValue()) {

                    movements.setTags(StringUtils.split(userInfo.getTags(), ','));
                    movements.setNickname(userInfo.getNickName());
                    movements.setGender(userInfo.getSex().name().toLowerCase());
                    movements.setAvatar(userInfo.getLogo());
                    movements.setAge(userInfo.getAge());

                    break;
                }
            }
        }

        return movementsList;
    }


    /**
     * 查询推荐publish列表
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryRecommendPublishList(Integer page, Integer pageSize) {
        return this.queryPublishList(null, page, pageSize);
    }

    /**
     * 查询用户好友publish列表——自己维护的时间线表
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryUserPublishList(Integer page, Integer pageSize) {
        return this.queryPublishList(UserThreadLocal.get(), page, pageSize);
    }

    /**
     * 点赞
     *
     * @param publishId
     * @return
     */
    public Long likeComment(String publishId) {
        User user = UserThreadLocal.get();

        boolean bool = this.quanZiApi.saveLikeComment(user.getId(), publishId);
        if (!bool) {
            return null;//保存失败
        }

        // 保存成功，获取点赞数
        Long likeCount = 0L;
        String likeCommentKey = "QUANZI_COMMENT_LIKE_" + publishId;
        if (!Boolean.TRUE.equals(this.redisTemplate.hasKey(likeCommentKey))) {
            Long count = this.quanZiApi.queryCommentCount(publishId, 1);
            likeCount = count;
            this.redisTemplate.opsForValue().set(likeCommentKey, String.valueOf(likeCount));
        } else {
            likeCount = this.redisTemplate.opsForValue().increment(likeCommentKey);//value自动递增1
        }

        //记录当前用户已经点赞
        String haseLikeCommentKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + publishId;
        this.redisTemplate.opsForValue().set(haseLikeCommentKey, "1");

        return likeCount;
    }

    /**
     * 取消点赞
     *
     * @param publishId
     * @return Long decrementCount
     */
    public Long dislikeComment(String publishId) {

        User user = UserThreadLocal.get();
        boolean bool = this.quanZiApi.removeComment(user.getId(), publishId, 1);
        if (!bool) {
            return null;
        }

        // redis中的点赞数需减一
        String likeCommentKey = "QUANZI_COMMENT_LIKE_" + publishId;
        Long decrementCount = this.redisTemplate.opsForValue().decrement(likeCommentKey);//TODO 查询逻辑需要修改

        // 删除该用户的标记点赞
        String haseLikeCommentKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + publishId;
        this.redisTemplate.delete(haseLikeCommentKey);
        return decrementCount;
    }

    /**
     * 喜欢并返回总喜欢数
     *
     * @param publishId
     * @return
     */
    public Long loveComment(String publishId) {

        User user = UserThreadLocal.get();

        boolean bool = this.quanZiApi.saveLoveComment(user.getId(), publishId);
        if (!bool) {
            return null;//保存失败
        }

        // 保存成功，获取喜欢数
        Long loveCount = 0L;
        String loveCommentKey = "QUANZI_COMMENT_LOVE_" + publishId;
        if (!Boolean.TRUE.equals(this.redisTemplate.hasKey(loveCommentKey))) {
            Long count = this.quanZiApi.queryCommentCount(publishId, 3);
            loveCount = count;
            this.redisTemplate.opsForValue().set(loveCommentKey, String.valueOf(loveCount));
        } else {
            loveCount = this.redisTemplate.opsForValue().increment(loveCommentKey);//value自动递增1
        }

        //记录当前用户已经点赞
        String haseLikeCommentKey = "QUANZI_COMMENT_LOVE_USER_" + user.getId() + "_" + publishId;
        this.redisTemplate.opsForValue().set(haseLikeCommentKey, "1");

        return loveCount;
    }

    /**
     * 取消喜欢
     *
     * @param publishId
     * @return
     */
    public Long unLoveComment(String publishId) {

        User user = UserThreadLocal.get();
        boolean bool = this.quanZiApi.removeComment(user.getId(), publishId, 3);
        if (!bool) {
            return null;
        }

        // redis中的喜欢数需减一
        String loveCommentKey = "QUANZI_COMMENT_LOVE_" + publishId;
        Long decrementCount = this.redisTemplate.opsForValue().decrement(loveCommentKey);

        // 删除该用户的标记喜欢
        String haseLoveCommentKey = "QUANZI_COMMENT_LOVE_USER_" + user.getId() + "_" + publishId;
        this.redisTemplate.delete(haseLoveCommentKey);
        return decrementCount;
    }

    public Movements queryMovementsById(String publishId) {

        Publish publish = this.quanZiApi.queryPublishById(publishId);
        if (null == publish) {
            return null;
        }

        // 查询到动态数据，并完成数据填充
        List<Movements> movementsList = this.fillValueToMovements(Arrays.asList(publish));
        return movementsList.get(0);
    }

    /**
     * 查询访问过我的用户
     *
     * @return
     */
    public List<VisitorsVo> queryVisitorsList() {

        User user = UserThreadLocal.get();

        // 如果redis中存在上一次查询的时间点的话，就按照时间查询，否则就按照数量查询
        String redisKey = "visitors_" + user.getId();
        String data = this.redisTemplate.opsForValue().get(redisKey);
        List<Visitors> visitorsList = null;
        if (StringUtils.isEmpty(data)) {
            visitorsList = visitorsApi.topVisitor(user.getId(), 6);
        } else {
            visitorsList = visitorsApi.topVisitor(user.getId(), Long.valueOf(data));
        }

        if (CollectionUtils.isEmpty(visitorsList)) {
            return Collections.emptyList();
        }

        List<Long> userIds = new ArrayList<>();
        for (Visitors visitors : visitorsList) {
            userIds.add(visitors.getVisitorUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<VisitorsVo> visitorsVoList = new ArrayList<>();

        for (Visitors visitors : visitorsList) {
            for (UserInfo userInfo : userInfoList) {
                if (visitors.getVisitorUserId().longValue() == userInfo.getUserId().longValue()) {
                    VisitorsVo visitorsVo = new VisitorsVo();

                    visitorsVo.setAge(userInfo.getAge());
                    visitorsVo.setAvatar(userInfo.getLogo());
                    visitorsVo.setGender(userInfo.getSex().name().toLowerCase());
                    visitorsVo.setId(userInfo.getUserId());
                    visitorsVo.setNickname(userInfo.getNickName());
                    visitorsVo.setTags(StringUtils.split(userInfo.getTags(), ','));
                    visitorsVo.setFateValue(visitors.getScore().intValue());

                    visitorsVoList.add(visitorsVo);
                    break;
                }
            }
        }
        return visitorsVoList;
    }

    /**
     * 在用户album中查询用户发布的所有动态
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryAlbumList(Long userId, Integer page, Integer pageSize) {

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);

        PageInfo<Publish> albumPageInfo = this.quanZiApi.queryAlbumList(userId, page, pageSize);
        List<Publish> records = albumPageInfo.getRecords();

        if (CollectionUtils.isEmpty(records)) {
            return pageResult;
        }

        pageResult.setItems(this.fillValueToMovements(records));
        return pageResult;
    }

}
