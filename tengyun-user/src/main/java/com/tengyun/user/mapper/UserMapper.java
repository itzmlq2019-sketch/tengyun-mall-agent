package com.tengyun.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tengyun.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 后，单表的 CRUD 操作就已经全部自动生成了！
}