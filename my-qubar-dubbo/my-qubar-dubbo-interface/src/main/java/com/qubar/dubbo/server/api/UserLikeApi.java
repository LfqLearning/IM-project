package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.pojo.UserLike;
import com.qubar.dubbo.server.vo.PageInfo;

public interface UserLikeApi {

    /**
     * 保存喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    String saveUserLike(Long userId, Long likeUserId);


    /**
     * 相互喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    Boolean isMutualLike(Long userId, Long likeUserId);

    /**
     * 删除用户喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    Boolean deleteUserLike(Long userId, Long likeUserId);

    /**
     * 与当前用户相互喜欢的其他用户数量
     *
     * @return
     */
    Long queryEachLikeCount(Long userId);

    /**
     * 喜欢数
     *
     * @return
     */
    Long queryLikeCount(Long userId);

    /**
     * 粉丝数
     *
     * @return
     */
    Long queryFanCount(Long userId);

    /**
     * 查询相互喜欢列表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<UserLike> queryEachLikeList(Long userId, Integer page, Integer pageSize);

    /**
     * 查询我喜欢的列表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<UserLike> queryLikeList(Long userId, Integer page, Integer pageSize);

    /**
     * 查询粉丝列表
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<UserLike> queryFanList(Long userId, Integer page, Integer pageSize);
}