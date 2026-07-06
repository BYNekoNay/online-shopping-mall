package com.pzhu.mall.security;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class RoleCheckAspect {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoleCheckAspect.class);

    @Before("@within(com.pzhu.mall.security.RequireRole) || @annotation(com.pzhu.mall.security.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) return;

        Integer currentRole = LoginUserContext.getCurrentRole();
        if (currentRole == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        for (int allowedRole : requireRole.value()) {
            if (currentRole == allowedRole) return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
