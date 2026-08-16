<template>
  <div ref="chartEl" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
// F-1 统一图表封装：按需引入的 echarts + resize 自动适配 + 组件卸载时销毁
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import echarts from '@/utils/echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '350px' }
})

const chartEl = ref(null)
let chart = null
let resizeObserver = null

function render() {
  if (!chart && chartEl.value) {
    chart = echarts.init(chartEl.value)
  }
  if (chart) {
    chart.setOption(props.option, { notMerge: true })
  }
}

function handleResize() {
  chart && chart.resize()
}

onMounted(() => {
  render()
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(chartEl.value)
  } else {
    window.addEventListener('resize', handleResize)
  }
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  } else {
    window.removeEventListener('resize', handleResize)
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>
