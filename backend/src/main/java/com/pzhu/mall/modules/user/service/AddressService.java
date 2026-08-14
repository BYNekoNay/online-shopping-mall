package com.pzhu.mall.modules.user.service;

import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    /**
     * 查询当前用户的所有地址。
     */
    public List<Address> listByUser(Long userId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Address>();
        qw.eq(Address::getUserId, userId)
          .orderByDesc(Address::getIsDefault)
          .orderByDesc(Address::getCreateTime);
        return addressMapper.selectList(qw);
    }

    /**
     * 新增地址。
     * <p>U-06 修复：用户无任何默认地址时，新地址自动设为默认（避免"无默认地址"状态）。</p>
     */
    @Transactional
    public Long add(Long userId, Address address) {
        address.setUserId(userId);
        boolean isDefaultRequested = address.getIsDefault() != null && address.getIsDefault() == 1;
        if (isDefaultRequested) {
            // 如果设为默认，先清除该用户的其他默认地址
            var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Address>();
            uw.set(Address::getIsDefault, 0).eq(Address::getUserId, userId);
            addressMapper.update(null, uw);
        } else {
            // U-06 修复：用户还没有任何地址/默认地址时，自动设为默认
            Long count = addressMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Address>()
                            .eq(Address::getUserId, userId)
                            .eq(Address::getIsDefault, 1)
            );
            if (count == null || count == 0) {
                address.setIsDefault(1);
            }
        }
        addressMapper.insert(address);
        return address.getId();
    }

    /**
     * 更新地址。
     */
    @Transactional
    public void update(Long userId, Long addressId, Address data) {
        Address exist = addressMapper.selectById(addressId);
        if (exist == null || !Objects.equals(exist.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (data.getIsDefault() != null && data.getIsDefault() == 1) {
            var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Address>();
            uw.set(Address::getIsDefault, 0).eq(Address::getUserId, userId);
            addressMapper.update(null, uw);
        }
        data.setId(addressId);
        data.setUserId(userId);
        addressMapper.updateById(data);
    }

    /**
     * 删除地址。
     * <p>U-05 修复：删除默认地址后，自动将最近创建的地址补为默认，避免"无默认地址"状态。</p>
     */
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address exist = addressMapper.selectById(addressId);
        if (exist == null || !Objects.equals(exist.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        boolean wasDefault = exist.getIsDefault() != null && exist.getIsDefault() == 1;
        addressMapper.deleteById(addressId);
        // U-05 修复：删除默认地址后补位
        if (wasDefault) {
            List<Address> rest = addressMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Address>()
                            .eq(Address::getUserId, userId)
                            .orderByDesc(Address::getCreateTime)
            );
            if (!rest.isEmpty()) {
                Address first = rest.get(0);
                first.setIsDefault(1);
                addressMapper.updateById(first);
            }
        }
    }
}
