package com.pzhu.mall.modules.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品分类服务。
 */
@Service
public class CategoryService {

    @Resource
    private CategoryMapper categoryMapper;

    /**
     * 获取分类树。
     */
    public List<CategoryVO> listTree() {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<>();
        qw.eq(Category::getIsDeleted, 0).orderByAsc(Category::getSort);
        List<Category> all = categoryMapper.selectList(qw);

        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == 0)
                .collect(Collectors.toList());

        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != 0)
                .collect(Collectors.groupingBy(Category::getParentId));

        List<CategoryVO> result = new ArrayList<>();
        for (Category root : roots) {
            CategoryVO vo = toVO(root);
            List<Category> children = childrenMap.get(root.getId());
            if (children != null) {
                vo.setChildren(children.stream().map(this::toVO).collect(Collectors.toList()));
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 获取全部分类（平铺列表，供管理员后台使用）。
     */
    public List<Category> listAll() {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<>();
        qw.eq(Category::getIsDeleted, 0).orderByAsc(Category::getSort).orderByAsc(Category::getId);
        return categoryMapper.selectList(qw);
    }

    private CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setParentId(c.getParentId());
        vo.setName(c.getName());
        vo.setIcon(c.getIcon());
        vo.setSort(c.getSort());
        vo.setStatus(c.getStatus());
        return vo;
    }
}
