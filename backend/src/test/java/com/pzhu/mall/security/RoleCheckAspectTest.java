package com.pzhu.mall.security;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleCheckAspectUnitTest {

    private final RoleCheckAspect aspect = new RoleCheckAspect();

    @Test
    void noRequireRoleAnnotation_doesNothing() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method publicMethod = PublicController.class.getMethod("publicMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(publicMethod);
        when(jp.getTarget()).thenReturn(new PublicController());

        // Should not throw - no @RequireRole on class or method
        aspect.checkRole(jp);
    }

    @Test
    void classLevelRequireRole_withMatchingRole_doesNothing() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method anyMethod = MerchantController.class.getMethod("anyMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(anyMethod);
        when(jp.getTarget()).thenReturn(new MerchantController());

        LoginUserContext.set(1L, 2); // role=2 (merchant)
        aspect.checkRole(jp);
        // No exception = pass
    }

    @Test
    void classLevelRequireRole_withWrongRole_throwsForbidden() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method anyMethod = MerchantController.class.getMethod("anyMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(anyMethod);
        when(jp.getTarget()).thenReturn(new MerchantController());

        LoginUserContext.set(1L, 1); // role=1 (consumer), not merchant
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    @Test
    void classLevelRequireRole_noLoginContext_throwsUnauthorized() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method anyMethod = MerchantController.class.getMethod("anyMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(anyMethod);
        when(jp.getTarget()).thenReturn(new MerchantController());

        LoginUserContext.clear();
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    void methodLevelRequireRole_overridesClassLevel() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method adminMethod = MixedController.class.getMethod("adminMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(adminMethod);
        when(jp.getTarget()).thenReturn(new MixedController());

        LoginUserContext.set(1L, 3); // admin
        aspect.checkRole(jp);
        // No exception = pass
    }

    @Test
    void methodLevelRequireRole_wrongRole_throwsForbidden() throws Exception {
        JoinPoint jp = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method adminMethod = MixedController.class.getMethod("adminMethod");

        when(jp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(adminMethod);
        when(jp.getTarget()).thenReturn(new MixedController());

        LoginUserContext.set(1L, 2); // merchant trying admin method
        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.checkRole(jp));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }

    // --- Test fixture classes ---

    @com.pzhu.mall.security.RequireRole(2)
    static class MerchantController {
        public void anyMethod() {}
    }

    @com.pzhu.mall.security.RequireRole(3)
    static class MixedController {
        public void anyMethod() {}
        @com.pzhu.mall.security.RequireRole(3)
        public void adminMethod() {}
    }

    static class PublicController {
        public void publicMethod() {}
    }
}
