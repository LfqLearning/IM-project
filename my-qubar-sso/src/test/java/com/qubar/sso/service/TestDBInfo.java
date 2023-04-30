package com.qubar.sso.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TestDBInfo {

    //生成数据代码
    @Test
    public void testMongoDBData() {
        for (int i = 2; i < 100; i++) {
            int score = RandomUtils.nextInt(30, 99);
            System.out.println("db.recommend_user.insert({\"userId\":" + i +
                    ",\"toUserId\":1,\"score\":"+score+",\"date\":\"2022/12/12\"})");
        }
    }

    //生成数据的代码
    @Test
    public void testMySQLData(){
        System.out.println("INSERT INTO `tb_user` (`id`, `mobile`, `password`, `created`, `updated`) " +
                "VALUES ('1', '15882320653', 'e10adc3949ba59abbe56e057f20f883e', '2022-12-12 16:43:46', '2022-12-12 16:43:46');");
        System.out.println("INSERT INTO `tb_user` (`id`, `mobile`, `password`, `created`, `updated`) " +
                "VALUES ('2', '15882320653', 'e10adc3949ba59abbe56e057f20f883e', '2022-12-12 16:50:32', '2022-12-12 16:50:32');");
        for (int i = 3; i < 100; i++) {
            String mobile = "13"+ RandomStringUtils.randomNumeric(9);
            System.out.println("INSERT INTO `tb_user` (`id`, `mobile`, `password`, `created`, `updated`) VALUES " +
                    "('"+i+"', '"+mobile+"', 'e10adc3949ba59abbe56e057f20f883e', '2022-12-02 16:43:46', '2022-12-02 16:43:46');");
        }
        System.out.println("INSERT INTO `tb_user_info`(`id`, `user_id`, `nick_name`, `logo`, `tags`, `sex`, `age`, `edu`, `city`, " +
                "`birthday`, `cover_pic`, `industry`, `income`, `marriage`, `created`, `updated`) VALUES ('1', '1', 'qubar', " +
                "'https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/logo/21.jpg', '单身,本科,年龄相仿', '1', '30', " +
                "'本科', '北京市-北京城区-东城区', '2022-12-01', 'https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/logo/21.jpg', " +
                "'计算机行业', '30', '已婚', '2022-12-02 16:44:23', '2022-12-02 16:44:23');");
        System.out.println("INSERT INTO `tb_user_info` (`id`, `user_id`, `nick_name`, `logo`, `tags`, `sex`, `age`, `edu`, `city`, " +
                        "`birthday`, `cover_pic`, `industry`, `income`, `marriage`, `created`, `updated`) VALUES ('2', '2', 'qubar_2', " +
                "'https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/logo/22.jpg', '单身,本科,年龄相仿', '1', '30', '本科', " +
                        "'北京市-北京城区-东城区', '202-12-01', 'https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/logo/22.jpg', " +
                "'计算机行业', '30', '已婚', '2022-12-02 16:44:23', '2022-12-02 16:44:23');");
        for (int i = 3; i < 100; i++) {
            String logo = "https://bishe-qubar.oss-cn-hangzhou.aliyuncs.com/images/logo/"+RandomUtils.nextInt(1,20)+".jpg";
            System.out.println("INSERT INTO `tb_user_info` (`id`, `user_id`, `nick_name`, `logo`, `tags`, `sex`, `age`, `edu`, `city`, " +
                    "`birthday`, `cover_pic`, `industry`, `income`, `marriage`, `created`, `updated`) VALUES ('"+i+"', '"+i+"', " +
                            "'qubar_"+i+"', '"+logo+"', '单身,本科,年龄相仿', '1', '"+RandomUtils.nextInt(20,50)+"', '本科', " +
                            "'北京市-北京城区-东城区', '2022-12-01', '"+logo+"', '计算机行业', '40', '未婚', '2022-12-02 16:44:23', '2022-12-02 16:44:23');");
        }
    }
}
