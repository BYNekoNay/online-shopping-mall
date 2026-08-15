package com.pzhu.mall.modules.product.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.result.Result;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.service.ProductService;
import com.pzhu.mall.modules.product.vo.ProductVO;
import com.pzhu.mall.modules.shop.service.ShopService;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * MerchantProductController 单元测试（商家商品发布/列表/批量操作）。
 */
class MerchantProductControllerTest {

    private ProductService productService;
    private ShopService shopService;
    private StringRedisTemplate stringRedisTemplate;
    private MerchantProductController controller;

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
        productService = mock(ProductService.class);
        shopService = mock(ShopService.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        controller = new MerchantProductController();
        inject(controller, "productService", productService);
        inject(controller, "shopService", shopService);
        inject(controller, "stringRedisTemplate", stringRedisTemplate);
        LoginUserContext.set(200L, 2);
        when(shopService.getMerchantShopIdOrThrow(200L)).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void create_withSkus_setsPendingStatus() {
        MerchantProductController.CreateProductDTO dto = new MerchantProductController.CreateProductDTO();
        dto.setCategoryId(1L);
        dto.setName("新商品");
        dto.setPrice(new BigDecimal("99"));
        dto.setStock(10);
        MerchantProductController.SkuDTO skuDto = new MerchantProductController.SkuDTO();
        skuDto.setSpecJson("{\"颜色\":\"黑\"}");
        skuDto.setPrice(new BigDecimal("99"));
        skuDto.setStock(10);
        dto.setSkus(Collections.singletonList(skuDto));

        Product created = new Product();
        created.setId(1L);
        when(productService.createWithSkus(any(), any())).thenReturn(created);
        ProductVO vo = new ProductVO();
        vo.setId(1L);
        when(productService.toVO(created)).thenReturn(vo);

        Result<ProductVO> result = controller.create(dto);

        assertEquals(1L, result.getData().getId());
        // 商品归属商家店铺 + 默认待审核
        verify(productService).createWithSkus(argThat(p -> p.getShopId() == 1L && p.getStatus() == 2), any());
    }

    @Test
    void create_withoutSkus_createsProductOnly() {
        MerchantProductController.CreateProductDTO dto = new MerchantProductController.CreateProductDTO();
        dto.setCategoryId(1L);
        dto.setName("无SKU商品");
        dto.setPrice(new BigDecimal("50"));
        dto.setStock(5);
        dto.setSkus(null);

        Product created = new Product();
        created.setId(2L);
        when(productService.createWithSkus(any(), any())).thenReturn(created);
        when(productService.toVO(any())).thenReturn(new ProductVO());

        assertDoesNotThrow(() -> controller.create(dto));
        verify(productService).createWithSkus(any(), isNull());
    }

    @Test
    void list_delegatesToService() {
        com.pzhu.mall.modules.product.dto.ProductQueryDTO query =
                new com.pzhu.mall.modules.product.dto.ProductQueryDTO();
        when(productService.listPage(any())).thenReturn(
                new com.pzhu.mall.common.result.PageResult<>(0L, 1L, 10L, 0L, Collections.emptyList()));

        var result = controller.list(query);

        assertNotNull(result.getData());
        verify(productService).listPage(any());
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
