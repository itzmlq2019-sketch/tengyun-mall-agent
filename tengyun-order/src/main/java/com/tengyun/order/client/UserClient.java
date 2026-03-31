package com.tengyun.order.client;

import com.tengyun.order.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name属性必须和 Nacos 里的 user-service 服务名一模一样！
@FeignClient(name = "user-service")
public interface UserClient {

    // 这里的方法签名和路径，完全照抄 UserController 里的那个接口
    @GetMapping("/user/info/{id}")
    UserDTO getUserInfo(@PathVariable("id") Long id);
}