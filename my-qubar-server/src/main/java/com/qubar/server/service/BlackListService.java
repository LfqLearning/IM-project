package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qubar.server.mapper.BlackListMapper;
import com.qubar.server.pojo.BlackList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlackListService {

    @Autowired(required = false)
    private BlackListMapper blackListMapper;

    public IPage<BlackList> queryPageList(Long userId, Integer page, Integer pageSize) {
        QueryWrapper<BlackList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        //排序，根据创建时间倒序排序
        queryWrapper.orderByDesc("created");

        // 分页的参数
        Page<BlackList> pager = new Page<>(page, pageSize);

        return this.blackListMapper.selectPage(pager, queryWrapper);
    }

    /**
     * 把其他用户移除黑名单
     *
     * @param userId
     * @param blackListUserId
     * @return
     */
    public Boolean delBlacklist(Long userId, Long blackListUserId) {
        QueryWrapper<BlackList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("black_user_id", blackListUserId);
        return this.blackListMapper.delete(queryWrapper) > 0;
    }
}
