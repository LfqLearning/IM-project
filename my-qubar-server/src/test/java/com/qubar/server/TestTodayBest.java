package com.qubar.server;

import com.qubar.server.enums.SexEnum;
import com.qubar.server.service.TodayBestService;
import com.qubar.server.vo.PageResult;
import com.qubar.server.vo.RecommendUserQueryParam;
import com.qubar.server.vo.TodayBest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TestTodayBest {

    @Autowired
    private TodayBestService todayBestService;

    @Test
    public void testQueryTodayBest(){
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJtb2JpbGUiOiIxNTg4MjMyMDY1MyIsImlkIjoxfQ.9CHF00fek8xxaquAK8cLxspmvlJmytNBGJ8OWNEsAJA";
        TodayBest todayBest = this.todayBestService.queryTodayBest(token);
        System.out.println(todayBest);
    }

    @Test
    public void testQueryTodayBestList(){
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJtb2JpbGUiOiIxNTg4MjMyMDY1MyIsImlkIjoxfQ.9CHF00fek8xxaquAK8cLxspmvlJmytNBGJ8OWNEsAJA";
        PageResult pageResult = this.todayBestService.queryRecommendUserList(new RecommendUserQueryParam(), token);
        System.out.println(pageResult);
    }
}
