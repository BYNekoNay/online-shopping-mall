package com.pzhu.mall.modules.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.result.PageResult;
import com.pzhu.mall.modules.admin.service.OperationLogService;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AdminProductController 单元测试（商品审核 R3-C1/C2）。
 * <p>覆盖 docs/32 批次3 的 A-T05~08 用例：审核通过/驳回/并发双审拦截。</p>
 */
class AdminProductControllerTest {

    private ProductMapper productMapper;
    private ProductService productService;
    private OperationLogService operationLogService;
    private AdminProductController controller;

    @BeforeAll
    static void initTableInfo() {
        if (!TableInfoHelper.getTableInfos().stream()
                .anyMatch(t -> t.getEntityType() == Product.class)) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""), Product.class);
        }
    }

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        productService = mock(ProductService.class);
        operationLogService = mock(OperationLogService.class);
        controller = new AdminProductController();
        inject(controller, "productMapper", productMapper);
        inject(controller, "productService", productService);
        inject(controller, "operationLogService", operationLogService);
        LoginUserContext.set(1L, 3);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void audit_approve_succeeds() {
        Product p = product(1L, ProductStatus.PENDING.getCode());
        when(productMapper.selectById(1L)).thenReturn(p);
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        AdminProductController.AuditDTO dto = new AdminProductController.AuditDTO();
        dto.setApproved(true);

        controller.audit(1L, dto);

        // 原子更新：status → ONLINE，WHERE status=PENDING
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(productMapper).update(eq(null), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("status"));
    }

    @Test
    void audit_reject_succeeds() {
        Product p = product(1L, ProductStatus.PENDING.getCode());
        when(productMapper.selectById(1L)).thenReturn(p);
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        AdminProductController.AuditDTO dto = new AdminProductController.AuditDTO();
        dto.setApproved(false);

        assertDoesNotThrow(() -> controller.audit(1L, dto));
    }

    @Test
    void audit_notPending_throws() {
        // R3-C1 验证：仅待审核可审核
        Product p = product(1L, ProductStatus.ONLINE.getCode());
        when(productMapper.selectById(1L)).thenReturn(p);

        AdminProductController.AuditDTO dto = new AdminProductController.AuditDTO();
        dto.setApproved(true);

        assertThrows(BusinessException.class, () -> controller.audit(1L, dto));
    }

    @Test
    void audit_concurrentUpdateLost_throws() {
        // AD-02 修复验证：并发双审影响行数为 0 → 明确报错
        Product p = product(1L, ProductStatus.PENDING.getCode());
        when(productMapper.selectById(1L)).thenReturn(p);
        when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        AdminProductController.AuditDTO dto = new AdminProductController.AuditDTO();
        dto.setApproved(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> controller.audit(1L, dto));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void audit_productNotFound_throws() {
        when(productMapper.selectById(1L)).thenReturn(null);

        AdminProductController.AuditDTO dto = new AdminProductController.AuditDTO();
        dto.setApproved(true);

        assertThrows(BusinessException.class, () -> controller.audit(1L, dto));
    }

    private static Product product(Long id, int status) {
        Product p = new Product();
        p.setId(id);
        p.setStatus(status);
        p.setIsDeleted(0);
        return p;
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
