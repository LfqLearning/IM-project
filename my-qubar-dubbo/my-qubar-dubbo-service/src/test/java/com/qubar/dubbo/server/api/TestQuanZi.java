package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.pojo.Publish;
import com.qubar.dubbo.server.pojo.TimeLine;
import com.qubar.dubbo.server.vo.PageInfo;
import org.apache.commons.lang3.RandomUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TestQuanZi {

    @Autowired(required = false)
    private QuanZiApi quanZiApi;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void testSavePublish() {
        Publish publish = new Publish();
        publish.setUserId(1L);
        publish.setLocationName("北京市");
        publish.setSeeType(1);
        publish.setText("今天天气相当不错~~~");
        publish.setMedias(Arrays.asList("https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/quanzi/1.jpg"));
        String result = this.quanZiApi.savePublish(publish);
        System.out.println(result);
    }

    @Test
    public void testRecommendPublish() {
        //查询用户id为2的时间表动态作为推荐动态的数据
        PageInfo<Publish> pageInfo = this.quanZiApi.queryPublishList(2L, 1, 10);
        for (Publish record : pageInfo.getRecords()) {

            TimeLine timeLine = new TimeLine();
            timeLine.setId(ObjectId.get());
            timeLine.setPublishId(record.getId());
            timeLine.setUserId(record.getUserId());
            timeLine.setDate(System.currentTimeMillis());

            this.mongoTemplate.save(timeLine, "quanzi_time_line_recommend");
        }
    }

    @Test
    public void testSavePublish2() {
        for (int i = 0; i < 100; i++) {
            Publish publish = new Publish();
            publish.setUserId(RandomUtils.nextLong(1, 10));
            publish.setPid(Long.valueOf(1000 + i));
            publish.setLocationName("北京市");
            publish.setSeeType(1);
            publish.setText("今天天气很nice~ " + i);
            publish.setMedias(Arrays.asList("http://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/2019/10/01/15699262101875720.png",
                    "http://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/2019/10/01/15699262107836768.png",
                    "http://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/2019/10/01/15699262108584571.png",
                    "http://itcast-tanhua.oss-cn-shanghai.aliyuncs.com/images/2019/10/01/15699262109221190.png"));
            String publishId = this.quanZiApi.savePublish(publish);
            System.out.println(publishId);
        }
    }

    @Test
    public void testSaveRecommend() {
        for (int i = 0; i < 1000; i++) {
            RecommendQuanZi recommendQuanZi = new RecommendQuanZi();
            recommendQuanZi.setDate(System.currentTimeMillis());
            recommendQuanZi.setId(ObjectId.get());
            recommendQuanZi.setScore(Double.valueOf(RandomUtils.nextInt(0, 15)));
            recommendQuanZi.setUserId(RandomUtils.nextLong(1, 10));
            recommendQuanZi.setPublishId(RandomUtils.nextLong(1000, 1099));

            this.mongoTemplate.save(recommendQuanZi, "recommend_quanzi_20230116");
        }
    }

    @Test
    public void testQueryAlbumList() {
        PageInfo<Publish> pageInfo = this.quanZiApi.queryAlbumList(57L, 1, 10);
        for (Publish publish : pageInfo.getRecords()) {
            System.out.println(publish);
        }
    }
}
