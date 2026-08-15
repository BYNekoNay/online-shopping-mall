package com.pzhu.mall.modules.product.service;

import com.pzhu.mall.modules.product.entity.Category;
import com.pzhu.mall.modules.product.mapper.CategoryMapper;
import com.pzhu.mall.modules.product.vo.CategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CategoryService 单元测试（分类树构建 / 平铺列表）。
 * <p>覆盖 docs/32 批次1 的 P-T11~14 用例：树结构、空分类、多级挂载。</p>
 */
class CategoryServiceTest {

    private CategoryMapper categoryMapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(CategoryMapper.class);
        service = new CategoryService();
        inject(service, "categoryMapper", categoryMapper);
    }

    @Test
    void listTree_buildsHierarchy() {
        // 一级：1(数码) 2(服饰)；二级：11(手机,parent=1) 12(电脑,parent=1)
        Category c1 = category(1L, 0L, "数码", 1);
        Category c2 = category(2L, 0L, "服饰", 2);
        Category c11 = category(11L, 1L, "手机", 3);
        Category c12 = category(12L, 1L, "电脑", 4);
        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2, c11, c12));

        List<CategoryVO> tree = service.listTree();

        assertEquals(2, tree.size());
        // 数码下挂 2 个子类
        CategoryVO digital = tree.stream().filter(v -> v.getId() == 1L).findFirst().orElseThrow();
        assertEquals(2, digital.getChildren().size());
        assertEquals("手机", digital.getChildren().get(0).getName());
        // 服饰无子类
        CategoryVO cloth = tree.stream().filter(v -> v.getId() == 2L).findFirst().orElseThrow();
        assertNull(cloth.getChildren());
    }

    @Test
    void listTree_empty_returnsEmpty() {
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertTrue(service.listTree().isEmpty());
    }

    @Test
    void listTree_singleLevel_noChildren() {
        Category c1 = category(1L, 0L, "数码", 1);
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(c1));

        List<CategoryVO> tree = service.listTree();

        assertEquals(1, tree.size());
        assertNull(tree.get(0).getChildren());
    }

    @Test
    void listAll_returnsFlatSorted() {
        Category c1 = category(2L, 0L, "服饰", 2);
        Category c2 = category(1L, 0L, "数码", 1);
        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2));

        List<Category> all = service.listAll();

        assertEquals(2, all.size());
        verify(categoryMapper).selectList(any());
    }

    private static Category category(Long id, Long parentId, String name, Integer sort) {
        Category c = new Category();
        c.setId(id);
        c.setParentId(parentId);
        c.setName(name);
        c.setSort(sort);
        c.setIsDeleted(0);
        return c;
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
