package com.qubar.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qubar.server.mapper.QuestionMapper;
import com.qubar.server.pojo.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {

    @Autowired(required = false)
    private QuestionMapper questionMapper;

    public Question queryQuestion(Long userId) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return this.questionMapper.selectOne(queryWrapper);
    }

    /**
     * 设置陌生人问题
     *
     * @param userId
     * @param content
     */
    public void save(Long userId, String content) {
        //判断问题是否存在，如果存在，进行修改，如果不存在，就新增操作
        Question question = this.queryQuestion(userId);
        if (null != question) {
            question.setTxt(content);
            this.questionMapper.updateById(question);
        } else {
            question = new Question();
            question.setUserId(userId);
            question.setTxt(content);

            this.questionMapper.insert(question);
        }
    }
}
