package com.qubar.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsVo {

    private Long id;
    private String strangerQuestion = "";
    private String phone; //用户手机号
    private Boolean likeNotification = true;
    private Boolean reviewNotification = true;
    private Boolean systemNotification = true;

}
