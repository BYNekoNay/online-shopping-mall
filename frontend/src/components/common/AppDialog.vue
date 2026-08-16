<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    @update:model-value="onUpdateModelValue"
  >
    <slot />
    <template #footer>
      <slot name="footer">
        <el-button @click="onUpdateModelValue(false)">取消</el-button>
        <el-button type="primary" :loading="loading" @click="emit('confirm')">{{ confirmText }}</el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 通用弹窗（F-09）。
 *
 * 收敛 el-dialog 的 v-model 开关与「取消 / 确认」footer；业务内容走默认插槽，
 * 需要自定义 footer 时用 #footer 具名插槽覆盖。
 *
 * @props modelValue 是否可见（v-model）
 * @props title 标题
 * @props width 宽度（默认 500px）
 * @props confirmText 确认按钮文案（默认「保存」）
 * @props loading 确认按钮 loading
 * @slots default 弹窗正文
 * @slots footer 自定义 footer（不传则渲染默认 取消/确认）
 * @emits update:modelValue 关闭
 * @emits confirm 点击确认
 */
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '500px' },
  confirmText: { type: String, default: '保存' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

function onUpdateModelValue(value) {
  emit('update:modelValue', value)
}
</script>
