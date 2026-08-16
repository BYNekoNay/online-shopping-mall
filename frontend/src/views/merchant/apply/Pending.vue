<template>
  <div class="apply-pending">
    <el-result v-if="!rejectReason" icon="info" title="申请审核中" sub-title="请耐心等待管理员审核"> </el-result>
    <el-result v-else icon="error" title="审核拒绝" sub-title="您的店铺入驻申请已被拒绝">
      <template #extra>
        <p style="color: #f56c6c">拒绝原因：{{ rejectReason }}</p>
        <el-button type="primary" @click="$router.push('/merchant/apply')">重新申请</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMerchantStore } from '@/store/merchant'
import request from '@/api/merchant'
import { useRouter } from 'vue-router'

const merchantStore = useMerchantStore()
const rejectReason = ref('')
const router = useRouter()

onMounted(async () => {
  try {
    const data = await request.getApplyStatus()
    if (data && data.status === 1) {
      // 已通过审核，跳转到店铺信息页
      router.replace('/merchant/shop/info')
    } else if (data && data.rejectReason) {
      rejectReason.value = data.rejectReason
      merchantStore.rejectReason = data.rejectReason
    }
  } catch {
    // 加载失败，使用 store 中的缓存数据
    rejectReason.value = merchantStore.rejectReason || ''
  }
})
</script>
