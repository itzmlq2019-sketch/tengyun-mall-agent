package com.tengyun.user.controller;

import com.tengyun.user.entity.User;
import com.tengyun.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 接口 1：查询用户信息 (Feign 调用)
     */
    @GetMapping("/info/{id}")
    public User getUserInfo(@PathVariable("id") Long id) {
        // 直接调用 Service 继承自 IService 的方法
        return userService.getById(id);
    }

    /**
     * 接口 2：用户登录 (前端调用)
     */
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // 🌟 逻辑全部下沉，Controller 只负责调度
        String token = userService.login(username, password);

        if (token == null) {
            return "登录失败：账号或密码错误！";
        }
        return token;
    }
}