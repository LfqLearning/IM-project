package com.qubar.dubbo.server.api;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.lang.Nullable;
import com.qubar.dubbo.server.pojo.*;
import com.qubar.dubbo.server.server.IdService;
import com.qubar.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0", timeout = 3000)
public class QuanZiApiImpl implements QuanZiApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdService idService;

    @Override
    public String savePublish(Publish publish) {

        // 校验publish对象
        if (publish.getUserId() == null) {
            return null;
        }
        // ...其他逻辑类似

        // 填充数据
        try {
            publish.setId(ObjectId.get());
            publish.setCreated(System.currentTimeMillis());
            publish.setSeeType(1);//查看权限设置 1-公开，2-私密，3-部分可见，4-不给谁看

            // 增加自增长的pid
            publish.setPid(this.idService.createId("publish", publish.getId().toHexString()));

            // 保存动态信息到发布表——quanzi_publish
            this.mongoTemplate.save(publish);

            // 写入到自己的相册表中——quanzi_album
            Album album = new Album();
            album.setId(ObjectId.get());
            album.setPublishId(publish.getId());//动态id
            album.setCreated(System.currentTimeMillis());

            // 将相册对象写入到MongoDB中
            this.mongoTemplate.save(album, "quanzi_album_" + publish.getUserId());

            // 查询当前用户的好友数据，将动态数据写入到好友的时间线表中
            Criteria criteria = Criteria.where("UserId").is(publish.getUserId());
            List<Users> users = this.mongoTemplate.find(Query.query(criteria), Users.class);// 获取当前用户好友列表
            for (Users user : users) {
                TimeLine timeLine = new TimeLine();
                timeLine.setId(ObjectId.get());
                timeLine.setUserId(publish.getUserId());
                timeLine.setPublishId(publish.getId());
                timeLine.setDate(System.currentTimeMillis());

                mongoTemplate.save(timeLine, "quanzi_time_line_" + user.getFriendId());//将动态信息存入好友时间线表中——time_line_friendId
            }
            return String.valueOf(publish.getId());
        } catch (Exception e) {
            e.printStackTrace();
            //TODO 出错时事务回滚！！！ 重点
        }

        return null;
    }

    @Override
    public PageInfo<Publish> queryPublishList(@Nullable Long userId, Integer page, Integer pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("data")));
        Query query = new Query().with(pageable);

        String tableName = "quanzi_time_line_";
        if (null == userId) {
            //查询推荐动态
            tableName += "recommend";
        } else {
            //查询好友动态
            tableName += userId;
        }
        //查询自己的时间线表
        List<TimeLine> timeLines = this.mongoTemplate.find(query, TimeLine.class, tableName);

        //查询动态信息
        List<ObjectId> ids = new ArrayList<>();
        for (TimeLine timeLine : timeLines) {
            ids.add(timeLine.getPublishId());
        }
        Criteria criteria = Criteria.where("Id").in(ids);
        Query queryPublish = Query.query(criteria).with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishList = this.mongoTemplate.find(queryPublish, Publish.class);

        //封装分页对象
        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setTotal(0); //不提供总数
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(publishList);

        return pageInfo;
    }

    @Override
    public boolean saveLikeComment(Long userId, String publishId) {

        // 判断是否已经点赞，如果已经点赞就返回
        Long count = this.queryCommentCountByUserIdAndPublishId(userId, publishId, 1);
        if (count > 0) {
            return false;
        }
        return this.saveComment(userId, publishId, 1, null);
    }

    @Override
    public boolean removeComment(Long userId, String publishId, Integer commentType) {

        Criteria criteria = Criteria.where("userId").is(userId)
                .and("publishId").is(new ObjectId(publishId))
                .and("commentType").is(commentType);//注意，new ObjectId(publishId)
        Query query = Query.query(criteria);
        DeleteResult deleteResult = this.mongoTemplate.remove(query, Comment.class);

        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean saveLoveComment(Long userId, String publishId) {

        // 判断是否已经喜欢，如果已经喜欢就返回
        Long count = this.queryCommentCountByUserIdAndPublishId(userId, publishId, 3);
        if (count > 0) {
            return false;
        }
        return this.saveComment(userId, publishId, 3, null);
    }

    /**
     * 保存评论
     *
     * @param userId
     * @param publishId
     * @param commentType
     * @param content
     * @return
     */
    @Override
    public boolean saveComment(Long userId, String publishId, Integer commentType, String content) {

        try {
            Comment comment = new Comment();
            comment.setContent(content);
            comment.setIsParent(true);
            comment.setCommentType(commentType);
            comment.setPublishId(new ObjectId(publishId));
            comment.setUserId(userId);
            comment.setId(ObjectId.get());
            comment.setCreated(System.currentTimeMillis());

            // 设置发布人id
            Publish publish = this.mongoTemplate.findById(comment.getPublishId(), Publish.class);
            if (null != publish) {
                comment.setPublishUserId(publish.getUserId());
            } else {
                Video video = this.mongoTemplate.findById(comment.getPublishId(), Video.class);
                if (null != video) {
                    comment.setPublishUserId(video.getUserId());
                }
            }

            this.mongoTemplate.save(comment);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Long queryCommentCount(String publishId, Integer commentType) {

        Criteria criteria = Criteria.where("publishId").is(new ObjectId(publishId))//注意，new ObjectId(publishId)
                .and("commentType").is(commentType);
        Query query = Query.query(criteria);
        return this.mongoTemplate.count(query, Comment.class);
    }

    @Override
    public Publish queryPublishById(String publishId) {
//TODO 修改了参数 new ObjectId(publishId)       return this.mongoTemplate.findById(new ObjectId(publishId), Publish.class);
        return this.mongoTemplate.findById(publishId, Publish.class);
    }

    @Override
    public Long queryCommentCountByUserIdAndPublishId(Long userId, String publishId, Integer commentType) {

        Criteria criteria = Criteria.where("userId").is(userId)
                .and("publishId").is(new ObjectId(publishId))
                .and("commentType").is(commentType);//注意，new ObjectId(publishId)
        Query query = Query.query(criteria);
        return this.mongoTemplate.count(query, Comment.class);
        //List<Comment> comments = this.mongoTemplate.find(query, Comment.class);
    }

    @Override
    public PageInfo<Comment> queryCommentList(String publishId, Integer page, Integer pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, 10, Sort.by(Sort.Order.asc("created")));
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(publishId)).and("commentType").is(2))
                .with(pageRequest);
        List<Comment> commentsList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setTotal(0);
        pageInfo.setPageSize(pageSize);
        pageInfo.setPageNum(page);
        pageInfo.setRecords(commentsList);

        return pageInfo;

    }

    @Override
    public PageInfo<Comment> queryCommentListByUser(Long userId, Integer type, Integer page, Integer pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        //TODO 进行字段映射改动 Criteria.where("publishUserId").is(userId)
        Query query = new Query(Criteria.where("publishUserId").is(userId).and("commentType").is(type)).with(pageRequest);
        List<Comment> commentList = this.mongoTemplate.find(query, Comment.class);

        PageInfo<Comment> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(commentList);
        pageInfo.setTotal(0); //不提供总数

        return pageInfo;
    }

    @Override
    public List<Publish> queryPublishByPIds(List<Long> pids) {

        Criteria criteria = Criteria.where("pid").in(pids);
        Query query = Query.query(criteria);
        List<Publish> publishes = this.mongoTemplate.find(query, Publish.class);
        return null;
    }

    @Override
    public PageInfo<Publish> queryAlbumList(Long userId, Integer page, Integer pageSize) {

        PageInfo<Publish> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0);//不提供总数

// page - 1 = 0,默认查询第一页的数据
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query().with(pageable);
        List<Album> albumList = this.mongoTemplate.find(query, Album.class, "quanzi_album_" + userId);

        if (CollectionUtils.isEmpty(albumList)) {
            return pageInfo;
        }

        //查询相册所对应的动态信息
        List<ObjectId> publishIds = new ArrayList<>();
        for (Album album : albumList) {
            publishIds.add(album.getPublishId());
        }

        Query publishQuery = Query.query(Criteria.where("id").in(publishIds)).with(Sort.by(Sort.Order.desc("created")));
        List<Publish> publishList = this.mongoTemplate.find(publishQuery, Publish.class);

        pageInfo.setRecords(publishList);

        return pageInfo;
    }

}
