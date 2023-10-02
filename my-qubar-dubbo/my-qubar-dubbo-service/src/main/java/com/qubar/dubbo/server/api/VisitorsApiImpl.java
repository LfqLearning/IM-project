package com.qubar.dubbo.server.api;

import com.alibaba.dubbo.config.annotation.Service;
import com.qubar.dubbo.server.pojo.RecommendUser;
import com.qubar.dubbo.server.pojo.Visitors;
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

@Service(version = "1.0.0")
public class VisitorsApiImpl implements VisitorsApi {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public String saveVisitor(Visitors visitors) {

        visitors.setId(ObjectId.get());
        visitors.setDate(System.currentTimeMillis());

        this.mongoTemplate.save(visitors);
        return visitors.getId().toHexString();
    }

    /**
     * 进行分页查询，返回num条数据
     *
     * @param userId
     * @param num
     * @return
     */
    @Override
    public List<Visitors> topVisitor(Long userId, Integer num) {

        Pageable pageable = PageRequest.of(0, num, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageable);
        return commonQueryVisitorList(query);
    }

    /**
     * 查询在date时间之前的访问记录
     *
     * @param userId
     * @param date
     * @return
     */
    @Override
    public List<Visitors> topVisitor(Long userId, Long date) {

        Query query = Query.query(Criteria
                .where("userId")
                .is(userId)
                .and("date")
                .lt(date)).with(Sort.by(Sort.Order.desc("date"))).limit(500);//TODO 对查询数量进行限制
        return commonQueryVisitorList(query);
    }

    /**
     * 分页查询，返回page(第几页)页的pageSize(每页的记录数)条数据
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageInfo<Visitors> topVisitor(Long userId, Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("date")));
        Query query = Query.query(Criteria.where("userId").is(userId)).with(pageable);
        List<Visitors> visitorsList = this.commonQueryVisitorList(query);

        PageInfo<Visitors> pageInfo = new PageInfo<>();

        pageInfo.setRecords(visitorsList);
        pageInfo.setPageSize(pageSize);
        pageInfo.setPageNum(page);
        pageInfo.setTotal(0); //暂时不提供

        return pageInfo;
    }

    /**
     * mongo按条件进行查询，抽取的公共方法
     *
     * @param query
     * @return List<Visitors>
     */
    private List<Visitors> commonQueryVisitorList(Query query) {
        List<Visitors> visitorsList = this.mongoTemplate.find(query, Visitors.class);

        // 遍历查询推荐用户库，获取并设置用户之间的推荐分数
        for (Visitors visitors : visitorsList) {

            Query recommendQuery = Query.query(Criteria
                    .where("userId").is(visitors.getVisitorUserId())
                    .and("toUserId").is(visitors.getUserId()));//toUserId 是当前用户，userId是其他用户
            RecommendUser recommendUser = this.mongoTemplate.findOne(recommendQuery, RecommendUser.class);
            if (null != recommendUser) {
                visitors.setScore(recommendUser.getScore());
            } else {
                //未查询到设置默认值
                visitors.setScore(30d);
            }
        }

        return visitorsList;
    }

}
