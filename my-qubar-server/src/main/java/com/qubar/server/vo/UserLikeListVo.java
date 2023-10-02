package com.qubar.server.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页查询，返回被查询用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLikeListVo {

    private Long id; //被查询用户id
    private String avatar;
    private String nickname;
    private String gender;
    private Integer age;
    private String city;
    private String education;
    private Integer marriage; //婚姻状态（0未婚，1已婚）
    private Integer matchRate; //匹配度

}