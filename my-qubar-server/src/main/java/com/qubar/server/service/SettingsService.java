package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.server.mapper.SettingsMapper;
import com.qubar.server.pojo.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    @Autowired(required = false)
    private SettingsMapper settingsMapper;

    public Settings querySettingsByUserId(Long userId) {
        QueryWrapper<Settings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.settingsMapper.selectOne(queryWrapper);
    }

    public void updateNotification(Long userId, Boolean likeNotification, Boolean reviewNotification, Boolean systemNotification) {
        Settings settings = new Settings();
        settings.setLikeNotification(likeNotification);
        settings.setReviewNotification(reviewNotification);
        settings.setSystemNotification(systemNotification);

        QueryWrapper<Settings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        this.settingsMapper.update(settings, queryWrapper);

    }
}
