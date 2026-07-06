<template>
  <div class="user-coupons">
    <el-card>
      <h3>我的优惠券</h3>
      <el-tabs v-model="activeStatus">
        <el-tab-pane label="未使用" name="0" />
        <el-tab-pane label="已使用" name="1" />
        <el-tab-pane label="已过期" name="2" />
      </el-tabs>
      <el-empty description="暂无优惠券" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activeStatus = ref('0')
</script>
