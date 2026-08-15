package com.pzhu.mall.modules.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.logistics.entity.LogisticsCompany;
import com.pzhu.mall.modules.logistics.mapper.LogisticsCompanyMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 物流公司字典服务（C-4 物流公司信息维护）。
 */
@Service
public class LogisticsCompanyService {

    @Resource
    private LogisticsCompanyMapper logisticsCompanyMapper;

    /** 启用列表（商家发货下拉，按 sort 排序）。 */
    public List<LogisticsCompany> listEnabled() {
        return logisticsCompanyMapper.selectList(
                new LambdaQueryWrapper<LogisticsCompany>()
                        .eq(LogisticsCompany::getStatus, 1)
                        .orderByAsc(LogisticsCompany::getSort)
                        .orderByAsc(LogisticsCompany::getId));
    }

    /** 管理端全量列表。 */
    public List<LogisticsCompany> adminList() {
        return logisticsCompanyMapper.selectList(
                new LambdaQueryWrapper<LogisticsCompany>()
                        .orderByAsc(LogisticsCompany::getSort)
                        .orderByAsc(LogisticsCompany::getId));
    }

    public void create(LogisticsCompany company) {
        validate(company);
        if (company.getSort() == null) {
            company.setSort(0);
        }
        if (company.getStatus() == null) {
            company.setStatus(1);
        }
        logisticsCompanyMapper.insert(company);
    }

    public void update(LogisticsCompany company) {
        if (company.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司ID不能为空");
        }
        validate(company);
        logisticsCompanyMapper.updateById(company);
    }

    public void delete(Long id) {
        logisticsCompanyMapper.deleteById(id);
    }

    private void validate(LogisticsCompany c) {
        if (c.getName() == null || c.getName().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司名称不能为空");
        }
        if (c.getName().length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司名称最长50字符");
        }
        if (c.getCode() == null || c.getCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司编码不能为空");
        }
        if (c.getCode().length() > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "公司编码最长20字符");
        }
    }
}
