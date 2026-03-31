package com.tengyun.order.dto;

public class UserDTO {
    private Long id;
    private String username;
    private String phone;
    // 为了代码简洁，这里手写 Getter/Setter。如果配了 Lombok，直接用 @Data 注解即可
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}