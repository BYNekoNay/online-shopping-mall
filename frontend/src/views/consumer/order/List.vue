<template>
  <div class="order-list">
    <el-tabs v-model="activeStatus" @tab-change="loadOrders">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="已发货" name="2" />
      <el-tab-pane label="已完成" name="4" />
      <el-tab-pane label="已取消" name="5" />
      <el-tab-pane label="退款中" name="6" />
      <el-tab-pane label="已退款" name="7" />
    </el-tabs>
    <div v-if="orders.length === 0" class="empty">
      <el-empty description="暂无订单" />
    </div>
    <el-card v-for="order in orders" :key="order.orderId" class="order-card" style="margin-bottom: 15px;">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <span class="order-status">{{ formatStatus(order.status) }}</span>
      </div>
      <div class="order-items">
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <img :src="item.productImage" />
          <div class="item-info">
            <div>{{ item.productName }}</div>
            <div v-if="item.isGift" class="gift-tag">赠品</div>
          </div>
          <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
        </div>
      </div>
      <div class="order-footer">
        <span>实付：<strong>¥{{ order.payAmount }}</strong></span>
        <div>
          <el-button v-if="order.status === 0" type="primary" @click="$router.push(`/order/pay/${order.orderId}`)">去支付</el-button>
          <el-button v-if="order.status === 0" @click="cancelOrder(order.orderId)">取消订单</el-button>
          <el-button v-if="order.status === 2" @click="confirmOrder(order.orderId)">确认收货</el-button>
          <!-- R-1：申请退款（待发货1/已发货2/已收货3/已完成4，与后端 RefundService 放行口径一致） -->
          <el-button
            v-if="order.status >= 1 && order.status <= 4"
            type="warning"
            plain
            @click="openRefundDialog(order)"
          >申请退款</el-button>
          <el-button type="default" @click="$router.push(`/orders/${order.orderId}`)">查看详情</el-button>
          <!-- B-1：删除订单（仅已取消5/已退款7 显示） -->
          <el-button
            v-if="order.status === 5 || order.status === 7"
            type="danger"
            plain
            @click="handleDeleteOrder(order.orderId)"
          >删除</el-button>
        </div>
      </div>
    </el-card>

    <!-- R-1 退款申请 Dialog -->
    <el-dialog v-model="refundDialogVisible" title="申请退款" width="480px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="退款商品" required>
          <el-select v-model="refundForm.orderItemId" placeholder="选择退款商品" style="width: 100%;">
            <el-option
              v-for="item in refundableItems"
              :key="item.id"
              :label="`${item.productName} x${item.quantity}（¥${item.price}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额" required>
          <el-input-number v-model="refundForm.amount" :min="0.01" :max="refundMaxAmount" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input v-model="refundForm.reason" type="textarea" :rows="3" maxlength="200" placeholder="请填写退款原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/order'

const activeStatus = ref('')
const orders = ref([])

const statusMap = { '0': '待付款', '1': '待发货', '2': '已发货', '3': '已收货', '4': '已完成', '5': '已取消', '6': '退款中', '7': '已退款' }

function formatStatus(status) {
  return statusMap[status] || '未知'
}

async function loadOrders() {
  try {
    const data = await request.getOrders(activeStatus.value ? { status: activeStatus.value } : {})
    orders.value = data.records || data
  } catch {
    orders.value = []
  }
}

async function cancelOrder(id) {
  try {
    await request.cancelOrder(id)
    ElMessage.success('已取消')
    loadOrders()
  } catch {
    ElMessage.error('取消订单失败')
  }
}

// ==================== R-1 申请退款 ====================
const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const currentRefundOrder = ref(null)
const refundableItems = ref([])
const refundForm = ref({ orderItemId: null, reason: '', amount: null })
const refundMaxAmount = ref(0)

function openRefundDialog(order) {
  currentRefundOrder.value = order
  // 可退款商品：非赠品行
  refundableItems.value = (order.items || []).filter(i => !i.isGift)
  const first = refundableItems.value[0]
  if (first) {
    refundForm.value = { orderItemId: first.id, reason: '', amount: Number(first.price) * first.quantity }
    refundMaxAmount.value = Number(first.price) * first.quantity
  } else {
    refundForm.value = { orderItemId: null, reason: '', amount: null }
    refundMaxAmount.value = 0
  }
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!refundForm.value.orderItemId) {
    ElMessage.warning('请选择退款商品')
    return
  }
  if (!refundForm.value.amount || refundForm.value.amount <= 0) {
    ElMessage.warning('请输入正确的退款金额')
    return
  }
  if (!refundForm.value.reason) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundSubmitting.value = true
  try {
    await request.refundOrder(currentRefundOrder.value.orderId, {
      orderItemId: refundForm.value.orderItemId,
      type: 1,
      reason: refundForm.value.reason,
      amount: refundForm.value.amount,
    })
    ElMessage.success('退款申请已提交，等待商家处理')
    refundDialogVisible.value = false
    loadOrders()
  } catch {
    ElMessage.error('退款申请提交失败')
  } finally {
    refundSubmitting.value = false
  }
}

async function confirmOrder(id) {
  try {
    await request.confirmOrder(id)
    ElMessage.success('已确认')
    loadOrders()
  } catch {
    ElMessage.error('确认收货失败')
  }
}

// B-1 删除订单（二次确认 + 失败提示）
async function handleDeleteOrder(id) {
  try {
    await ElMessageBox.confirm('删除后该订单将从列表移除（保留对账记录），确认删除？', '删除订单', { type: 'warning' })
    await request.deleteOrder(id)
    ElMessage.success('已删除')
    loadOrders()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败，仅已取消/已退款的订单可删除')
    }
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.order-header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f5f5;
}
.order-status {
  color: #f56c6c;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
}
.order-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}
.item-info {
  flex: 1;
}
.gift-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 5px;
}
.item-price {
  color: #666;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #f5f5f5;
}
</style>
