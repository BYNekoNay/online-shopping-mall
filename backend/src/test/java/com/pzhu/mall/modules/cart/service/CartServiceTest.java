package com.pzhu.mall.modules.cart.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.cart.vo.CartVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 6.2 CartService 单元测试。
 * <p>覆盖加购校验（M-02：状态/SKU/库存/数量上限）、原子 upsert（M-05：累加优先 +
 * 唯一键冲突回退）、更新字段白名单（M-03 防 Mass Assignment）、归属校验。</p>
 */
class CartServiceTest {

    private CartMapper cartMapper;
    private ProductMapper productMapper;
    private SkuMapper skuMapper;
    private CartService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Cart.class);
    }

    @BeforeEach
    void setUp() {
        cartMapper = mock(CartMapper.class);
        productMapper = mock(ProductMapper.class);
        skuMapper = mock(SkuMapper.class);
        service = new CartService();
        inject(service, "cartMapper", cartMapper);
        inject(service, "productMapper", productMapper);
        inject(service, "skuMapper", skuMapper);
        LoginUserContext.set(100L, 1);
    }

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    // ==================== add ====================

    @Test
    void add_newItem_insertsWithSelectedAndServerUserId() {
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        when(cartMapper.update(isNull(), any())).thenReturn(0);
        when(cartMapper.insert(any(Cart.class))).thenReturn(1);

        Cart req = new Cart();
        req.setId(999L);          // 请求体携带主键，必须被清空
        req.setUserId(888L);      // 请求体携带 userId，必须被覆盖为登录用户
        req.setProductId(10L);
        req.setQuantity(2);

        service.add(req);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).insert(captor.capture());
        Cart saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(100L, saved.getUserId());
        assertEquals(1, saved.getSelected());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    void add_existingRow_atomicallyIncrements() {
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        when(cartMapper.update(isNull(), any())).thenReturn(1);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(2);
        service.add(req);

        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void add_duplicateKeyConflict_fallsBackToIncrement() {
        // M-05 修复验证：并发加购时 INSERT 撞唯一键 → 回退为原子累加
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        when(cartMapper.update(isNull(), any())).thenReturn(0);
        when(cartMapper.insert(any(Cart.class))).thenThrow(new DuplicateKeyException("uk_user_product_sku"));

        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(2);

        assertDoesNotThrow(() -> service.add(req));
        verify(cartMapper, times(2)).update(isNull(), any());
    }

    @Test
    void add_skuPresent_usesSkuStock() {
        Product product = onlineProduct(10L, 50);
        when(productMapper.selectById(10L)).thenReturn(product);
        Sku sku = new Sku();
        sku.setId(5L);
        sku.setProductId(10L); // C-1 后绑定校验要求 SKU 归属一致
        sku.setStock(3);
        when(skuMapper.selectById(5L)).thenReturn(sku);
        when(cartMapper.update(isNull(), any())).thenReturn(1);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setSkuId(5L);
        req.setQuantity(4); // 超过 SKU 库存 3（即使商品库存 50 足够）

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void add_quantityZero_throws() {
        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void add_productOffline_throws() {
        Product product = onlineProduct(10L, 50);
        product.setStatus(0); // 下架
        when(productMapper.selectById(10L)).thenReturn(product);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.PRODUCT_OFFLINE_ORDER.getCode(), ex.getCode());
    }

    @Test
    void add_productDeleted_throws() {
        Product product = onlineProduct(10L, 50);
        product.setIsDeleted(1);
        when(productMapper.selectById(10L)).thenReturn(product);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.PRODUCT_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void add_skuNotFound_throws() {
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        when(skuMapper.selectById(5L)).thenReturn(null);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setSkuId(5L);
        req.setQuantity(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.SKU_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void add_skuBelongsToOtherProduct_throwsMismatch() {
        // C-1 修复验证：攻击载荷"商品10 + 商品777的低价SKU"必须被拒绝，且不写库
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        Sku foreignSku = new Sku();
        foreignSku.setId(5L);
        foreignSku.setProductId(777L); // 归属其他商品
        foreignSku.setStock(100);
        foreignSku.setPrice(new BigDecimal("0.01"));
        when(skuMapper.selectById(5L)).thenReturn(foreignSku);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setSkuId(5L);
        req.setQuantity(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.SKU_PRODUCT_MISMATCH.getCode(), ex.getCode());
        verify(cartMapper, never()).insert(any(Cart.class));
        verify(cartMapper, never()).update(isNull(), any());
    }

    @Test
    void add_skuMatchingProduct_passes() {
        // C-1 修复验证：归属一致的 SKU 正常加购（校验不误伤）
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 50));
        Sku sku = new Sku();
        sku.setId(5L);
        sku.setProductId(10L);
        sku.setStock(10);
        when(skuMapper.selectById(5L)).thenReturn(sku);
        when(cartMapper.update(isNull(), any())).thenReturn(1);

        Cart req = new Cart();
        req.setProductId(10L);
        req.setSkuId(5L);
        req.setQuantity(2);

        assertDoesNotThrow(() -> service.add(req));
        verify(cartMapper).update(isNull(), any());
    }

    @Test
    void add_exceedsMaxQuantity_throws() {
        // M-02 修复验证：单条购物车项上限 99
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 500));

        Cart req = new Cart();
        req.setProductId(10L);
        req.setQuantity(100);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ==================== update ====================

    @Test
    void update_onlyWritesWhitelistedFields() {
        // M-03 修复验证：updateById 的实体只含 id/quantity/selected，
        // userId/productId/skuId 一律为 null（MP 忽略 null 字段）
        // CR-02 修复：update 会校验商品 ONLINE 与库存，需 mock 商品查询
        Cart exist = new Cart();
        exist.setId(1L);
        exist.setUserId(100L);
        exist.setProductId(10L);
        when(cartMapper.selectById(1L)).thenReturn(exist);
        when(productMapper.selectById(10L)).thenReturn(onlineProduct(10L, 100));

        Cart data = new Cart();
        data.setQuantity(5);
        data.setSelected(0);
        data.setUserId(888L);      // 攻击载荷：必须被忽略
        data.setProductId(777L);   // 攻击载荷：必须被忽略

        service.update(1L, data);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).updateById(captor.capture());
        Cart written = captor.getValue();
        assertEquals(1L, written.getId());
        assertEquals(5, written.getQuantity());
        assertEquals(0, written.getSelected());
        assertNull(written.getUserId());
        assertNull(written.getProductId());
        assertNull(written.getSkuId());
    }

    @Test
    void update_notOwner_throws() {
        Cart exist = new Cart();
        exist.setId(1L);
        exist.setUserId(999L);
        when(cartMapper.selectById(1L)).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(1L, new Cart()));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(cartMapper, never()).updateById(any(Cart.class));
    }

    @Test
    void update_quantityZero_throws() {
        Cart exist = new Cart();
        exist.setId(1L);
        exist.setUserId(100L);
        when(cartMapper.selectById(1L)).thenReturn(exist);

        Cart data = new Cart();
        data.setQuantity(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, data));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ==================== delete ====================

    @Test
    void delete_success() {
        Cart exist = new Cart();
        exist.setId(1L);
        exist.setUserId(100L);
        when(cartMapper.selectById(1L)).thenReturn(exist);

        service.delete(1L);
        verify(cartMapper).deleteById(1L);
    }

    @Test
    void delete_notOwner_throws() {
        Cart exist = new Cart();
        exist.setId(1L);
        exist.setUserId(999L);
        when(cartMapper.selectById(1L)).thenReturn(exist);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        verify(cartMapper, never()).deleteById(anyLong());
    }

    // ==================== listVO ====================

    @Test
    void listVO_mergesProductAndSkuInfo() {
        Cart c1 = new Cart();
        c1.setId(1L);
        c1.setProductId(10L);
        c1.setSkuId(5L);
        c1.setQuantity(2);
        c1.setSelected(1);
        Cart c2 = new Cart();
        c2.setId(2L);
        c2.setProductId(11L);
        c2.setSkuId(null);
        c2.setQuantity(1);
        c2.setSelected(0);
        Cart c3 = new Cart();
        c3.setId(3L);
        c3.setProductId(99L); // 商品已不存在 → 跳过
        c3.setQuantity(1);
        when(cartMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2, c3));

        Product p1 = onlineProduct(10L, 50);
        p1.setName("Laptop");
        p1.setPrice(new BigDecimal("5999.00"));
        Product p2 = onlineProduct(11L, 8);
        p2.setName("Shirt");
        p2.setPrice(new BigDecimal("99.00"));
        when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(p1, p2));

        Sku sku = new Sku();
        sku.setId(5L);
        sku.setStock(10);
        sku.setPrice(new BigDecimal("6999.00"));
        sku.setSpecJson("{\"color\":\"Black\"}");
        when(skuMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(sku));

        List<CartVO> vos = service.listVO();

        assertEquals(2, vos.size());
        CartVO v1 = vos.get(0);
        assertEquals(new BigDecimal("6999.00"), v1.getPrice()); // SKU 价覆盖商品价
        assertEquals(10, v1.getStock());
        assertTrue(v1.getStockEnough()); // SKU 库存 10 >= 数量 2
        CartVO v2 = vos.get(1);
        assertEquals(new BigDecimal("99.00"), v2.getPrice());
        assertEquals(8, v2.getStock()); // 无 SKU 时展示商品库存
        // CR-04 修复：无 SKU 行同样填充 stockEnough（库存 8 >= 数量 2）
        assertTrue(v2.getStockEnough());
    }

    // ==================== helpers ====================

    private static Product onlineProduct(Long id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setStatus(1);
        p.setStock(stock);
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
