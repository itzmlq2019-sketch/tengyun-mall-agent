package com.tengyun.user.controller;

import com.tengyun.user.dto.ApiResponse;
import com.tengyun.user.dto.LoginResponse;
import com.tengyun.user.entity.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.tengyun.user.security.JwtTokenProvider;
import com.tengyun.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/me")
    public ApiResponse<User> getUserInfo(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId
    ) {
        return ApiResponse.success(userService.getById(userId));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @RequestParam @NotBlank(message = "USERNAME_REQUIRED") String username,
            @RequestParam @NotBlank(message = "PASSWORD_REQUIRED") String password
    ) {
        String token = userService.login(username, password);
        return ApiResponse.success(new LoginResponse(token, "Bearer", jwtTokenProvider.getExpireSeconds()));
    }
}
