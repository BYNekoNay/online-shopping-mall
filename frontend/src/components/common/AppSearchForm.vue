<template>
  <el-form inline class="app-search-form" @submit.prevent>
    <el-form-item v-for="field in fields" :key="field.key" :label="field.label">
      <el-select
        v-if="field.type === 'select'"
        v-model="model[field.key]"
        :placeholder="field.placeholder"
        clearable
        :style="{ width: field.width || '140px' }"
      >
        <el-option v-for="opt in field.options || []" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-date-picker
        v-else-if="field.type === 'date-range'"
        v-model="model[field.key]"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
      <el-input
        v-else
        v-model="model[field.key]"
        :placeholder="field.placeholder"
        clearable
        :style="{ width: field.width || '180px' }"
        @keyup.enter="handleSearch"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { reactive } from 'vue'

/**
 * 通用搜索表单（F-09）。
 *
 * 用 fields 配置驱动渲染 el-form 搜索区，统一「查询 / 重置」交互；
 * 仅收敛重复的表单结构与事件，不限制业务字段类型（input/select/date-range）。
 *
 * @props fields 字段配置数组：
 *   [{ key, label, type: 'input'|'select'|'date-range', options: [{label,value}], placeholder, width, defaultValue }]
 * @emits search(values) 点击查询，上报当前表单值对象
 * @emits reset() 点击重置，恢复字段默认值
 */
const props = defineProps({
  fields: { type: Array, default: () => [] }
})

const emit = defineEmits(['search', 'reset'])

// 本地表单模型：初始值来自 fields[].defaultValue（缺省 input→null、date-range→[]）
const model = reactive({})

function initModel() {
  Object.keys(model).forEach((key) => delete model[key])
  props.fields.forEach((field) => {
    if (field.defaultValue !== undefined) {
      model[field.key] = field.defaultValue
    } else {
      model[field.key] = field.type === 'date-range' ? [] : null
    }
  })
}

initModel()

/** 查询：将当前表单值整体上报父组件 */
function handleSearch() {
  emit('search', { ...model })
}

/** 重置：恢复默认值并通知父组件重新加载 */
function handleReset() {
  initModel()
  emit('reset')
}
</script>

<style scoped>
.app-search-form {
  margin-top: 15px;
}
</style>
