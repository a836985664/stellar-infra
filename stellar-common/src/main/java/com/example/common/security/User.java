package com.example.common.security;

/**
 * JwtService.genreateToken 接收的最小用户信息 DTO。
 * 不引入 entity 依赖，仅承载 token 生成所需字段。
 */
public class User {
    private Long id;
    private String username;
    private String role;

    public User() {}

    public User(Long id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
