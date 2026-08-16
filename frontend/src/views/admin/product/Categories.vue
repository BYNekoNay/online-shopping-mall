<template>
  <div class="admin-categories">
    <el-card>
      <h3>分类管理</h3>
      <el-tree :data="categories" :props="{ label: 'name', children: 'children' }" style="margin-top: 15px" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/admin'

const categories = ref([])
onMounted(async () => {
  try {
    categories.value = await request.getCategories()
  } catch {}
})
</script>
