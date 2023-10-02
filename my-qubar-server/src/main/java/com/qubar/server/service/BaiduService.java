package com.qubar.server.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qubar.dubbo.server.api.UserLocationApi;
import com.qubar.server.pojo.User;
import com.qubar.server.utils.UserThreadLocal;
import org.springframework.stereotype.Service;

@Service
public class BaiduService {

    @Reference(version = "1.0.0")
    private UserLocationApi userLocationApi;

    public Boolean updateLocation(Double longitude, Double latitude, String address) {
        try {
            User user = UserThreadLocal.get();
            String userLocationId = this.userLocationApi.updateUserLocation(user.getId(), longitude, latitude, address);
            if (null != userLocationId) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
