package com.qubar.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.qubar.dubbo.server.pojo.Users;
import com.qubar.dubbo.server.vo.PageInfo;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;


@Service(version = "1.0.0", timeout = 3000)
public class UsersApiImpl implements UsersApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 在MongoDB中添加好友
     *
     * @param users
     * @return
     */
    @Override
    public String saveUsers(Users users) {

        // 校验
        if (users.getUserId() == null || users.getFriendId() == null) {
            return null;
        }

        // 判断好友是否已存在
        Query query = Query.query(Criteria.where("userId").is(users.getUserId()).and("friendId").is(users.getFriendId()));
        Users oneFriend = this.mongoTemplate.findOne(query, Users.class);
        if (null != oneFriend) {
            // 该好友已经存在
            return null;
        }

        users.setId(ObjectId.get());
        users.setDate(System.currentTimeMillis());

        // 将数据写入到MongoDB中
        this.mongoTemplate.save(users);

        return users.getId().toHexString();
    }

    /**
     * 根据用户id查询Users列表——在qubar_users中全局查找
     *
     * @param userId
     * @return
     */
    @Override
    public List<Users> queryAllUsersList(Long userId) {

        Query query = Query.query(Criteria.where("userId").is(userId));
        return this.mongoTemplate.find(query, Users.class);
    }

    @Override
    public PageInfo<Users> queryUsersList(Long userId, Integer page, Integer pageSize) {

        PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("created")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageRequest);

        List<Users> usersList = this.mongoTemplate.find(query, Users.class);

        PageInfo<Users> pageInfo = new PageInfo<>();
        pageInfo.setPageNum(page);
        pageInfo.setPageSize(pageSize);
        pageInfo.setRecords(usersList);
        pageInfo.setTotal(0); //不提供总数

        return pageInfo;
    }
}
