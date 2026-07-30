package com.pzhu.mall.modules.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.enums.ProductStatus;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.cart.vo.CartVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务。
 */
@Service
public class CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SkuMapper skuMapper;

    /** M-02：单条购物车项的最大购买数量上限 */
    private static final int MAX_CART_QUANTITY = 99;

    /**
     * 获取当前用户的购物车列表（含商品信息）。
     */
    public List<CartVO> listVO() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId).orderByDesc(Cart::getCreateTime);
        List<Cart> items = cartMapper.selectList(qw);

        if (items.isEmpty()) {
            return java.util.Collections.emptyList();
        }

            // 批量加载所有商品和 SKU（消除 N+1 查询）
            Set<Long> productIds = items.stream()
                    .map(Cart::getProductId)
                    .collect(Collectors.toSet());
            List<Product> products = productMapper.selectBatchIds(productIds);
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            Set<Long> skuIds = items.stream()
                    .map(Cart::getSkuId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, Sku> skuMap = new HashMap<>();
            if (!skuIds.isEmpty()) {
                List<Sku> skuList = skuMapper.selectBatchIds(skuIds);
                skuMap = skuList.stream()
                        .collect(Collectors.toMap(Sku::getId, s -> s));
            }

        List<CartVO> voList = new ArrayList<>();
        for (Cart item : items) {
            CartVO vo = new CartVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setSkuId(item.getSkuId());
            vo.setQuantity(item.getQuantity());
            vo.setSelected(item.getSelected());

            Product product = productMap.get(item.getProductId());
            if (product == null) continue;
            vo.setProductName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());

            if (item.getSkuId() != null) {
                Sku sku = skuMap.get(item.getSkuId());
                if (sku != null) {
                    vo.setSpecText(sku.getSpecJson());
                    vo.setPrice(sku.getPrice());
                    vo.setStock(sku.getStock());
                    vo.setStockEnough(sku.getStock() >= item.getQuantity());
                }
            }
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 添加到购物车（已存在时原子累加数量）。
     */
    public void add(Cart cart) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        cart.setUserId(userId);
        cart.setId(null); // 防止请求体携带主键造成误更新
        // 校验数量必须为正数
        if (cart.getQuantity() == null || cart.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品数量必须大于0");
        }

        // M-02 修复：加购时校验商品状态、规格与库存
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null || (product.getIsDeleted() != null && product.getIsDeleted() == 1)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (ProductStatus.of(product.getStatus()) != ProductStatus.ONLINE) {
            throw new BusinessException(ErrorCode.PRODUCT_OFFLINE_ORDER);
        }
        int stock;
        if (cart.getSkuId() != null) {
            Sku sku = skuMapper.selectById(cart.getSkuId());
            if (sku == null) {
                throw new BusinessException(ErrorCode.SKU_NOT_FOUND);
            }
            // C-1 修复：校验 SKU 与商品的绑定关系，防止用其他商品的低价 SKU 加购本商品（价格篡改）
            if (!cart.getProductId().equals(sku.getProductId())) {
                throw new BusinessException(ErrorCode.SKU_PRODUCT_MISMATCH);
            }
            stock = sku.getStock() != null ? sku.getStock() : 0;
        } else {
            stock = product.getStock() != null ? product.getStock() : 0;
        }
        if (cart.getQuantity() > MAX_CART_QUANTITY) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "单件商品加购数量不能超过 " + MAX_CART_QUANTITY);
        }
        if (cart.getQuantity() > stock) {
            throw new BusinessException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        // M-05 修复：原子 upsert，替代"先查后写"的 check-then-act，防止并发加购产生重复行。
        // 先尝试对已有行原子累加；无匹配行则 INSERT；若并发下唯一键冲突，回退为再次累加。
        int updated = incrementQuantity(userId, cart.getProductId(), cart.getSkuId(), cart.getQuantity());
        if (updated == 0) {
            cart.setSelected(1);
            cart.setCreateTime(LocalDateTime.now());
            try {
                cartMapper.insert(cart);
            } catch (DuplicateKeyException e) {
                incrementQuantity(userId, cart.getProductId(), cart.getSkuId(), cart.getQuantity());
            }
        }
    }

    /**
     * 对指定（用户+商品+SKU）的购物车行原子累加数量。
     * <p>skuId 可为空，为空时使用 IS NULL 匹配，避免生成 {@code sku_id = NULL} 恒假条件。</p>
     */
    private int incrementQuantity(Long userId, Long productId, Long skuId, int quantity) {
        LambdaUpdateWrapper<Cart> uw = new LambdaUpdateWrapper<Cart>()
                .setSql("quantity = quantity + " + quantity)
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId);
        if (skuId == null) {
            uw.isNull(Cart::getSkuId);
        } else {
            uw.eq(Cart::getSkuId, skuId);
        }
        return cartMapper.update(null, uw);
    }

    /**
     * 更新购物车项。
     */
    public void update(Long id, Cart data) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Cart exist = cartMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // 校验数量必须为正数
        if (data.getQuantity() != null && data.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品数量必须大于0");
        }
        // M-03 修复：仅允许更新 quantity/selected，禁止覆写 userId/productId/skuId（Mass Assignment）。
        // updateById 忽略 null 字段，因此新建实体只会写入这两个可变字段。
        Cart update = new Cart();
        update.setId(id);
        update.setQuantity(data.getQuantity());
        update.setSelected(data.getSelected());
        cartMapper.updateById(update);
    }

    /**
     * 删除购物车项。
     */
    public void delete(Long id) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        Cart exist = cartMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        cartMapper.deleteById(id);
    }
}
