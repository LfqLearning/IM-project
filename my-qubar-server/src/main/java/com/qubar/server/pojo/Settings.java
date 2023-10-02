package com.qubar.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings extends BasePojo {

    private Long id;
    private Long userId;
    private Boolean likeNotification; //喜欢通知
    private Boolean reviewNotification; //评论通知
    private Boolean systemNotification; //系统通知

}