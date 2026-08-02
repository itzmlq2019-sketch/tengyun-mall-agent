package com.tengyun.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tengyun.order.entity.OrderDeadLetterLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDeadLetterLogMapper extends BaseMapper<OrderDeadLetterLog> {
}
