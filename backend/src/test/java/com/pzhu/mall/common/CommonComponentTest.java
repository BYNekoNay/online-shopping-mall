package com.pzhu.mall.common;

import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.exception.GlobalExceptionHandler;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 公共组件单元测试（E-1 覆盖率补测：exception 18.8% / common.result 52.8% → ≥80%）。
 * <p>覆盖 CE-01~CE-08：BusinessException 构造 / Result / PageResult / GlobalExceptionHandler 各分支。</p>
 */
class CommonComponentTest {

    // ==================== BusinessException ====================

    @Test
    void businessException_errorCode_constructor() {
        // CE-01：ErrorCode 构造 → code/message 正确
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR);
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals(ErrorCode.PARAM_ERROR.getMessage(), ex.getMessage());
    }

    @Test
    void businessException_errorCodeWithMessage_constructor() {
        // CE-02：ErrorCode + 自定义消息
        BusinessException ex = new BusinessException(ErrorCode.PARAM_ERROR, "自定义");
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        assertEquals("自定义", ex.getMessage());
    }

    @Test
    void businessException_rawCode_constructor() {
        // CE-03：原始 code + message
        BusinessException ex = new BusinessException(99999, "原始");
        assertEquals(99999, ex.getCode());
        assertEquals("原始", ex.getMessage());
    }

    // ==================== Result / PageResult ====================

    @Test
    void result_success_variants() {
        // CE-04：success 系列
        Result<String> empty = Result.success();
        assertEquals(0, empty.getCode());
        assertNull(empty.getData());
        Result<String> withData = Result.success("abc");
        assertEquals(0, withData.getCode());
        assertEquals("abc", withData.getData());
        assertEquals("success", withData.getMessage());
    }

    @Test
    void result_failure_variants() {
        // CE-05：error 系列
        Result<Void> fail = Result.error(10001, "失败");
        assertEquals(10001, fail.getCode());
        assertEquals("失败", fail.getMessage());
        assertNull(fail.getData());
    }

    @Test
    void pageResult_constructor() {
        // CE-06：分页结构
        PageResult<String> pr = new PageResult<>(100L, 1, 10, 10L, List.of("a"));
        assertEquals(100L, pr.getTotal());
        assertEquals(1, pr.getPageNum());
        assertEquals(10, pr.getPageSize());
        assertEquals(10L, pr.getPages());
        assertEquals(1, pr.getRecords().size());
    }

    // ==================== GlobalExceptionHandler ====================

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handler_businessException() {
        // CE-07：业务异常 → 对应错误码
        Result<Void> r = handler.handleBusinessException(new BusinessException(ErrorCode.PARAM_ERROR, "业务失败"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), r.getCode());
        assertEquals("业务失败", r.getMessage());
    }

    @Test
    void handler_validationException() throws Exception {
        // CE-08：参数校验异常 → 收集字段错误（m2 修复：多条拼接）
        org.springframework.validation.BeanPropertyBindingResult br =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new FieldError("obj", "name", "名称不能为空"));
        br.addError(new FieldError("obj", "age", "年龄不能为负"));
        java.lang.reflect.Method m = TestTarget.class.getMethod("m", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new org.springframework.core.MethodParameter(m, 0), br);

        Result<Void> r = handler.handleValidationException(ex);

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), r.getCode());
        assertTrue(r.getMessage().contains("name") && r.getMessage().contains("age"));
    }

    @Test
    void handler_bindException() {
        // CE-09：BindException（Query 参数绑定错误）
        org.springframework.validation.BeanPropertyBindingResult br =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj");
        br.addError(new FieldError("obj", "sort", "排序字段非法"));
        BindException ex = new BindException(br);

        Result<Void> r = handler.handleBindException(ex);

        assertFalse(r.getCode() == 0);
        assertTrue(r.getMessage().contains("sort"));
    }

    /** MethodArgumentNotValidException 构造所需的方法载体。 */
    public static class TestTarget {
        public void m(String arg) {}
    }

    @Test
    void handler_unreadableBody() {
        // CE-10：请求体不可读
        Result<Void> r = handler.handleHttpMessageNotReadable(new HttpMessageNotReadableException("bad json"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), r.getCode());
        assertFalse(r.getMessage().isBlank());
    }

    @Test
    void handler_missingParameter() throws Exception {
        // CE-11：缺少参数
        Result<Void> r = handler.handleMissingParameter(new MissingServletRequestParameterException("id", "Long"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), r.getCode());
        assertTrue(r.getMessage().contains("id"));
    }

    @Test
    void handler_methodNotSupported() {
        // CE-12：方法不支持
        Result<Void> r = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("POST", List.of("GET")));
        assertFalse(r.getCode() == 0);
    }

    @Test
    void handler_runtimeException() {
        // CE-13：兜底运行时异常 → SYSTEM_BUSY
        Result<Void> r = handler.handleRuntimeException(new RuntimeException("boom"));
        assertEquals(ErrorCode.SYSTEM_BUSY.getCode(), r.getCode());
    }
}
