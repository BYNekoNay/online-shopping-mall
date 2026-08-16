<template>
  <div class="app-table">
    <el-table :data="data" style="width: 100%; margin-top: 15px">
      <el-table-column
        v-for="col in columns"
        :key="col.prop || col.label"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :fixed="col.fixed"
      >
        <template v-if="col.slot" #default="{ row }">
          <slot :name="col.slot" :row="row" />
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="pagination"
      :current-page="pagination.currentPage"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      :layout="pagination.layout || 'total, prev, pager, next'"
      style="margin-top: 20px; justify-content: flex-end"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup>
/**
 * 通用表格 + 分页（F-09）。
 *
 * 用 columns 配置驱动渲染 el-table，统一分页交互；业务列用具名插槽自定义渲染，
 * 不做任何业务假设（纯结构收敛）。
 *
 * @props columns 列配置数组：[{ prop, label, width, fixed, slot }]，带 slot 的列渲染同名具名插槽
 * @props data 表格数据数组
 * @props pagination 分页配置 { currentPage, pageSize, total, layout? }；不传则不渲染分页
 * @slots 具名插槽（名称 = columns[].slot），scope 为 { row }
 * @emits page-change(page) 页码变化
 */
defineProps({
  columns: { type: Array, default: () => [] },
  data: { type: Array, default: () => [] },
  pagination: { type: Object, default: null }
})

const emit = defineEmits(['page-change'])

function onPageChange(page) {
  emit('page-change', page)
}
</script>
