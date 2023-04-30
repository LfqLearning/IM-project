package com.qubar.server.service;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.dubbo.server.api.QuanZiApi;
import com.qubar.dubbo.server.pojo.Comment;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.Comments;
import com.qubar.server.vo.PageResult;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentsService {

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 查询对publish的评论
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryCommentsList(String publishId, Integer page, Integer pageSize) {

        User user = UserThreadLocal.get();

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);
        pageResult.setCounts(0);
        pageResult.setPages(0);

        PageInfo<Comment> commentPageInfo = this.quanZiApi.queryCommentList(publishId, page, pageSize);
        List<Comment> records = commentPageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return pageResult;
        }

        List<Long> userIds = new ArrayList<>();
        for (Comment record : records) {
            if (!userIds.contains(record.getUserId())) {
                userIds.add(record.getUserId());
            }

        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = userInfoService.queryUserInfoList(queryWrapper);

        List<Comments> commentsList = new ArrayList<>();
        for (Comment record : records) {
            Comments comments = new Comments();

            comments.setId(record.getId().toHexString());
            comments.setCreateDate(new DateTime(record.getCreated()).toString("yyyy年MM月dd日 HH:mm"));
            comments.setContent(record.getContent());

            for (UserInfo userInfo : userInfoList) {
                if (record.getUserId().longValue() == userInfo.getUserId().longValue()) {
                    comments.setAvatar(userInfo.getLogo());
                    comments.setNickname(userInfo.getNickName());
                    break;
                }
            }

            String likeCommentsKey = "QUANZI_COMMENT_LIKE_USER_" + user.getId() + "_" + comments.getId();//对comments是否点赞
            if (Boolean.TRUE.equals(this.redisTemplate.hasKey(likeCommentsKey))) {
                comments.setHasLiked(1);//comments 是否点赞
            } else {
                Long likeCount = this.quanZiApi.queryCommentCountByUserIdAndPublishId(user.getId(), comments.getId(), 1);
                if (likeCount > 0) {
                    comments.setHasLiked(likeCount.intValue());
                } else {
                    comments.setHasLiked(0);
                }
            }

            String likeCommentsCountKey = "QUANZI_COMMENT_LIKE_" + comments.getId();
            String likeCommentsCountInRedis = this.redisTemplate.opsForValue().get(likeCommentsCountKey);
            if (StringUtils.isNotEmpty(likeCommentsCountInRedis)) {
                comments.setLikeCount(Integer.valueOf(likeCommentsCountInRedis));//comments 点赞数
            } else {
                Long commentCount = this.quanZiApi.queryCommentCount(comments.getId(), 1);
                if (commentCount > 0) {
                    comments.setLikeCount(commentCount.intValue());
                } else {
                    comments.setLikeCount(0);
                }
            }

            commentsList.add(comments);

        }
        pageResult.setItems(commentsList);
        return pageResult;
    }

    /**
     * 对comment发表comment
     *
     * @param publishId
     * @param content
     * @return
     */
    public Boolean saveComments(String publishId, String content) {

        User user = UserThreadLocal.get();
        return this.quanZiApi.saveComment(user.getId(), publishId, 2, content);
    }
}
