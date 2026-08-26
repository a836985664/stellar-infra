package com.example.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_log")  // 表名
@Data                           // Lombok 自动生成 getter/setter
@NoArgsConstructor
@AllArgsConstructor
@Builder                        // 后面用建造者模式组装日志很方便
public class OperationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 接口描述：用户注册 / AI对话
    private String operation;

    // 模块名：用户管理 / 客服
    private String module;

    // HTTP 方法 + URI：POST /users/chat
    private String method;

    // 请求参数（JSON 字符串，注意可能被截断）
    @Column(columnDefinition = "TEXT")  // PostgreSQL 大文本，防止超长参数报错
    private String params;

    // 操作人 ID 或名称（先用占位符，比如 "anonymous"，后面接 ThreadLocal 用户）
    private String operator;

    // 请求 IP
    private String ip;

    // 接口耗时（毫秒）
    private Long duration;

    // 执行结果：SUCCESS / FAIL
    private String result;

    // 错误信息（如果失败的话）
    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    // 创建时间
    private LocalDateTime createTime;
}