package com.qubar.server.controller;

import com.qubar.server.service.MovementsService;
import com.qubar.server.service.QuanziMQService;
import com.qubar.server.vo.Movements;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.VisitorsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("movements")
@RestController
public class MovementsController {

    @Autowired
    private MovementsService movementsService;

    @Autowired
    private QuanziMQService quanziMQService;

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
    //@NoAuthorization
    @PostMapping
    public ResponseEntity<Void> saveMovements(@RequestParam("textContent") String textContent,
                                              @RequestParam("location") String location,
                                              @RequestParam("longitude") String longitude,
                                              @RequestParam("latitude") String latitude,
                                              @RequestParam("imageContent") MultipartFile[] multipartFiles) {
        try {
            String publishId = this.movementsService.saveMovements(textContent, location, longitude, latitude, multipartFiles);
            if (StringUtils.isNotEmpty(publishId)) {

                // 发送消息
                this.quanziMQService.sendSavaPublishMsg(publishId);

                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询好友的动态信息
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping
    public ResponseEntity<PageResult> queryPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                       @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        try {
            PageResult pageResult = this.movementsService.queryUserPublishList(page, pageSize);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询推荐的动态信息
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("recommend")
    public ResponseEntity<PageResult> queryRecommendPublishList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize) {
        try {
            PageResult pageResult = this.movementsService.queryRecommendPublishList(page, pageSize);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 点赞publish
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/like")
    public ResponseEntity<Long> likeComment(@PathVariable("id") String publishId) {

        try {
            Long count = this.movementsService.likeComment(publishId);
            if (null != count) {

                // 发送消息
                this.quanziMQService.sendLikePublishMsg(publishId);

                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消点赞
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/dislike")
    public ResponseEntity<Long> disLikeComment(@PathVariable("id") String publishId) {

        try {
            Long count = this.movementsService.dislikeComment(publishId);
            if (null != count) {

                // 发送消息
                this.quanziMQService.sendDislikePublishMsg(publishId);

                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 喜欢
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/love")
    public ResponseEntity<Long> loveComment(@PathVariable("id") String publishId) {

        try {
            Long count = this.movementsService.loveComment(publishId);
            if (null != count) {

                // 发送消息
                this.quanziMQService.sendLovePublishMsg(publishId);

                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 取消喜欢
     *
     * @param publishId
     * @return
     */
    @GetMapping("/{id}/unlove")
    public ResponseEntity<Long> unLoveComment(@PathVariable("id") String publishId) {

        try {
            Long count = this.movementsService.unLoveComment(publishId);
            if (null != count) {

                // 发送消息
                this.quanziMQService.sendDisLovePublishMsg(publishId);

                return ResponseEntity.ok(count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询单条动态信息
     *
     * @param publishId
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Movements> queryMovementsById(@PathVariable("id") String publishId) {
        try {
            Movements movements = this.movementsService.queryMovementsById(publishId);
            if (null != movements) {

                // 发送消息
                this.quanziMQService.sendQueryPublishMsg(publishId);

                return ResponseEntity.ok(movements);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("visitors")
    public ResponseEntity<List<VisitorsVo>> queryVisitorsList() {

        try {
            List<VisitorsVo> list = this.movementsService.queryVisitorsList();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * 查询用户发布的所有动态（Publish）
     *
     * @return
     */
    @GetMapping("all")
    public ResponseEntity<PageResult> queryAlbumList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                     @RequestParam(value = "pagesize", defaultValue = "10") Integer pageSize,
                                                     @RequestParam(value = "userId") Long userId) {
        try {
            PageResult pageResult = this.movementsService.queryAlbumList(userId, page, pageSize);
            return ResponseEntity.ok(pageResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
