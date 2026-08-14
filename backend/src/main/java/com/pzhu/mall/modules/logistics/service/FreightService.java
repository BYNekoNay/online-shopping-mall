package com.pzhu.mall.modules.logistics.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.logistics.entity.FreightTemplate;
import com.pzhu.mall.modules.logistics.mapper.FreightTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 运费服务。
 */
@Service
public class FreightService {

    private static final Logger log = LoggerFactory.getLogger(FreightService.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @Resource
    private FreightTemplateMapper freightTemplateMapper;

    /**
     * 省份-大区映射表（简化版）。
     */
    private static final Map<String, String> PROVINCE_REGION = Map.ofEntries(
        Map.entry("北京", "华北"), Map.entry("天津", "华北"), Map.entry("河北", "华北"), Map.entry("山西", "华北"), Map.entry("内蒙古", "华北"),
        Map.entry("上海", "华东"), Map.entry("江苏", "华东"), Map.entry("浙江", "华东"), Map.entry("安徽", "华东"), Map.entry("福建", "华东"), Map.entry("江西", "华东"), Map.entry("山东", "华东"),
        Map.entry("广东", "华南"), Map.entry("广西", "华南"), Map.entry("海南", "华南"),
        Map.entry("河南", "华中"), Map.entry("湖北", "华中"), Map.entry("湖南", "华中"),
        Map.entry("重庆", "西南"), Map.entry("四川", "西南"), Map.entry("贵州", "西南"), Map.entry("云南", "西南"), Map.entry("西藏", "西南"),
        Map.entry("陕西", "西北"), Map.entry("甘肃", "西北"), Map.entry("青海", "西北"), Map.entry("宁夏", "西北"), Map.entry("新疆", "西北"),
        Map.entry("辽宁", "东北"), Map.entry("吉林", "东北"), Map.entry("黑龙江", "东北"),
        // LG-03 修复：补港澳台地区（此前缺失，按大区配置时覆盖不全）
        Map.entry("香港", "华南"), Map.entry("澳门", "华南"), Map.entry("台湾", "华东")
    );

    /**
     * 计算运费。
     * <p>LG-01 修复：多模板时按 id 升序取最先创建的模板（原 LIMIT 1 无排序结果不确定）。</p>
     */
    public BigDecimal calculate(Long shopId, String province, BigDecimal goodsAmount) {
        List<FreightTemplate> templates = freightTemplateMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FreightTemplate>()
                .eq(FreightTemplate::getShopId, shopId)
                .eq(FreightTemplate::getIsDeleted, 0)
                .orderByAsc(FreightTemplate::getId)
                .last("LIMIT 1")
        );

        if (templates.isEmpty()) {
            return BigDecimal.ZERO; // 未配置模板默认免运费
        }

        FreightTemplate template = templates.get(0);

        // 满额免邮
        if (template.getFreeShippingThreshold() != null
                && goodsAmount != null
                && goodsAmount.compareTo(template.getFreeShippingThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }

        String region = PROVINCE_REGION.getOrDefault(province, "default");

        // 解析 region_rule_json
        if (template.getRegionRuleJson() != null && !template.getRegionRuleJson().isEmpty()) {
            try {
                List<Map<String, Object>> rules = MAPPER.readValue(template.getRegionRuleJson(), List.class);
                for (Map<String, Object> rule : rules) {
                    Object regionObj = rule.get("region");
                    if (regionObj != null && Objects.equals(region, regionObj.toString())) {
                        Object feeObj = rule.getOrDefault("fee", 0);
                        return new BigDecimal(feeObj.toString());
                    }
                }
            } catch (Exception e) {
                log.warn("[运费] 解析 region_rule_json 失败 shopId={} rule={}", shopId, template.getRegionRuleJson(), e);
            }
        }
        return template.getDefaultFee() != null ? template.getDefaultFee() : BigDecimal.ZERO;
    }

    /**
     * 获取店铺运费模板列表。
     */
    public List<FreightTemplate> listByShop(Long shopId) {
        return freightTemplateMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FreightTemplate>()
                .eq(FreightTemplate::getShopId, shopId)
                .eq(FreightTemplate::getIsDeleted, 0)
        );
    }

    /**
     * 按 ID 获取运费模板（用于归属校验）。
     */
    public FreightTemplate getById(Long id) {
        return freightTemplateMapper.selectById(id);
    }

    /**
     * 保存运费模板。
     * <p>LG-02 修复：金额非负校验 + region_rule_json 合法性校验。</p>
     */
    public void save(FreightTemplate template) {
        if (template.getDefaultFee() != null && template.getDefaultFee().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(com.pzhu.mall.common.enums.ErrorCode.PARAM_ERROR, "默认运费不能为负数");
        }
        if (template.getFreeShippingThreshold() != null
                && template.getFreeShippingThreshold().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(com.pzhu.mall.common.enums.ErrorCode.PARAM_ERROR, "免邮门槛不能为负数");
        }
        if (template.getRegionRuleJson() != null && !template.getRegionRuleJson().isBlank()) {
            try {
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(template.getRegionRuleJson());
            } catch (Exception e) {
                throw new BusinessException(com.pzhu.mall.common.enums.ErrorCode.PARAM_ERROR, "运费区域规则 JSON 格式不正确");
            }
        }
        if (template.getId() == null) {
            freightTemplateMapper.insert(template);
        } else {
            freightTemplateMapper.updateById(template);
        }
    }
}
