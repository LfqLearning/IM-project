package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.vo.UserLocationVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TestUserLocationApi {

    @Autowired(required = false)
    private UserLocationApi userLocationApi;

    @Test
    public void testUpdateUserLocation() {
        String userLocationId = this.userLocationApi.updateUserLocation(2L, 3D, 4D, "北京");
        System.out.println(userLocationId);
    }

    @Test
    public void testQueryByUserId() {
        UserLocationVo userLocationVo = this.userLocationApi.queryByUserId(5L);
        System.out.println(userLocationVo);
    }

    public void testQueryUserFromLocation() {
        List<UserLocationVo> userLocationVosList = this.userLocationApi.queryUserFromLocation(8D, 9D, 1500);
        for (UserLocationVo userLocationVo : userLocationVosList) {
            System.out.println(userLocationVo);
        }
    }
}
