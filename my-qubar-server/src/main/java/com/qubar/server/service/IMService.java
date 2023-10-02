package com.qubar.server.service;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qubar.dubbo.server.api.QuanZiApi;
import com.qubar.dubbo.server.api.UsersApi;
import com.qubar.dubbo.server.pojo.Comment;
import com.qubar.dubbo.server.pojo.Users;
import com.qubar.dubbo.server.vo.PageInfo;
import com.qubar.server.pojo.Announcement;
import com.qubar.server.pojo.User;
import com.qubar.server.pojo.UserInfo;
import com.qubar.server.utils.UserThreadLocal;
import com.qubar.server.vo.Contacts;
import com.qubar.server.vo.MessageAnnouncement;
import com.qubar.server.vo.MessageLike;
import com.qubar.server.vo.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class IMService {

    @Reference(version = "1.0.0")
    private UsersApi usersApi;

    @Reference(version = "1.0.0")
    private QuanZiApi quanZiApi;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private AnnouncementService announcementService;

    @Value("${qubar.sso.url}")
    private String ssoUrl;

    public Boolean contactUser(Long friendId) {

        // 需要做两件事，1、保存数据到MongoDB，2、好友关系注册到环信
        Users users = new Users();
        users.setUserId(UserThreadLocal.get().getId());
        users.setFriendId(friendId);

        // 1、保存数据到MongoDB,好友库中，保存成功，则成为好友
        this.usersApi.saveUsers(users);

        // 2、好友关系注册到环信
        String url = ssoUrl + "user/huanxin/contacts/" + users.getUserId() + "/" + users.getFriendId();
        ResponseEntity<Void> voidResponseEntity = restTemplate.postForEntity(url, null, Void.class);

        if (voidResponseEntity.getStatusCodeValue() == 200) {
            return true;
        }
        return false;
    }

    public PageResult queryContactsList(Integer page, Integer pageSize, String keyword) {

        User user = UserThreadLocal.get();

        List<Users> usersList = null;

        if (StringUtils.isNotEmpty(keyword)) {
            usersList = this.usersApi.queryAllUsersList(user.getId());
        } else {
            PageInfo<Users> usersPageInfo = this.usersApi.queryUsersList(user.getId(), page, pageSize);
            usersList = usersPageInfo.getRecords();
        }

        List<Long> fUserIds = new ArrayList<>();
        for (Users users : usersList) {
            fUserIds.add(users.getFriendId());
        }

        //查询好友详细信息
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", fUserIds);
        if (StringUtils.isNotEmpty(keyword)) {
            queryWrapper.like("nick_name", keyword);//模糊查询
        }
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<Contacts> contactsList = new ArrayList<>();
        if (StringUtils.isEmpty(keyword)) {
            for (Users users : usersList) { //多余步骤，增加开销
                for (UserInfo userInfo : userInfoList) {
                    if (users.getFriendId().longValue() == userInfo.getUserId().longValue()) {
                        Contacts contacts = new Contacts();

                        contacts.setCity(StringUtils.substringBefore(userInfo.getCity(), "-"));
                        contacts.setUserId(String.valueOf(userInfo.getUserId()));
                        contacts.setNickname(userInfo.getNickName());
                        contacts.setGender(userInfo.getSex().name().toLowerCase());
                        contacts.setAvatar(userInfo.getLogo());
                        contacts.setAge(userInfo.getAge());

                        contactsList.add(contacts);
                        break;
                    }
                }
            }
        } else {
            for (UserInfo userInfo : userInfoList) {
                Contacts contacts = new Contacts();

                contacts.setCity(StringUtils.substringBefore(userInfo.getCity(), "-"));
                contacts.setUserId(String.valueOf(userInfo.getUserId()));
                contacts.setNickname(userInfo.getNickName());
                contacts.setGender(userInfo.getSex().name().toLowerCase());
                contacts.setAvatar(userInfo.getLogo());
                contacts.setAge(userInfo.getAge());

                contactsList.add(contacts);
            }
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setPagesize(pageSize);
        pageResult.setCounts(0);
        pageResult.setItems(contactsList);

        return pageResult;
    }

    // 查询点赞
    public PageResult queryMessageLikeList(Integer page, Integer pageSize) {
        return this.messageCommentList(1, page, pageSize);
    }

    // 查询评论
    public PageResult queryMessageCommentList(Integer page, Integer pageSize) {
        return this.messageCommentList(2, page, pageSize);
    }

    // 查询喜欢
    public PageResult queryMessageLoveList(Integer page, Integer pageSize) {
        return this.messageCommentList(3, page, pageSize);
    }

    // 抽取评论查询公共方法
    private PageResult messageCommentList(Integer type, Integer page, Integer pageSize) {
        User user = UserThreadLocal.get();
        PageInfo<Comment> pageInfo = this.quanZiApi.queryCommentListByUser(user.getId(), type, page, pageSize);

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPagesize(pageSize);

        List<Comment> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return pageResult;
        }

        List<Long> userIds = new ArrayList<>();
        for (Comment comment : records) {
            userIds.add(comment.getUserId());
        }

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        List<UserInfo> userInfoList = this.userInfoService.queryUserInfoList(queryWrapper);

        List<MessageLike> messageLikeList = new ArrayList<>();
        for (Comment record : records) {
            for (UserInfo userInfo : userInfoList) {
                if (userInfo.getUserId().longValue() == record.getUserId().longValue()) {

                    MessageLike messageLike = new MessageLike();
                    messageLike.setId(record.getId().toHexString());
                    messageLike.setAvatar(userInfo.getLogo());
                    messageLike.setNickname(userInfo.getNickName());
                    messageLike.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));

                    messageLikeList.add(messageLike);
                    break;
                }
            }
        }

        pageResult.setItems(messageLikeList);
        return pageResult;
    }

    public PageResult queryMessageAnnouncementList(Integer page, Integer pageSize) {

        IPage<Announcement> announcementPage = this.announcementService.queryList(page, pageSize);

        List<MessageAnnouncement> messageAnnouncementList = new ArrayList<>();

        for (Announcement record : announcementPage.getRecords()) {
            MessageAnnouncement messageAnnouncement = new MessageAnnouncement();
            messageAnnouncement.setId(record.getId().toString());
            messageAnnouncement.setTitle(record.getTitle());
            messageAnnouncement.setDescription(record.getDescription());
            messageAnnouncement.setCreateDate(new DateTime(record.getCreated()).toString("yyyy-MM-dd HH:mm"));

            messageAnnouncementList.add(messageAnnouncement);
        }

        PageResult pageResult = new PageResult();
        pageResult.setPage(page);
        pageResult.setPages(0);
        pageResult.setCounts(0);
        pageResult.setPagesize(pageSize);
        pageResult.setItems(messageAnnouncementList);

        return pageResult;
    }
}
