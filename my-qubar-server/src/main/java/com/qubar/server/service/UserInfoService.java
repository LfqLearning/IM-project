package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.server.mapper.UserInfoMapper;
import com.qubar.server.pojo.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 根据Id从mysql数据库查询用户info
 * return userInfo
 */
@Service
public class UserInfoService {

    @Autowired(required = false)
    private UserInfoMapper userInfoMapper;

    /**
     * 查询数据库，查找用户的信息数据
     * 说明：为了简单处理，直接查询数据库。建议：编写dubbo服务，进行调用
     *
     * @param id
     * @return
     */
    public UserInfo queryUserById(Long id) {

        return this.userInfoMapper.selectById(id);
    }

    /**
     * 查询用户信息列表
     *
     * @param queryWrapper
     * @return
     */
    public List<UserInfo> queryUserInfoList(QueryWrapper<UserInfo> queryWrapper) {

        return this.userInfoMapper.selectList(queryWrapper);
    }

    /**
     * 更新用户数据
     *
     * @param userInfo
     * @return
     */
    public Boolean updateUserInfoByUserId(UserInfo userInfo) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userInfo.getUserId());
        return this.userInfoMapper.update(userInfo, queryWrapper) > 0;
    }

}
