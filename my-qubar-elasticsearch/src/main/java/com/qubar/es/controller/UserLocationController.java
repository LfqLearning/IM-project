package com.qubar.es.controller;

import com.qubar.es.pojo.UserLocationES;
import com.qubar.es.service.UserLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("es/user/location")
public class UserLocationController {

    @Autowired
    private UserLocationService userLocationService;

    /**
     * 更新用户地理位置
     *
     * @param paramMap
     * @return
     */
    @PostMapping()
    public ResponseEntity<UserLocationES> updateUserLocation(@RequestBody Map<String, Object> paramMap) {
        try {
            Long userId = Long.valueOf(String.valueOf(paramMap.get("userId")));
            Double longitude = Double.valueOf(paramMap.get("longitude").toString());
            Double latitude = Double.valueOf(paramMap.get("latitude").toString());
            String address = paramMap.get("address").toString();
            boolean result = this.userLocationService.updateUserLocation(userId, longitude, latitude, address);
            if (result) {
                return ResponseEntity.ok(null);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).build();
    }

    /**
     * 查询用户的地理位置
     *
     * @param userId
     * @return
     */
    @GetMapping("{userId}")
    public ResponseEntity<UserLocationES> queryUserLocation(@PathVariable("userId") Long userId) {
        try {
            UserLocationES userLocationES = this.userLocationService.queryByUserId(userId);
            if (null == userLocationES) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(userLocationES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).build();
    }

    /**
     * 搜索附近的人
     *
     * @param param
     * @return
     */
    @PostMapping("list")
    public ResponseEntity<List<UserLocationES>> queryUserFromLocation(@RequestBody Map<String, Object> param) {
        try {
            Double longitude = Double.valueOf(param.get("longitude").toString());
            Double latitude = Double.valueOf(param.get("latitude").toString());
            Double distance = Double.valueOf(param.get("distance").toString());
            Integer page = param.get("page") == null ? 1 : Integer.valueOf(param.get("page").toString());
            Integer pageSize = param.get("pageSize") == null ? 100 : Integer.valueOf(param.get("pageSize").toString());

            Page<UserLocationES> userLocationES = this.userLocationService.queryUserFromLocation(longitude, latitude, distance, page, pageSize);
            return ResponseEntity.ok(userLocationES.getContent());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).build();
    }
}
