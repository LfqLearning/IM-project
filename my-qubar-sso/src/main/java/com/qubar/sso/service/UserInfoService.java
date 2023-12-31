package com.qubar.sso.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.sso.enums.SexEnum;
import com.qubar.sso.mapper.UserInfoMapper;
import com.qubar.sso.pojo.User;
import com.qubar.sso.pojo.UserInfo;
import com.qubar.sso.vo.PicUploadResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class UserInfoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInfoService.class);
    @Autowired(required = false)
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private FaceEngineService faceEngineService;
    @Autowired
    private PicUploadService picUploadService;

    /**
     * 完善个人信息
     *
     * @return
     */
    public Boolean saveUserInfo(Map<String, String> param, String token) {
        User user = this.userService.queryUserByToken(token);
        if (user == null) {
            return false;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setSex(StringUtils.equals(param.get("gender"), "man") ? SexEnum.MAN : SexEnum.WOMAN);
        userInfo.setNickName(param.get("nickname"));
        userInfo.setBirthday(param.get("birthday"));
        userInfo.setCity(param.get("city"));
// 保存UserInfo数据到数据库
        this.userInfoMapper.insert(userInfo);
        return true;
    }

    /**
     * 保存用户头像
     *
     * @param file
     * @param token
     * @return
     */
    public Boolean saveLogo(MultipartFile file, String token) {
        User user = this.userService.queryUserByToken(token);
        if (user == null) {
            return false;
        }

        try {
//校验头像是否为人像
            boolean isPortrait = this.faceEngineService.checkIsPortrait(file.getBytes());
            if (!isPortrait) {
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("检测人像图片出错!", e);
            return false;
        }

// 图片上传到阿里云OSS
        PicUploadResult uploadResult = this.picUploadService.upload(file);

        UserInfo userInfo = new UserInfo();
        userInfo.setLogo(uploadResult.getName());

        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", user.getId());

        this.userInfoMapper.update(userInfo, queryWrapper);
        return true;
    }
}
