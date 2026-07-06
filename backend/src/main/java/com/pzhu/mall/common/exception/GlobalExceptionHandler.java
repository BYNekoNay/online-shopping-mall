package com.pzhu.mall.common.exception;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(com.pzhu.mall.common.exception.BusinessException.class)
    public Result<Void> handleBusinessException(com.pzhu.mall.common.exception.BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected error", e);
        return Result.error(ErrorCode.SYSTEM_BUSY.getCode(), ErrorCode.SYSTEM_BUSY.getMessage());
    }
}
