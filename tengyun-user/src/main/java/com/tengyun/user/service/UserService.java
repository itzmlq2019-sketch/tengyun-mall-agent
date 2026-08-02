package com.tengyun.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tengyun.user.entity.User;

public interface UserService extends IService<User> {
    String login(String username, String password);
}
