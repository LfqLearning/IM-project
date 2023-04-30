package com.qubar.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.qubar.dubbo.server.api.QuanZiApi;
import com.qubar.dubbo.server.api.VideoApi;
import com.qubar.dubbo.server.pojo.Video;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.PicUploadResult;
import com.qubar.server.vo.VideoVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

    @Reference(version = "1.0.0")
    private VideoApi videoApi;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private PicUploadService picUploadService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private FdfsWebServer fdfsWebServer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 保存小视频
     *
     * @param picFile
     * @param videoFile
     * @return
     */
    public Boolean saveVideo(MultipartFile picFile, MultipartFile videoFile) {

        User user = UserThreadLocal.get();

        Video video = new Video();
        video.setUserId(user.getId());
        video.setSeeType(1);

        try {
            //上传图片——上传到阿里云OSS
            PicUploadResult picUploadResult = picUploadService.upload(picFile);
            video.setPicUrl(picUploadResult.getName());

            //上传视频
            StorePath storePath = fastFileStorageClient.uploadFile(videoFile.getInputStream(),
                    videoFile.getSize(),
                    StringUtils.substringAfter(videoFile.getOriginalFilename(), "."), null);
            video.setVideoUrl(fdfsWebServer.getWebServerUrl() + storePath.getFullPath());

            return this.videoApi.saveVideo(video);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 查询小视频列表
     *
     * @param page
     * @param pageSize
     * @return
     */
    public PageResult queryVideoList(Integer page, Integer pageSize) {

        User user = UserThreadLocal.get();

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPagesize(pageSize);
        pageResult.setPages(0);
        pageResult.setCounts(0);

        PageInfo<Video> pageInfo = this.videoApi.queryVideoList(page, pageSize);

/*        //先从Redis进行命中，如果命中则返回推荐列表，如果未命中查询默认列表
        String redisValue = this.redisTemplate.opsForValue().get("QUANZI_VIDEO_RECOMMEND_" + user.getId());
        if (StringUtils.isNotEmpty(redisValue)) {
            String[] vids = StringUtils.split(redisValue, ',');
            int startIndex = (page - 1) * pageSize;
            if (startIndex < vids.length) {
                int endIndex = startIndex + pageSize - 1;
                if (endIndex >= vids.length) {
                    endIndex = vids.length - 1;
                }

                List<Long> vidList = new ArrayList<>();
                for (int i = startIndex; i <= endIndex; i++) {
                    vidList.add(Long.valueOf(vids[i]));
                }

                List<Video> videoList = this.videoApi.queryVideoListByPids(vidList);
                pageInfo = new PageInfo<>();
                pageInfo.setRecords(videoList);
            }
        }

        if (null == pageInfo) {
            pageInfo = this.videoApi.queryVideoList(page, pageSize);
        }*/

        List<Video> records = pageInfo.getRecords();
        List<VideoVo> videoVoList = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        for (Video record : records) {
            VideoVo videoVo = new VideoVo();

            videoVo.setUserId(record.getUserId());
            videoVo.setCover(record.getPicUrl());
            videoVo.setVideoUrl(record.getVideoUrl());
            videoVo.setId(record.getId().toHexString());
            videoVo.setSignature("我就是我_userId_" + record.getUserId() + "_videoId_" + record.getId()); //TODO 签名

            // 评论数
            Long commentCount = this.quanZiApi.queryCommentCount(videoVo.getId(), 2);
            videoVo.setCommentCount(commentCount == null ? 0 : commentCount.intValue());

            videoVo.setHasFocus(0); //TODO 是否关注

            String likeUserCommentsKey = "QUANZI_COMMENT_LIVE_USER_" + user.getId() + "_" + videoVo.getId();
            if (Boolean.TRUE.equals(this.redisTemplate.hasKey(likeUserCommentsKey))) {
                videoVo.setHasLiked(1);//videoVo 是否点赞
            } else {
                Long likeCount = this.quanZiApi.queryCommentCountByUserIdAndPublishId(user.getId(), videoVo.getId(), 1);
                if (likeCount > 0) {
                    videoVo.setHasLiked(likeCount.intValue());
                } else {
                    videoVo.setHasLiked(0);
                }
            }

            String likeCommentsCountKey = "QUANZI_COMMENT_LIVE_" + videoVo.getId();
            String likeCommentsCountInRedis = this.redisTemplate.opsForValue().get(likeCommentsCountKey);
            if (com.alibaba.dubbo.common.utils.StringUtils.isNotEmpty(likeCommentsCountInRedis)) {
                videoVo.setLikeCount(Integer.valueOf(likeCommentsCountInRedis));//videoVo 点赞数
            } else {
                Long likeCommentsCount = this.quanZiApi.queryCommentCount(videoVo.getId(), 1);
                if (likeCommentsCount > 0) {
                    videoVo.setLikeCount(likeCommentsCount.intValue());
                } else {
                    videoVo.setLikeCount(0);
                }
            }

            if (!userIds.contains(record.getUserId())) {
                userIds.add(record.getUserId());
            }

            videoVoList.add(videoVo);
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfos = this.userInfoService.queryUserInfoList(queryWrapper);
        for (VideoVo videoVo : videoVoList) {
            for (UserInfo userInfo : userInfos) {
                if (videoVo.getUserId().longValue() == userInfo.getUserId().longValue()) {

                    videoVo.setNickname(userInfo.getNickName());
                    videoVo.setAvatar(userInfo.getLogo());

                    break;
                }
            }

        }

        pageResult.setItems(videoVoList);

        return pageResult;
    }

    public Boolean followUser(Long videoAuthorId) {//TODO 关注总数没完成

        User user = UserThreadLocal.get();
        String followUserKey = "VIDEO_FOLLOW_USER_" + user.getId() + "_" + videoAuthorId;
        Boolean redisFollowUserKey = this.redisTemplate.hasKey(followUserKey);
        if (Boolean.TRUE.equals(redisFollowUserKey)) {
            return true;
        }
        Boolean whetherFollowUser = this.videoApi.queryFollowUserByUserIdAndFollowUserId(user.getId(), videoAuthorId);
        if (Boolean.TRUE.equals(whetherFollowUser)) {
            this.redisTemplate.opsForValue().set(followUserKey, "1");
            return true;
        }
        Boolean bool = this.videoApi.followUser(user.getId(), videoAuthorId);
        if (!Boolean.TRUE.equals(bool)) {
            return false;
        }

        this.redisTemplate.opsForValue().set(followUserKey, "1");

        return true;
    }

    public Boolean disFollowUser(Long videoAuthorId) {

        User user = UserThreadLocal.get();
        String followUserKey = "VIDEO_FOLLOW_USER_" + user.getId() + "_" + videoAuthorId;
        Boolean redisFollowUserKey = this.redisTemplate.hasKey(followUserKey);
        if (Boolean.TRUE.equals(redisFollowUserKey)) {
            if (Boolean.FALSE.equals(this.redisTemplate.delete(followUserKey))) {
                return false;
            }
        }

        Boolean whetherFollowUser = this.videoApi.queryFollowUserByUserIdAndFollowUserId(user.getId(), videoAuthorId);
        if (Boolean.FALSE.equals(whetherFollowUser)) {
            return true;
        }

        return videoApi.disFollowUser(user.getId(), videoAuthorId);
    }



    public Long queryFollowUser(Long videoAuthorId) {

        return this.videoApi.queryFollowUserCount(videoAuthorId);
    }
}
