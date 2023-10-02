package com.qubar.dubbo.server.api;


import com.alibaba.dubbo.config.annotation.Service;
import com.mongodb.client.result.DeleteResult;
import com.qubar.dubbo.server.pojo.UserLike;
import com.qubar.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

@Service(version = "1.0.0")
public class UserLikeApiImpl implements UserLikeApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存用户对其他用户的喜欢操作
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    @Override
    public String saveUserLike(Long userId, Long likeUserId) {
        // 判断喜欢关系是否存在
        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("likeUserId").is(likeUserId));

        if (this.mongoTemplate.count(query, UserLike.class) > 0) {
            return this.mongoTemplate.findOne(query, UserLike.class).getId().toHexString();
        }

        UserLike userLike = new UserLike();
        userLike.setId(ObjectId.get());
        userLike.setLikeUserId(likeUserId);
        userLike.setUserId(userId);
        userLike.setCreated(System.currentTimeMillis());

        this.mongoTemplate.save(userLike);
        return userLike.getId().toHexString();
    }

    /**
     * 是否互相喜欢
     *
     * @param userId
     * @param likeUserId
     * @return
     */
    @Override
    public Boolean isMutualLike(Long userId, Long likeUserId) {
        Criteria criteria1 = Criteria.where("userId").is(userId).and("likeUserId").is(likeUserId);//查询自己是否喜欢对方
        Criteria criteria2 = Criteria.where("userId").is(likeUserId).and("likeUserId").is(userId);//查询对方是否喜欢自己

        Criteria criteria = new Criteria().orOperator(criteria1, criteria2);//TODO 加入集合map，迭代查询
        return this.mongoTemplate.count(Query.query(criteria), UserLike.class) == 2;
    }

    @Override
    public Boolean deleteUserLike(Long userId, Long likeUserId) {
        Query query = Query.query(Criteria.where("userId").is(userId)
                .and("likeUserId").is(likeUserId));
        DeleteResult deleteResult = this.mongoTemplate.remove(query, UserLike.class);
        return deleteResult.getDeletedCount() == 1;
    }

    @Override
    public Long queryEachLikeCount(Long userId) {
        // 思路：首先查询我的喜欢列表，然后，在我的喜欢的人范围内，查询喜欢我的人

        // 查询我的喜欢列表
        Query query = Query.query(Criteria.where("userId").is(userId));
        List<UserLike> userLikes = this.mongoTemplate.find(query, UserLike.class);

        // 收集到我的喜欢列表中的用户id（对方）
        List<Long> likeUserIds = new ArrayList<>();
        for (UserLike userLike : userLikes) {
            likeUserIds.add(userLike.getLikeUserId());
        }

        // 在我的喜欢列表范围内，查询喜欢我的人有哪些
        Query query2 = Query.query(Criteria.where("userId").in(likeUserIds).and("likeUserId").is(userId));

        return this.mongoTemplate.count(query2, UserLike.class);//查询喜欢我的人的数量（方法完整逻辑就是查询互相喜欢的数量）
    }

    @Override
    public Long queryLikeCount(Long userId) {
        return this.mongoTemplate.count(Query.query(Criteria.where("userId").is(userId)), UserLike.class);
    }

    @Override
    public Long queryFanCount(Long userId) {
        return this.mongoTemplate.count(Query.query(Criteria.where("likeUserId").is(userId)), UserLike.class);
    }

    @Override
    public PageInfo<UserLike> queryEachLikeList(Long userId, Integer page, Integer pageSize) {

        // 查询我的喜欢列表
        Query query = Query.query(Criteria.where("userId").is(userId));
        List<UserLike> userLikes = this.mongoTemplate.find(query, UserLike.class);

        // 收集到我的喜欢列表中的用户id（对方）
        List<Long> likeUserIds = new ArrayList<>();
        for (UserLike userLike : userLikes) {
            likeUserIds.add(userLike.getLikeUserId());
        }

        // 在我的喜欢列表范围内，查询喜欢我的人有哪些
        Query query2 = Query.query(Criteria.where("userId").in(likeUserIds).and("likeUserId").is(userId));
        return this.commonQueryList(query2, page, pageSize);
    }

    @Override
    public PageInfo<UserLike> queryLikeList(Long userId, Integer page, Integer pageSize) {
        return this.commonQueryList(Query.query(Criteria.where("userId").is(userId)), page, pageSize);
    }

    @Override
    public PageInfo<UserLike> queryFanList(Long userId, Integer page, Integer pageSize) {
        return this.commonQueryList(Query.query(Criteria.where("likeUserId").is(userId)), page, pageSize);
    }

    /**
     * 按照query进行分页查询 —抽取代码段成为公共方法，实现复用
     *
     * @param query
     * @param page
     * @param pageSize
     * @return
     */
    private PageInfo<UserLike> commonQueryList(Query query, Integer page, Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        query.with(pageRequest); //分页
        List<UserLike> userLikeList = this.mongoTemplate.find(query, UserLike.class);

        PageInfo<UserLike> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(0); //暂时不提供
        pageInfo.setRecords(userLikeList);

        return pageInfo;
    }
}

