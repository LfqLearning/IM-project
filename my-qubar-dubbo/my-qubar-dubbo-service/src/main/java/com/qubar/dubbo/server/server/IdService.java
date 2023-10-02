package com.qubar.dubbo.server.server;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 原理：使用Redis的自增长类型，实现自增长的Id
 */
@Service
public class IdService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 生成自增长的id
     *
     * @return
     */
    public Long createId(String type, String objectId) {

        type = StringUtils.upperCase(type);

        String hashKey = "QUBAR_HASH_ID_" + type;
        // 如果ObjectId已经存在就返回对应Id
        if (this.redisTemplate.opsForHash().hasKey(hashKey, objectId)) {
            return Long.parseLong(Objects.requireNonNull(this.redisTemplate.opsForHash().get(hashKey, objectId)).toString());
        }

        String key = "QUBAR_ID_" + type;
        Long id = this.redisTemplate.opsForValue().increment(key);

        // 将生成的id写入到hash表中
        this.redisTemplate.opsForHash().put(hashKey, objectId, String.valueOf(id));

        return id;
    }
}
