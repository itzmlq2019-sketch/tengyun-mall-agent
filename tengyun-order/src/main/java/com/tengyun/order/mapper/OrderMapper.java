package com.tengyun.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tengyun.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    // 基础的 CRUD 已经全包了，不需要手写 SQL
}