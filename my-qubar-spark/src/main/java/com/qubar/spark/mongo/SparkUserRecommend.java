package com.qubar.spark.mongo;

import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel;
import org.apache.spark.mllib.recommendation.Rating;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import redis.clients.jedis.Tuple;
import scala.Tuple2;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SparkUserRecommend {

    public static void main(String[] args) throws IOException {

        // 加载外部的配置文件
        InputStream inputStream = SparkQuanZi.class.getClassLoader().getResourceAsStream("app.properties");
        Properties properties = new Properties();
        properties.load(inputStream);

        // 构建Spark配置
        SparkConf sparkConf = new SparkConf()
                .setAppName("SparkQuanZi")
                .setMaster("local[*]")
                .set("spark.mongodb.output.uri", properties.getProperty("spark.mongodb.output.user.uri"));

        // 加载mysql数据
        SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        String url = properties.getProperty("jdcb.rul");

        // 设置数据库连接信息
        Properties connectionProperties = new Properties();
        connectionProperties.put("driver", properties.getProperty("jdbc.driver-class-name"));
        connectionProperties.put("user", properties.getProperty("jdbc.username"));
        connectionProperties.put("password", properties.getProperty("jdbc.password"));

        JavaRDD<Row> userInfoRdd = sparkSession.read().jdbc(url, "tb_user_info", connectionProperties).toJavaRDD();
        // userInfoRdd.foreach(System.out::println);//方法引用替代lambda表达式

        // 获取用户ids列表
        List<Long> userIds = userInfoRdd.map(row -> {
            return row.getLong(1);
        }).collect();

        // 计算出这张数据表的笛卡尔积
        JavaPairRDD<Row, Row> cartesian = userInfoRdd.cartesian(userInfoRdd);


        // 计算用户的相似度
        JavaPairRDD<Long, Rating> javaPairRDDScore = cartesian.mapToPair(row -> {
            Row row1 = row._1;
            Row row2 = row._2;

            Long userId1 = row1.getLong(1);
            Long userId2 = row2.getLong(1);

            Long key = userId1 + userId2 + RandomUtils.nextLong(0, 10000);
            // 自己与自己的对比
            if (userId1.longValue() == userId2.longValue()) {

                return new Tuple2<>(key % 10, new Rating(userId1.intValue(), userId2.intValue(), 0d));
            }

            double score = 0;

            // 计算年龄差得分
            int ageDiff = Math.abs(row1.getInt(6) - row2.getInt(6));
            if (ageDiff <= 2) {
                score += 20;
            } else if (ageDiff <= 5) {
                score += 10;
            } else if (ageDiff <= 10) {
                score += 5;
            }

            // 计算性别得分
            if (row1.getInt(5) != row2.getInt(5)) {
                score += 30;
            }

            // 计算城市是否相同得分
            String city1 = StringUtils.substringBefore(row1.getString(8), "-");
            String city2 = StringUtils.substringBefore(row2.getString(8), "-");
            if (StringUtils.equals(city1, city2)) {
                score += 20;
            }

            // 计算学历
            String edu1 = row1.getString(7);
            String edu2 = row2.getString(7);
            if (StringUtils.equals(edu1, edu2)) {
                score += 20;
            }

            Rating rating = new Rating(userId1.intValue(), userId2.intValue(), score);
            return new Tuple2<>(key % 10, rating);
        });

        // MLLib进行计算最佳的推荐模型
        MLlibRecommend mLlibRecommend = new MLlibRecommend();
        MatrixFactorizationModel bestModel = mLlibRecommend.bestModel(javaPairRDDScore);

        /* 测试训练效果
        Rating[] ratings = bestModel.recommendProducts(1, 20);
        for (Rating rating : ratings) {
            System.out.println(rating.user() + "--" + rating.product() + "--" + rating.rating());
        }*/

// 将数据写入到MongoDB中
        JavaSparkContext jsc = new JavaSparkContext(sparkSession.sparkContext());
        for (Long userId : userIds) {
            Rating[] ratings = bestModel.recommendProducts(userId.intValue(), 50);

            JavaRDD<Document> documentJavaRDD = jsc.parallelize(Arrays.asList(ratings)).map(v -> {
                Document document = new Document();
                document.put("_id", ObjectId.get());
                document.put("userId", v.product());
                document.put("toUserId", v.user());
                // score保留两位小数
                double score = new BigDecimal(v.rating()).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
                document.put("score", score);
                document.put("date", new DateTime().toString("yyyy/MM/dd"));

                return document;
            });
            MongoSpark.save(documentJavaRDD);
        }

    }
}
