package com.pzhu.mall.common.exception;

import com.pzhu.mall.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode_setsCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED);
        assertEquals(10002, ex.getCode());
        assertEquals("登录已过期，请重新登录", ex.getMessage());
    }

    @Test
    void constructor_withCustomMessage_overridesMessage() {
        BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED, "Token expired at 2026-01-01");
        assertEquals(10002, ex.getCode());
        assertEquals("Token expired at 2026-01-01", ex.getMessage());
    }

    @Test
    void constructor_withRawCode_works() {
        BusinessException ex = new BusinessException(500, "Internal error");
        assertEquals(500, ex.getCode());
        assertEquals("Internal error", ex.getMessage());
    }

    @Test
    void isRuntimeException() {
        assertThrows(BusinessException.class, () -> {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        });
    }
}
