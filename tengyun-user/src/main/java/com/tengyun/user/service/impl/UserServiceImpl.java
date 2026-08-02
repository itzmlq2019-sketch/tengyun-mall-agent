package com.tengyun.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tengyun.user.entity.User;
import com.tengyun.user.exception.AuthException;
import com.tengyun.user.mapper.UserMapper;
import com.tengyun.user.security.JwtTokenProvider;
import com.tengyun.user.security.PasswordService;
import com.tengyun.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserServiceImpl(PasswordService passwordService, JwtTokenProvider jwtTokenProvider) {
        this.passwordService = passwordService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("USERNAME_OR_PASSWORD_EMPTY");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = this.getOne(queryWrapper, false);
        if (user == null || !passwordService.matches(password, user.getPassword())) {
            throw new AuthException("USERNAME_OR_PASSWORD_INCORRECT");
        }

        return jwtTokenProvider.generateToken(user.getId(), user.getUsername());
    }
}
