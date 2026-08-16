<template>
  <div class="order-detail">
    <div v-if="!order.orderId" class="loading">
      <el-empty description="加载中..." />
    </div>
    <template v-else>
      <el-card style="margin-bottom: 15px;">
        <div class="section-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)">{{ statusText(order.status) }}</el-tag>
        </div>
        <div class="info-row"><span>下单时间：{{ formatTime(order.createTime) }}</span></div>
        <div v-if="order.payTime" class="info-row"><span>支付时间：{{ formatTime(order.payTime) }}</span></div>
        <div class="info-row">
          <span>收货地址：{{ parseAddress(order.addressSnapshot) }}</span>
        </div>
        <div v-if="order.remark" class="info-row"><span>备注：{{ order.remark }}</span></div>
      </el-card>

      <el-card style="margin-bottom: 15px;">
        <h4>商品清单</h4>
        <el-table :data="order.items || []" style="width: 100%;">
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="item-cell">
                <img :src="row.productImage" />
                <div>
                  <div>{{ row.productName }}</div>
                  <el-tag v-if="row.isGift" type="warning" size="small">赠品</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card>
        <div class="amounts">
          <div>商品金额：<span>¥{{ order.totalAmount }}</span></div>
          <div>运费：<span>¥{{ order.freightAmount }}</span></div>
          <div>优惠：<span>-¥{{ order.discountAmount }}</span></div>
          <div class="pay-amount">实付金额：<strong>¥{{ order.payAmount }}</strong></div>
        </div>
        <div style="margin-top: 20px;">
          <el-button v-if="order.status === 0" type="primary" @click="goPay">去支付</el-button>
          <el-button v-if="order.status === 0" @click="cancelOrder">取消订单</el-button>
          <el-button v-if="order.status === 2" type="success" @click="confirmOrder">确认收货</el-button>
          <!-- R-1：申请退款（待发货1/已发货2/已收货3/已完成4，与后端 RefundService 放行口径一致） -->
          <el-button
            v-if="order.status >= 1 && order.status <= 4"
            type="warning"
            plain
            @click="openRefundDialog"
          >申请退款</el-button>
          <el-button v-if="order.status >= 2 && order.status !== 5 && order.status !== 7" @click="showLogistics">查看物流</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>

        <!-- 物流弹窗 -->
        <el-dialog v-model="logisticsVisible" title="物流信息" width="500px">
          <div v-if="logisticsLoading" style="text-align: center; padding: 30px;">加载中...</div>
          <div v-else-if="logisticsError" style="text-align: center; padding: 30px; color: #999;">{{ logisticsError }}</div>
          <pre v-else style="white-space: pre-wrap; font-size: 13px; max-height: 400px; overflow-y: auto;">{{ logisticsInfo }}</pre>
        </el-dialog>

        <!-- 退款申请 Dialog（R-1：待发货1/已发货2/已收货3/已完成4 可申请） -->
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
            <el-form-item label="退款类型" required>
              <el-radio-group v-model="refundForm.type">
                <el-radio :label="1">仅退款</el-radio>
                <el-radio :label="2">退货退款</el-radio>
              </el-radio-group>
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
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/order'
import { queryLogistics } from '@/api/logistics'

const route = useRoute()
const router = useRouter()
const order = ref({})

const statusMap = { '0': '待付款', '1': '待发货', '2': '已发货', '3': '已收货', '4': '已完成', '5': '已取消', '6': '退款中', '7': '已退款' }

async function loadOrder() {
  try {
    const data = await request.getOrderDetail(route.params.id)
    order.value = data
  } catch {
    ElMessage.error('加载订单失败')
  }
}

onMounted(loadOrder)

