package com.tengyun.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tengyun.user.entity.User;
import com.tengyun.user.mapper.UserMapper;
import com.tengyun.user.service.UserService;
import com.tengyun.user.utils.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public String login(String username, String password) {
        // 1. 组装查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username).eq("password", password);

        // 2. 查询数据库（ServiceImpl 已经内置了 baseMapper，直接用）
        User user = this.getOne(queryWrapper);

        // 3. 业务逻辑判断
        if (user == null) {
            return null; // 或者自定义业务异常：throw new BusinessException("用户名或密码错误");
        }

        // 4. 签发令牌
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        System.out.println("🟢 [Service层] 用户 " + username + " 登录成功，令牌已签发。");

        return token;
    }
}