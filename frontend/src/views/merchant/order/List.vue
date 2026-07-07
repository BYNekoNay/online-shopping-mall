<template>
  <div class="merchant-orders">
    <el-card>
      <h3>订单管理</h3>
      <el-table :data="orders" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="orderNo" label="订单号" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥{{ row.payAmount }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="success" size="small" @click="openShipDialog(row)">发货</el-button>
            <el-button size="small" @click="$router.push(`/merchant/orders/${row.orderId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 发货对话框 -->
      <el-dialog v-model="shipDialogVisible" title="发货" width="500px">
        <el-form :model="shipForm" label-width="100px">
          <el-form-item label="物流公司" required>
            <el-input v-model="shipForm.logisticsCompany" placeholder="请输入物流公司" />
          </el-form-item>
          <el-form-item label="物流单号" required>
            <el-input v-model="shipForm.trackingNo" placeholder="请输入物流单号" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="shipDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="shipSubmitting" @click="confirmShip">确认发货</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const orders = ref([])
const shipDialogVisible = ref(false)
const shipSubmitting = ref(false)
const currentOrderId = ref(null)
const shipForm = ref({ logisticsCompany: '', trackingNo: '' })

onMounted(async () => {
  try { orders.value = await request.getMerchantOrders() } catch {}
})

function openShipDialog(order) {
  currentOrderId.value = order.orderId
  shipForm.value = { logisticsCompany: '', trackingNo: '' }
  shipDialogVisible.value = true
}

async function confirmShip() {
  if (!shipForm.value.logisticsCompany || !shipForm.value.trackingNo) {
    ElMessage.warning('请填写完整物流信息')
    return
  }
  shipSubmitting.value = true
  try {
    await request.shipOrder(currentOrderId.value, shipForm.value)
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    const order = orders.value.find(o => o.orderId === currentOrderId.value)
    if (order) order.statusText = '已发货'
  } catch {
    ElMessage.error('发货失败')
  } finally {
    shipSubmitting.value = false
  }
}
</script>
