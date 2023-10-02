package com.qubar.dubbo.server.api;

import com.qubar.dubbo.server.pojo.Visitors;
import com.qubar.dubbo.server.vo.PageInfo;

import java.util.List;

public interface VisitorsApi {

    /**
     * 保存来访记录
     *
     * @param visitors
     * @return
     */
    String saveVisitor(Visitors visitors);

    /**
     * 按照时间倒序排序，查询最近的访客信息
     *
     * @param userId
     * @param num
     * @return
     */
    List<Visitors> topVisitor(Long userId, Integer num);

    /**
     * 按照时间倒序排序，查询最近的访客信息(按时间进行查询)
     *
     * @param userId
     * @param date
     * @return
     */
    List<Visitors> topVisitor(Long userId, Long date);

    /**
     * 按照时间倒序排序，查询最近的访客信息，进行分页
     *
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    PageInfo<Visitors> topVisitor(Long userId, Integer page, Integer pageSize);
}
