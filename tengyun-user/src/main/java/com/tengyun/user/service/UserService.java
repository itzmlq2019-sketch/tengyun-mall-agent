package com.tengyun.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tengyun.user.entity.User;

public interface UserService extends IService<User> {
    /**
     * 用户登录业务
     * @return 返回生成的 JWT Token，登录失败则抛出异常或返回 null
     */
    String login(String username, String password);
}