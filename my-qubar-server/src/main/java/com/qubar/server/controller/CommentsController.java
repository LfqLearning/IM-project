package com.qubar.server.controller;

import com.qubar.server.service.CommentsService;
import com.qubar.server.service.MovementsService;
import com.qubar.server.service.QuanziMQService;
import com.qubar.server.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("comments")
public class CommentsController {

    @Autowired
    private CommentsService commentsService;

    @Autowired
    private MovementsService movementsService;

    @Autowired
    private QuanziMQService quanziMQService;

    /**
     * 查询评论列表
     *
     * @param publishId
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping
    public ResponseEntity<PageResult> queryCommentsList(@RequestParam("movementId") String publishId,
                                                        @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        try {
            PageResult pageResult = this.commentsService.queryCommentsList(publishId, page, pageSize);
            if (null != pageResult) {
                return ResponseEntity.ok(pageResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 发表评论
     *
     * @param param
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveComments(@RequestBody Map<String, String> param) {

        try {
            String publishId = param.get("movementId");
            String content = param.get("comment");
            Boolean bool = this.commentsService.saveComments(publishId, content);
            if (Boolean.TRUE.equals(bool)) {

                // 发送消息
                this.quanziMQService.sendCommentPublishMsg(publishId);

                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 给别人的comment点赞
     *
     * @param commentId
     * @return
     */
    @GetMapping("/{commentId}/like")
    public ResponseEntity<Long> likeComment(@PathVariable("commentId") String commentId) {

        try {
            Long count = this.movementsService.likeComment(commentId);
            if (null != count) {
                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消给别人的comment点赞
     *
     * @param commentId
     * @return
     */
    @GetMapping("/{commentId}/dislike")
    public ResponseEntity<Long> disLikeComment(@PathVariable("commentId") String commentId) {

        try {
            Long count = this.movementsService.dislikeComment(commentId);
            if (null != count) {
                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
