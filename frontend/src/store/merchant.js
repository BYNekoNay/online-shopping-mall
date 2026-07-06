import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useMerchantStore = defineStore('merchant', () => {
  const shopStatus = ref(-1)
  const rejectReason = ref('')
  const shopId = ref(0)

  async function fetchApplyStatus() {
    try {
      const data = await request.get('/merchant/shop/apply-status')
      shopStatus.value = data.status ?? -1
      rejectReason.value = data.rejectReason || ''
      shopId.value = data.shopId || 0
    } catch {
      shopStatus.value = -1
    }
  }

  return { shopStatus, rejectReason, shopId, fetchApplyStatus }
})
