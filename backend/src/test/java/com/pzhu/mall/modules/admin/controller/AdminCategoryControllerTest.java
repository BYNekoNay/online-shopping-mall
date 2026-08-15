package com.pzhu.mall.modules.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminCategoryController 单元测试（E-1 覆盖率补测：admin.controller 56% → ≥70%）。
 * <p>覆盖 AC-01~AC-05：列表/创建/更新/字段保护。</p>
 */
class AdminCategoryControllerTest {

    private CategoryMapper categoryMapper;
    private AdminCategoryController controller;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Category.class);
    }

    @BeforeEach
    void setUp() {
        categoryMapper = mock(CategoryMapper.class);
        controller = new AdminCategoryController();
        inject(controller, "categoryMapper", categoryMapper);
    }

    @Test
    void list_returnsNonDeleted() {
        // AC-01：列表过滤已删
        Category c = new Category();
        c.setId(1L);
        c.setName("手机");
        when(categoryMapper.selectList(any())).thenReturn(List.of(c));

        var result = controller.list();

        assertEquals(1, result.getData().size());
        verify(categoryMapper).selectList(any());
    }

    @Test
    void create_setsSafeFields() {
        // AC-02：创建仅映射 DTO 字段（Mass Assignment 防护）
        AdminCategoryController.CategoryDTO dto = new AdminCategoryController.CategoryDTO();
        dto.setParentId(0L);
        dto.setName("数码");
        dto.setIcon("icon.png");
        dto.setSort(1);
        dto.setStatus(1);
        Category saved = new Category();
        saved.setId(9L);
        when(categoryMapper.insert(any())).thenReturn(1);
        when(categoryMapper.selectById(9L)).thenReturn(saved);
        // 用 doAnswer 捕获插入对象设置 id
        doAnswer(inv -> {
            ((Category) inv.getArgument(0)).setId(9L);
            return 1;
        }).when(categoryMapper).insert(any());

        var result = controller.create(dto);

        assertEquals(9L, result.getData());
        org.mockito.ArgumentCaptor<Category> captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());
        Category inserted = captor.getValue();
        assertEquals("数码", inserted.getName());
        assertEquals(0, inserted.getIsDeleted());
    }

    @Test
    void update_mergesEditableFields() {
        // AC-03：更新仅覆盖允许字段
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("旧名");
        existing.setIsDeleted(0);
        when(categoryMapper.selectById(1L)).thenReturn(existing);

        AdminCategoryController.CategoryDTO dto = new AdminCategoryController.CategoryDTO();
        dto.setName("新名");
        dto.setSort(5);

        controller.update(1L, dto);

        assertEquals("新名", existing.getName());
        assertEquals(5, existing.getSort());
        verify(categoryMapper).updateById(existing);
    }

    @Test
    void update_notFound_throws() {
        // AC-04：分类不存在 → 10004
        when(categoryMapper.selectById(99L)).thenReturn(null);
        AdminCategoryController.CategoryDTO dto = new AdminCategoryController.CategoryDTO();
        dto.setName("x");

        assertThrows(BusinessException.class, () -> controller.update(99L, dto));
        verify(categoryMapper, never()).updateById(any());
    }

    @Test
    void update_deletedCategory_throws() {
        // AC-05：已删分类 → 10004
        Category deleted = new Category();
        deleted.setId(1L);
        deleted.setIsDeleted(1);
        when(categoryMapper.selectById(1L)).thenReturn(deleted);
        AdminCategoryController.CategoryDTO dto = new AdminCategoryController.CategoryDTO();
        dto.setName("x");

        assertThrows(BusinessException.class, () -> controller.update(1L, dto));
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
