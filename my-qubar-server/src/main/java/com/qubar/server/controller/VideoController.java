package com.qubar.server.controller;


import com.alibaba.dubbo.common.utils.StringUtils;
import com.qubar.server.service.VideoMQService;
import com.qubar.server.service.VideoService;
import com.qubar.server.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("smallVideos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private MovementsController movementsController;

    @Autowired
    private CommentsController commentsController;

    private VideoMQService videoMQService;

    /**
     * 发布小视频
     *
     * @param picFile
     * @param videoFile
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveVideo(@RequestParam(value = "videoThumbnail", required = false) MultipartFile picFile,
                                          @RequestParam(value = "videoFile", required = false) MultipartFile videoFile) {
        try {
            String videoID = this.videoService.saveVideo(picFile, videoFile);
            if (StringUtils.isNotEmpty(videoID)) {

                // video保存成功，发送一个mq消息
                videoMQService.videoMsg(videoID);
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询小视频列表
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping
    public ResponseEntity<PageResult> queryVideoList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                     @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        try {
            if (page <= 0) {
                page = 1;
            }
            PageResult pageResult = this.videoService.queryVideoList(page, pageSize);
            if (null != pageResult) {
                return ResponseEntity.ok(pageResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 视频点赞
     *
     * @param videoId 视频id
     * @return
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Long> likeComment(@PathVariable("id") String videoId) {
        ResponseEntity<Long> longResponseEntity = this.movementsController.likeComment(videoId);

        if (longResponseEntity.getStatusCode().is2xxSuccessful()) {
            // video点赞成功，发送一个mq消息
            videoMQService.likeVideoMsg(videoId);
        }
        return longResponseEntity;
    }

    /**
     * 取消点赞
     *
     * @param videoId
     * @return
     */
    @PostMapping("/{id}/dislike")
    public ResponseEntity<Long> disLikeComment(@PathVariable("id") String videoId) {
        ResponseEntity<Long> longResponseEntity = this.movementsController.disLikeComment(videoId);

        if (longResponseEntity.getStatusCode().is2xxSuccessful()) {
            // video取消点赞成功，发送一个mq消息
            videoMQService.disLikeVideoMsg(videoId);
        }
        return longResponseEntity;
    }

    /**
     * 查询评论列表
     *
     * @param videoId
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<PageResult> queryCommentsList(@PathVariable("id") String videoId,
                                                        @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        return this.commentsController.queryCommentsList(videoId, page, pageSize);
    }

    /**
     * 发布评论
     *
     * @param param
     * @param videoId
     * @return
     */
    @PostMapping("/{id}/comments")//application/json
    public ResponseEntity<Void> saveComments(@RequestBody Map<String, String> param,
                                             @PathVariable("id") String videoId) {
        param.put("movementId", videoId);
        ResponseEntity<Void> voidResponseEntity = this.commentsController.saveComments(param);
        if (voidResponseEntity.getStatusCode().is2xxSuccessful()) {
            // 发送消息
            videoMQService.commentVideoMsg(videoId);
        }
        return voidResponseEntity;
    }

    /**
     * 评论点赞
     *
     * @param publishId
     * @return
     */
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<Long> videoCommentsLikeComment(@PathVariable("id") String publishId) {
        return this.movementsController.likeComment(publishId);
    }

    /**
     * 评论取消点赞
     *
     * @param publishId
     * @return
     */
    @PostMapping("/comments/{id}/dislike")
    public ResponseEntity<Long> videoCommentsDisLikeComment(@PathVariable("id") String publishId) {
        return this.movementsController.disLikeComment(publishId);
    }

    /**
     * 关注视频作者
     *
     * @param videoAuthorId
     * @return
     */
    @PostMapping("/{id}/userFocus")
    public ResponseEntity<Void> saveUserFocusComments(@PathVariable("id") Long videoAuthorId) {

        try {
            Boolean bool = this.videoService.followUser(videoAuthorId);
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消对视频作者的关注
     *
     * @param videoAuthorId
     * @return
     */
    @PostMapping("/{id}/userUnFocus")
    public ResponseEntity<Void> saveUserUnFocusComments(@PathVariable("id") Long videoAuthorId) {
        try {
            Boolean bool = this.videoService.disFollowUser(videoAuthorId);
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}