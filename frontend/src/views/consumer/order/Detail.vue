<template>
  <div class="order-detail">
    <div v-if="!order.id" class="loading">
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
                <img :src="row.productImageSnapshot" />
                <div>
                  <div>{{ row.productNameSnapshot }}</div>
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
          <el-button v-if="order.status >= 2 && order.status !== 5 && order.status !== 7" @click="showLogistics">查看物流</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>

        <!-- 物流弹窗 -->
        <el-dialog v-model="logisticsVisible" title="物流信息" width="500px">
          <div v-if="logisticsLoading" style="text-align: center; padding: 30px;">加载中...</div>
          <div v-else-if="logisticsError" style="text-align: center; padding: 30px; color: #999;">{{ logisticsError }}</div>
          <pre v-else style="white-space: pre-wrap; font-size: 13px; max-height: 400px; overflow-y: auto;">{{ logisticsInfo }}</pre>
        </el-dialog>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/order'
import { queryLogistics } from '@/api/logistics'

const route = useRoute()
const router = useRouter()
const order = ref({})

const statusMap = { '0': '待付款', '1': '待发货', '2': '已发货', '3': '已收货', '4': '已完成', '5': '已取消', '6': '退款中', '7': '已退款' }

onMounted(async () => {
  try {
    const data = await request.getOrderDetail(route.params.id)
    order.value = data
  } catch {
    ElMessage.error('加载订单失败')
  }
})

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
  router.push(`/order/pay/${order.value.id}`)
}
async function cancelOrder() {
  try {
    await request.cancelOrder(order.value.id)
    ElMessage.success('已取消')
    order.value.status = 5
  } catch {
    // ignore
  }
}
async function confirmOrder() {
  try {
    await request.confirmOrder(order.value.id)
    ElMessage.success('已确认收货')
    order.value.status = 4
  } catch {
    // ignore
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
    const data = await queryLogistics(order.value.id)
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
