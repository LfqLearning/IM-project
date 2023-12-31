package com.qubar.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 好友列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "qubar_users")
public class Users implements java.io.Serializable {

    private static final long serialVersionUID = 6003135946820874230L;

    private ObjectId id;
    private Long userId; //用户id
    private Long friendId; //好友id
    private Long date; //时间
}