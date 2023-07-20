package com.qubar.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qubar.dubbo.server.api.QuanZiApi;
import com.qubar.dubbo.server.pojo.Publish;
import com.qubar.server.pojo.User;
import com.qubar.server.utils.UserThreadLocal;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QuanziMQService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuanziMQService.class);

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    /**
     * 新增动态消息
     *
     * @param publishId
     */
    public void sendSavaPublishMsg(String publishId) {

        this.sendMsg(1, publishId);
    }

    /**
     * 浏览动态消息
     *
     * @param publishId
     */
    public void sendQueryPublishMsg(String publishId) {

        this.sendMsg(2, publishId);
    }

    /**
     * 点赞动态消息
     *
     * @param publishId
     */
    public void sendLikePublishMsg(String publishId) {

        this.sendMsg(3, publishId);
    }

    /**
     * 喜欢动态消息
     *
     * @param publishId
     */
    public void sendLovePublishMsg(String publishId) {

        this.sendMsg(4, publishId);
    }

    /**
     * 评论动态消息
     *
     * @param publishId
     */
    public void sendCommentPublishMsg(String publishId) {

        this.sendMsg(5, publishId);
    }

    /**
     * 取消点赞动态消息
     *
     * @param publishId
     */
    public void sendDislikePublishMsg(String publishId) {

        this.sendMsg(6, publishId);
    }

    /**
     * 取消喜欢动态消息
     *
     * @param publishId
     */
    public void sendDisLovePublishMsg(String publishId) {

        this.sendMsg(7, publishId);
    }

    /**
     * 发送rocketMQ消息
     *
     * @param type      1-发动态，2-浏览动态，3-点赞，4-喜欢，5-评论，6-取消点赞，7-取消喜欢
     * @param publishId
     */
    private void sendMsg(Integer type, String publishId) {

        try {
            User user = UserThreadLocal.get();

            Publish publish = quanZiApi.queryPublishById(publishId);

            Map<String, Object> msg = new HashMap<>();

            msg.put("type", type);
            msg.put("publishId", publishId);
            msg.put("data", System.currentTimeMillis());
            msg.put("userId", user.getId());
            msg.put("pid", publish.getPid());

            this.rocketMQTemplate.convertAndSend("qubar-quanzi", msg);
        } catch (Exception e) {
            e.printStackTrace();

            LOGGER.error("圈子消息发送失败！type = " + type + "，/tpublishId = " + publishId, e);
        }
    }

}
