package com.qubar.dubbo.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "recommend_user")
public class RecommendUser implements java.io.Serializable {

    //主键ID需要注释
    @Id
    private ObjectId id; //主键ID
    @Indexed
    private Long userId; //被推荐的用户ID
    private Long toUserId; //用户ID
    @Indexed
    private Double score; //推荐得分
    private String date; //日期
}
