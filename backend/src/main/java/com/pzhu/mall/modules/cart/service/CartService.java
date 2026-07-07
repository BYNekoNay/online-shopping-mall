package com.pzhu.mall.modules.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.cart.entity.Cart;
import com.pzhu.mall.modules.cart.mapper.CartMapper;
import com.pzhu.mall.modules.cart.vo.CartVO;
import com.pzhu.mall.modules.product.entity.Product;
import com.pzhu.mall.modules.product.entity.Sku;
import com.pzhu.mall.modules.product.mapper.ProductMapper;
import com.pzhu.mall.modules.product.mapper.SkuMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 获取当前用户的购物车列表（含商品信息）。
     */
    public List<CartVO> listVO() {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId).orderByDesc(Cart::getCreateTime);
        List<Cart> items = cartMapper.selectList(qw);

        List<CartVO> voList = new ArrayList<>();
        for (Cart item : items) {
            CartVO vo = new CartVO();
            vo.setId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setSkuId(item.getSkuId());
            vo.setQuantity(item.getQuantity());
            vo.setSelected(item.getSelected());

            Product product = productMapper.selectById(item.getProductId());
            if (product == null) continue;
            vo.setProductName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setPrice(product.getPrice());
            vo.setStock(product.getStock());

            if (item.getSkuId() != null) {
                Sku sku = skuMapper.selectById(item.getSkuId());
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
     * 添加到购物车。
     */
    public void add(Cart cart) {
        Long userId = com.pzhu.mall.security.LoginUserContext.getCurrentUserId();
        cart.setUserId(userId);
        // 检查是否已存在（同一用户+同一商品+同一SKU）
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId)
          .eq(Cart::getProductId, cart.getProductId())
          .eq(Cart::getSkuId, cart.getSkuId());
        Cart exist = cartMapper.selectOne(qw);
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + (cart.getQuantity() != null ? cart.getQuantity() : 1));
            cartMapper.updateById(exist);
        } else {
            cartMapper.insert(cart);
        }
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
        data.setId(id);
        cartMapper.updateById(data);
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
