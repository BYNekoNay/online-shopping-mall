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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetailDialog(row)">查看详情</el-button>
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

      <!-- 订单详情对话框 -->
      <el-dialog v-model="orderDetailDialogVisible" title="订单详情" width="700px" @open="loadOrderDetail">
        <template v-if="selectedOrderDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单ID" span="2">{{ selectedOrderDetail.orderId }}</el-descriptions-item>
            <el-descriptions-item label="订单编号">{{ selectedOrderDetail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag>{{ selectedOrderDetail.statusText }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <h4 style="margin: 20px 0 10px;">商品明细</h4>
          <el-table :data="selectedOrderDetail.items || []" size="small" border>
            <el-table-column prop="productName" label="商品名称" />
            <el-table-column prop="skuName" label="SKU" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="price" label="单价" width="100">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="小计" width="100">
              <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
            </el-table-column>
          </el-table>

          <el-descriptions :column="2" border style="margin-top: 20px;">
            <el-descriptions-item label="商品总额">¥{{ selectedOrderDetail.goodsAmount }}</el-descriptions-item>
            <el-descriptions-item label="运费">¥{{ selectedOrderDetail.freightAmount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="优惠抵扣">-¥{{ selectedOrderDetail.discountAmount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="实付金额">
              <span style="color: #f56c6c; font-weight: bold;">¥{{ selectedOrderDetail.payAmount }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-descriptions :column="2" border style="margin-top: 20px;">
            <el-descriptions-item label="收货人">{{ selectedOrderDetail.consignee || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ selectedOrderDetail.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" span="2">{{ selectedOrderDetail.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物流公司">{{ selectedOrderDetail.logisticsCompany || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物流单号">{{ selectedOrderDetail.trackingNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedOrderDetail.createTime?.replace('T', ' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ selectedOrderDetail.payTime?.replace('T', ' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ selectedOrderDetail.shipTime?.replace('T', ' ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ selectedOrderDetail.completeTime?.replace('T', ' ') || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <template #footer>
          <el-button @click="orderDetailDialogVisible = false">关闭</el-button>
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
const orderDetailDialogVisible = ref(false)
const selectedOrderDetail = ref(null)
const currentDetailOrderId = ref(null)

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
    // 刷新列表获取最新状态
    orders.value = await request.getMerchantOrders()
  } catch {
    ElMessage.error('发货失败')
  } finally {
    shipSubmitting.value = false
  }
}

function openDetailDialog(order) {
  currentDetailOrderId.value = order.orderId
  selectedOrderDetail.value = null
  orderDetailDialogVisible.value = true
}

async function loadOrderDetail() {
  try {
    selectedOrderDetail.value = await request.getMerchantOrderDetail(currentDetailOrderId.value)
  } catch {
    ElMessage.error('加载订单详情失败')
  }
}
</script>
