package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qubar.server.mapper.AnnouncementMapper;
import com.qubar.server.pojo.Announcement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {

    @Autowired(required = false)
    private AnnouncementMapper announcementMapper;


    public IPage<Announcement> queryList(Integer page, Integer pageSize) {

        QueryWrapper<Announcement> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created");
        return this.announcementMapper.selectPage(new Page<>(page, pageSize), queryWrapper);
    }
}