function statusText(status) {
  return statusMap[status] || '未知'
}
function statusType(status) {
  if (status == null) return 'info'
  if ([5, 7].includes(status)) return 'info'
  if (status === 0) return 'danger'
  if (status === 4) return 'success'
  return 'warning'
}
function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}
function parseAddress(snapshot) {
  if (!snapshot) return '-'
  try {
    const obj = JSON.parse(snapshot)
    return [obj.province, obj.city, obj.district, obj.detail, obj.receiver, obj.phone].filter(Boolean).join('，')
  } catch {
    return snapshot
  }
}
function goPay() {
  router.push(`/order/pay/${order.value.orderId}`)
}
async function cancelOrder() {
  try {
    await request.cancelOrder(order.value.orderId)
    ElMessage.success('已取消')
    order.value.status = 5
  } catch {
    // ignore
  }
}
async function confirmOrder() {
  try {
    await request.confirmOrder(order.value.orderId)
    ElMessage.success('已确认收货')
    order.value.status = 4
  } catch {
    // ignore
  }
}
// ==================== R-1 申请退款（已收货3/已完成4） ====================
const refundDialogVisible = ref(false)
const refundSubmitting = ref(false)
const refundableItems = ref([])
const refundForm = ref({ orderItemId: null, type: 1, reason: '', amount: null })
const refundMaxAmount = ref(0)

function openRefundDialog() {
  // 可退款商品：非赠品行
  refundableItems.value = (order.value.items || []).filter(i => !i.isGift)
  const first = refundableItems.value[0]
  if (first) {
    const itemAmount = Number(first.price) * first.quantity
    const maxAmount = Math.min(itemAmount, Number(order.value.payAmount || 0))
    refundForm.value = { orderItemId: first.id, type: 1, reason: '', amount: maxAmount }
    refundMaxAmount.value = maxAmount
  } else {
    refundForm.value = { orderItemId: null, type: 1, reason: '', amount: null }
    refundMaxAmount.value = 0
  }
  refundDialogVisible.value = true
}

// 切换退款商品时重算可退金额上限（金额 ≤ 该行实付 且 ≤ 订单实付）
watch(() => refundForm.value.orderItemId, (id) => {
  if (!id) {
    refundMaxAmount.value = 0
    return
  }
  const item = refundableItems.value.find(i => i.id === id)
  if (item) {
    const maxAmount = Math.min(Number(item.price) * item.quantity, Number(order.value.payAmount || 0))
    refundMaxAmount.value = maxAmount
    refundForm.value.amount = maxAmount
  }
})

async function submitRefund() {
  if (!refundForm.value.orderItemId) {
    ElMessage.warning('请选择退款商品')
    return
  }
  if (!refundForm.value.amount || refundForm.value.amount <= 0) {
    ElMessage.warning('请输入正确的退款金额')
    return
  }
  if (refundForm.value.amount > refundMaxAmount.value) {
    ElMessage.warning('退款金额不能超过实付金额')
    return
  }
  if (!refundForm.value.reason) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundSubmitting.value = true
  try {
    await request.refundOrder(order.value.orderId, {
      orderItemId: refundForm.value.orderItemId,
      type: refundForm.value.type,
      reason: refundForm.value.reason,
      amount: refundForm.value.amount,
    })
    ElMessage.success('退款申请已提交，等待商家处理')
    refundDialogVisible.value = false
    loadOrder()
  } catch {
    ElMessage.error('退款申请提交失败')
  } finally {
    refundSubmitting.value = false
  }
}
// 物流查询
const logisticsVisible = ref(false)
const logisticsLoading = ref(false)
const logisticsError = ref('')
const logisticsInfo = ref('')
async function showLogistics() {
  logisticsVisible.value = true
  logisticsLoading.value = true
  logisticsError.value = ''
  logisticsInfo.value = ''
  try {
    const data = await queryLogistics(order.value.orderId)
    // 后端返回 JSON 字符串，尝试格式化
    try {
      logisticsInfo.value = JSON.stringify(JSON.parse(data), null, 2)
    } catch {
      logisticsInfo.value = data || '暂无物流信息'
    }
  } catch {
    logisticsError.value = '物流信息暂不可用'
  } finally {
    logisticsLoading.value = false
  }
}
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.info-row {
  margin-top: 8px;
  color: #666;
}
.item-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.item-cell img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}
.amounts div {
  margin-top: 6px;
}
.pay-amount {
  margin-top: 10px;
  font-size: 16px;
  color: #f56c6c;
}
</style>
