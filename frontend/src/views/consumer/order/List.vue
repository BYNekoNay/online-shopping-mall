<template>
  <div class="order-list">
    <div class="list-panel">
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
      <div v-for="order in orders" :key="order.orderId" class="order-card">
        <div class="order-header">
          <span class="order-no">
            <span class="no-label">订单号：</span>{{ order.orderNo }}
          </span>
          <span class="order-status" :class="`status-${order.status}`">{{ formatStatus(order.status) }}</span>
        </div>
        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <img
              :src="resolvedItemImage(item)"
              :data-seed="item.productId"
              :alt="item.productName"
              loading="lazy"
              @error="handleImgError"
            />
            <div class="item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div v-if="item.isGift" class="gift-tag">赠品</div>
            </div>
            <div class="item-price">¥{{ item.price }} x {{ item.quantity }}</div>
          </div>
        </div>
        <div class="order-footer">
          <span class="pay-amount"
            >实付：<strong>¥{{ order.payAmount }}</strong></span
          >
          <div class="order-actions">
            <el-button v-if="order.status === 0" type="primary" @click="$router.push(`/order/pay/${order.orderId}`)"
              >去支付</el-button
            >
            <el-button v-if="order.status === 0" @click="cancelOrder(order.orderId)">取消订单</el-button>
            <el-button v-if="order.status === 2" @click="confirmOrder(order.orderId)">确认收货</el-button>
            <!-- R-1：申请退款（待发货1/已发货2/已收货3/已完成4，与后端 RefundService 放行口径一致） -->
            <el-button v-if="order.status >= 1 && order.status <= 4" type="warning" plain @click="openRefundDialog(order)"
              >申请退款</el-button
            >
            <el-button @click="$router.push(`/orders/${order.orderId}`)">查看详情</el-button>
            <!-- B-1：删除订单（仅已取消5/已退款7 显示） -->
            <el-button
              v-if="order.status === 5 || order.status === 7"
              type="danger"
              plain
              @click="handleDeleteOrder(order.orderId)"
              >删除</el-button
            >
          </div>
        </div>
      </div>
    </div>

    <!-- R-1 退款申请 Dialog -->
    <el-dialog v-model="refundDialogVisible" title="申请退款" width="480px">
      <el-form :model="refundForm" label-width="100px">
        <el-form-item label="退款商品" required>
          <el-select v-model="refundForm.orderItemId" placeholder="选择退款商品" style="width: 100%">
            <el-option
              v-for="item in refundableItems"
              :key="item.id"
              :label="`${item.productName} x${item.quantity}（¥${item.price}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额" required>
          <el-input-number
            v-model="refundForm.amount"
            :min="0.01"
            :max="refundMaxAmount"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退款原因" required>
          <el-input
            v-model="refundForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="请填写退款原因"
          />
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
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/order'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const activeStatus = ref('')
const orders = ref([])

const statusMap = {
  0: '待付款',
  1: '待发货',
  2: '已发货',
  3: '已收货',
  4: '已完成',
  5: '已取消',
  6: '退款中',
  7: '已退款'
}

function formatStatus(status) {
  return statusMap[status] || '未知'
}

/** 订单商品图兜底：占位/空图 → picsum。 */
function resolvedItemImage(item) {
  return resolveImg(item.productImage || '', item.productId ?? item.productName ?? 'default', 120, 120)
}

function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 120, 120)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
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
  refundableItems.value = (order.items || []).filter((i) => !i.isGift)
  const first = refundableItems.value[0]
  if (first) {
    const itemAmount = Number(first.price) * first.quantity
    const maxAmount = Math.min(itemAmount, Number(order.payAmount || 0))
    refundForm.value = { orderItemId: first.id, reason: '', amount: maxAmount }
    refundMaxAmount.value = maxAmount
  } else {
    refundForm.value = { orderItemId: null, reason: '', amount: null }
    refundMaxAmount.value = 0
  }
  refundDialogVisible.value = true
}

// FRONT-11 修复：切换退款商品时重算可退金额上限（与 Detail.vue 同口径：
// 上限 = min(该行 price×quantity, 订单实付)），此前只按第一个商品设置，切换后金额被错误 clamp
watch(
  () => refundForm.value.orderItemId,
  (id) => {
    if (!id || !currentRefundOrder.value) {
      refundMaxAmount.value = 0
      return
    }
    const item = refundableItems.value.find((i) => i.id === id)
    if (item) {
      const maxAmount = Math.min(Number(item.price) * item.quantity, Number(currentRefundOrder.value.payAmount || 0))
      refundMaxAmount.value = maxAmount
      refundForm.value.amount = maxAmount
    }
  }
)

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
      amount: refundForm.value.amount
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
.order-list {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}
.list-panel {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  padding: 20px 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
}
.empty {
  padding: 40px 0;
}
.order-card {
  border: 1px solid #eef0f4;
  border-radius: 14px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.order-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.08);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}
.order-no {
  font-size: 13px;
  color: #334155;
}
.no-label {
  color: #94a3b8;
}
.order-status {
  font-size: 13px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
}
.order-status.status-0 {
  color: #ef4444;
  background: #fef2f2;
}
.order-status.status-1,
.order-status.status-2,
.order-status.status-3,
.order-status.status-6 {
  color: #d97706;
  background: #fffbeb;
}
.order-status.status-4 {
  color: #059669;
  background: #ecfdf5;
}
.order-status.status-5,
.order-status.status-7 {
  color: #64748b;
  background: #f1f5f9;
}
.order-items {
  padding: 6px 18px;
}
.order-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
}
.order-item img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #eef0f4;
  background: #f8fafc;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  color: #64748b;
  font-size: 13px;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-top: 1px solid #f6f8fb;
}
.pay-amount {
  font-size: 13px;
  color: #334155;
}
.pay-amount strong {
  font-size: 18px;
  color: #ef4444;
}
.order-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
