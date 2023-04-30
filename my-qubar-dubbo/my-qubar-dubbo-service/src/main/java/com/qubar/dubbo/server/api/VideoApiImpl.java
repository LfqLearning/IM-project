package com.qubar.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.qubar.dubbo.server.pojo.FollowUser;
import com.qubar.dubbo.server.pojo.Video;
import com.qubar.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@Service(version = "1.0.0", timeout = 3000)
public class VideoApiImpl implements VideoApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Boolean saveVideo(Video video) {

        if (video.getUserId() == null) {
            return false;
        }

        video.setId(ObjectId.get());
        video.setCreated(System.currentTimeMillis());
        this.mongoTemplate.save(video);

        return true;
    }

    @Override
    public PageInfo<Video> queryVideoList(Integer page, Integer pageSize) {

        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = new Query().with(pageable);
        List<Video> videos = this.mongoTemplate.find(query, Video.class);
        PageInfo<Video> pageInfo = new PageInfo<>();
        pageInfo.setRecords(videos);
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //不提供总数

        return pageInfo;
    }

    @Override
    public Boolean followUser(Long userId, Long followUserId) {

        try {
            FollowUser followUser = new FollowUser();
            followUser.setId(ObjectId.get());
            followUser.setUserId(userId);
            followUser.setFollowUserId(followUserId);
            followUser.setCreated(System.currentTimeMillis());
            this.mongoTemplate.save(followUser);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Boolean disFollowUser(Long userId, Long followUserId) {

        Query query = Query.query(Criteria.where("userId").is(userId).and("followUserId").is(followUserId));
        DeleteResult deleteResult = this.mongoTemplate.remove(query, FollowUser.class);

        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public Boolean queryFollowUserByUserIdAndFollowUserId(Long userId, Long followUserId) {

        Query query = Query.query(Criteria.where("userId").is(userId).and("followUserId").is(followUserId));
        long hasFollowUser = this.mongoTemplate.count(query, FollowUser.class);

        return hasFollowUser > 0;
    }

    @Override
    public Long queryFollowUserCount(Long followUserId) {

        Query query = Query.query(Criteria.where("followUserId").is(followUserId));
        long followUserCount = this.mongoTemplate.count(query, FollowUser.class);

        return followUserCount;
    }
}
