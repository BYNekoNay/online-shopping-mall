<template>
  <div class="apply-pending">
    <el-result icon="info" title="申请审核中" sub-title="请耐心等待管理员审核">
      <template v-if="rejectReason" #extra>
        <p style="color: #f56c6c;">拒绝原因：{{ rejectReason }}</p>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMerchantStore } from '@/store/merchant'

const merchantStore = useMerchantStore()
const rejectReason = ref('')

onMounted(() => {
  rejectReason.value = merchantStore.rejectReason || ''
})
</script>
