package com.tengyun.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tengyun.product.entity.StockDeduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockDeductionMapper extends BaseMapper<StockDeduction> {

    @Select("SELECT * FROM product_stock_deduction WHERE request_id = #{requestId}")
    StockDeduction selectByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM product_stock_deduction WHERE request_id = #{requestId} FOR UPDATE")
    StockDeduction selectByRequestIdForUpdate(@Param("requestId") String requestId);
}
