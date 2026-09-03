package com.example.user.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)// 只能加在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时保留（AOP 反射读它必须加这个）
@Documented                          // 生成 Javadoc
public @interface OperationLog {
    String value() default "";        // 操作描述，如 "创建用户"
    String module() default "";       // 模块，如 "用户管理"
    boolean saveParams() default true; // 是否记录入参（敏感接口设 false）
}
