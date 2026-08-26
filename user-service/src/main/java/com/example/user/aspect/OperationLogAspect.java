package com.example.user.aspect;

import com.example.user.annotation.OperationLog;
import com.example.user.entity.OperationLogEntity;
import com.example.user.repository.OperationLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRepository logRepository;
    @Around("@annotation(operationLog)") //切点：拦截所有带 @OperationLog 注解的方法
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        Object result  = null;
        Exception ex = null;
        try {
            result  = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            ex = e;
            throw e;
        } finally {
            OperationLogEntity entity  = OperationLogEntity.builder().operation(operationLog.value()).module(operationLog.module()).method(request.getMethod() + " " + request.getRequestURI())
                    .params(operationLog.saveParams() ? "..." : "[敏感参数]").operator("anonymous").ip(getClientIp(request)).duration(System.currentTimeMillis() - start)
                    .result(ex == null ? "SUCCESS" : "FAIL").errorMsg(ex != null ? ex.getMessage() : null).createTime(LocalDateTime.now()).build();
            logRepository.save(entity);

        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        return ip != null ? ip : request.getRemoteAddr();
    }
}