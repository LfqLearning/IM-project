package com.qubar.es.service;

import com.qubar.es.pojo.UserLocationES;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserLocationService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    public boolean updateUserLocation(Long userId, Double longitude, Double latitude, String address) {

        try {
            // 判断索引库是否存在，如果不存在需要新建
            if (!this.elasticsearchTemplate.indexExists(UserLocationES.class)) {
                this.elasticsearchTemplate.createIndex(UserLocationES.class);
            }

            // 判断type类型是否存在，如果不存在，需要新建
            if (!this.elasticsearchTemplate.typeExists("qubar", "user_location")) {
                this.elasticsearchTemplate.putMapping(UserLocationES.class);
            }


            // 判断用户的位置信息是否存在，如果存在，进行更新操作，否则进行插入新的数据
            GetQuery getQuery = new GetQuery();
            getQuery.setId(userId.toString());
            UserLocationES userLocationES = this.elasticsearchTemplate.queryForObject(getQuery, UserLocationES.class);
            if (null == userLocationES) {
                // 新建一个位置对象
                UserLocationES userLocationES1 = new UserLocationES();
                userLocationES1.setUserId(userId);
                userLocationES1.setAddress(address);
                userLocationES1.setLocation(new GeoPoint(latitude, longitude));
                userLocationES1.setCreated(System.currentTimeMillis());
                userLocationES1.setUpdated(userLocationES1.getCreated());
                userLocationES1.setLastUpdated(userLocationES1.getCreated());

                IndexQuery indexQuery = new IndexQueryBuilder().withObject(userLocationES1).build();
                String index = this.elasticsearchTemplate.index(indexQuery); //保存数据到elasticsearch中

            } else {
                // 更新数据操作
                //TODO es对数据进行操作，重点！！！
                Map<String, Object> map = new HashMap<>();
                map.put("location", new GeoPoint(latitude, longitude));
                map.put("address", address);
                map.put("update", System.currentTimeMillis());
                map.put("lastUpdated", userLocationES.getUpdated());

                UpdateRequest updateRequest = new UpdateRequest();
                updateRequest.doc(map);
                UpdateQuery updateQuery = new UpdateQueryBuilder().withId(userId.toString())
                        .withClass(UserLocationES.class)
                        .withUpdateRequest(updateRequest).build();
                this.elasticsearchTemplate.update(updateQuery);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 通过Id查询用户地理位置
     *
     * @param userId
     * @return UserLocationES对象
     */
    public UserLocationES queryByUserId(Long userId) {

        GetQuery getQuery = new GetQuery();
        getQuery.setId(userId.toString());
        return this.elasticsearchTemplate.queryForObject(getQuery, UserLocationES.class);
    }

    /**
     * 查询附近的人
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param distance  距离（单位：米）
     * @param page      页数
     * @param pageSize  页面大小
     * @return Page<UserLocationES> 分页对象
     */
    public Page<UserLocationES> queryUserFromLocation(Double longitude, Double latitude, Double distance, Integer page, Integer pageSize) {

        String fieldName = "location";

        // 1.NativeSearchQuery实现了SearchQuery接口
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();

        // 1.1分页
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        nativeSearchQueryBuilder.withPageable(pageRequest);

        // 2.定义bool查询
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // 3.geo查询，定义中心点，指定查询范围
        GeoDistanceQueryBuilder geoDistanceQueryBuilder = new GeoDistanceQueryBuilder(fieldName);//geoPoint对象对应的字段
        geoDistanceQueryBuilder.point(latitude, longitude);
        geoDistanceQueryBuilder.distance(distance / 1000, DistanceUnit.KILOMETERS);

        // 4.searchQuery设置查询clause
        boolQueryBuilder.must(geoDistanceQueryBuilder);
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);

        // 5.按照距离升序
        GeoDistanceSortBuilder geoDistanceSortBuilder = new GeoDistanceSortBuilder(fieldName, latitude, longitude);
        geoDistanceSortBuilder.unit(DistanceUnit.KILOMETERS); //距离单位
        geoDistanceSortBuilder.order(SortOrder.ASC); //升序

        nativeSearchQueryBuilder.withSort(geoDistanceSortBuilder);

        return this.elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), UserLocationES.class);
    }
}
