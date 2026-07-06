package com.pzhu.mall.modules.user.service;

import com.pzhu.mall.modules.user.entity.Address;
import com.pzhu.mall.modules.user.mapper.AddressMapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.security.LoginUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
     */
    @Transactional
    public Long add(Long userId, Address address) {
        address.setUserId(userId);
        // 如果设为默认，先清除该用户的其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Address>();
            uw.set(Address::getIsDefault, 0).eq(Address::getUserId, userId);
            addressMapper.update(null, uw);
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
        if (exist == null || !exist.getUserId().equals(userId)) {
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
     */
    @Transactional
    public void delete(Long userId, Long addressId) {
        Address exist = addressMapper.selectById(addressId);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        addressMapper.deleteById(addressId);
    }
}
