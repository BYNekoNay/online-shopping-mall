<template>
  <div class="admin-logs">
    <el-card>
      <h3>操作日志</h3>
      <el-table :data="logs" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="operation" label="操作" />
        <el-table-column prop="target" label="目标" />
        <el-table-column prop="createTime" label="时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/api/admin'

const logs = ref([])
</script>
