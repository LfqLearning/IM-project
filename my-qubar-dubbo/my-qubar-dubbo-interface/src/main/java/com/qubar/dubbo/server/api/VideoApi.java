package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.pojo.Video;
import com.qubar.dubbo.server.vo.PageInfo;

import java.util.List;

public interface VideoApi {

    /**
     * 保存小视频
     *
     * @param video
     * @return
     */
    String saveVideo(Video video);

    /**
     * 分页查询小视频列表，按照时间倒序排序
     *
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Video> queryVideoList(Integer page, Integer pageSize);

    /**
     * 关注用户
     *
     * @param userId
     * @param followUserId
     * @return
     */
    Boolean followUser(Long userId, Long followUserId);

    /**
     * 取消关注用户
     *
     * @param userId
     * @param followUserId
     * @return
     */
    Boolean disFollowUser(Long userId, Long followUserId);

    /**
     * 查询用户是否关注视频作者
     *
     * @param userId
     * @param followUserId
     * @return
     */
    Boolean queryFollowUserByUserIdAndFollowUserId(Long userId, Long followUserId);

    /**
     * 查询视频作者粉丝数
     *
     * @param followUserId
     * @return
     */
    Long queryFollowUserCount(Long followUserId);

    /**
     * 根据Id查询小视频
     *
     * @param videoId
     * @return
     */
    Video queryVideoById(String videoId);

    /**
     * 根据vids批量查询视频列表
     *
     * @param vidList
     * @return
     */
    List<Video> queryVideoListByPids(List<Long> vidList);
}