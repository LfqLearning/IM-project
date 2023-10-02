package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.pojo.Comment;
import com.qubar.dubbo.server.pojo.Publish;
import com.qubar.dubbo.server.vo.PageInfo;

import java.util.List;

public interface QuanZiApi {

    /**
     * 发布动态
     *
     * @param publish
     * @return 主键id
     */
    String savePublish(Publish publish);

    /**
     * 查询好友动态——即查询自己的时间线性表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Publish> queryPublishList(Long userId, Integer page, Integer pageSize);

    /**
     * 点赞
     *
     * @param userId
     * @param publishId
     * @return
     */
    boolean saveLikeComment(Long userId, String publishId);

    /**
     * 取消点赞、喜欢等
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @return
     */
    boolean removeComment(Long userId, String publishId, Integer commentType);

    /**
     * 喜欢
     *
     * @param userId
     * @param publishId
     * @return
     */
    boolean saveLoveComment(Long userId, String publishId);

    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @param content
     * @return
     */
    boolean saveComment(Long userId, String publishId, Integer commentType, String content);

    /**
     * 查询评论数
     *
     * @param publishId
     * @param CommentType
     * @return
     */
    Long queryCommentCount(String publishId, Integer CommentType);

    /**
     * 根据id查询
     *
     * @param publishId
     * @return
     */
    Publish queryPublishById(String publishId);

    /**
     * 根据userId、publishId、commentType查找指定comment数量
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @return
     */
    Long queryCommentCountByUserIdAndPublishId(Long userId, String publishId, Integer commentType);

    /**
     * 查询评论
     *
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize);

    /**
     * 查询用户的评论数据
     *
     * @return
     */
    PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize);

    /**
     * 根据pid集合批量查询publish
     *
     * @param pids
     * @return
     */
    List<Publish> queryPublishByPIds(List<Long> pids);

    /**
     * 查询用户album
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize);
}
