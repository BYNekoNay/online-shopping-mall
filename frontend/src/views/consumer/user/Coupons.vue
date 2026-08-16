<template>
  <div class="user-coupons">
    <el-card>
      <h3>我的优惠券</h3>
      <el-tabs v-model="activeStatus">
        <el-tab-pane label="可领取" name="-1" />
        <el-tab-pane label="未使用" name="0" />
        <el-tab-pane label="已使用" name="1" />
        <el-tab-pane label="已过期" name="2" />
      </el-tabs>

      <!-- 可领取 tab -->
      <div v-if="activeStatus === '-1'">
        <div v-if="availableCoupons.length === 0" class="empty-state">
          <el-empty description="暂无可用优惠券" />
        </div>
        <div v-else class="coupon-list">
          <div v-for="coupon in availableCoupons" :key="coupon.id" class="coupon-item available-coupon">
            <div class="coupon-left">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-rule">
                满{{ getThreshold(coupon.discountRule) }}减{{ getDiscount(coupon.discountRule) }}
              </div>
              <div class="coupon-valid">
                有效期：{{ formatDate(coupon.validFrom) }} - {{ formatDate(coupon.validTo) }}
              </div>
            </div>
            <div class="coupon-right">
              <el-button type="primary" size="small" @click="receive(coupon.id)">立即领取</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 已领取优惠券 -->
      <template v-if="activeStatus !== '-1'">
        <div v-if="filteredCoupons.length === 0" class="empty-state">
          <el-empty description="暂无优惠券" />
        </div>
        <div v-else class="coupon-list">
          <div
            v-for="coupon in filteredCoupons"
            :key="coupon.id"
            class="coupon-item"
            :class="{ used: coupon.status === 1, expired: coupon.status === 2 }"
          >
            <div class="coupon-left">
              <div class="coupon-name">{{ coupon.name }}</div>
              <div class="coupon-rule">
                满{{ getThreshold(coupon.discountRule) }}减{{ getDiscount(coupon.discountRule) }}
              </div>
              <div class="coupon-valid">
                有效期：{{ formatDate(coupon.validFrom) }} - {{ formatDate(coupon.validTo) }}
              </div>
            </div>
            <div class="coupon-right">
              <el-tag :type="coupon.status === 0 ? 'success' : coupon.status === 1 ? 'info' : 'danger'">
                {{ statusMap[coupon.status] }}
              </el-tag>
            </div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/coupon'

const activeStatus = ref('-1')
const coupons = ref([])
const availableCoupons = ref([])
const statusMap = { 0: '未使用', 1: '已使用', 2: '已过期' }

const filteredCoupons = computed(() => {
  if (activeStatus.value === '-1') return availableCoupons.value
  if (activeStatus.value === '') return coupons.value
  return coupons.value.filter((c) => c.status === Number(activeStatus.value))
})

async function loadCoupons() {
  try {
    const data = await request.getUserCoupons()
    coupons.value = data || []
  } catch {
    coupons.value = []
  }
}

async function loadAvailableCoupons() {
  try {
    const data = await request.getAvailableCoupons()
    availableCoupons.value = data || []
  } catch {
    availableCoupons.value = []
  }
}

async function receive(id) {
  try {
    await request.receiveCoupon(id)
    ElMessage.success('领取成功')
    await Promise.all([loadCoupons(), loadAvailableCoupons()])
  } catch {
    // error handled by interceptor
  }
}

function getThreshold(rule) {
  if (!rule) return 0
  try {
    const obj = JSON.parse(rule)
    return obj.threshold || 0
  } catch {
    return 0
  }
}

function getDiscount(rule) {
  if (!rule) return 0
  try {
    const obj = JSON.parse(rule)
    return obj.discount || 0
  } catch {
    return 0
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 10)
}

onMounted(() => {
  loadCoupons()
  loadAvailableCoupons()
})
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
}
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 15px;
}
.coupon-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s;
}
.coupon-item:hover {
  border-color: #409eff;
}
.coupon-item.used {
  background: #f5f7fa;
  border-color: #e4e7ed;
}
.coupon-item.expired {
  background: #f5f7fa;
  border-color: #e4e7ed;
}
.available-coupon:hover {
  border-color: #67c23a;
}
.coupon-left {
  flex: 1;
}
.coupon-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 6px;
}
.coupon-rule {
  font-size: 14px;
  color: #f56c6c;
  margin-bottom: 4px;
}
.coupon-valid {
  font-size: 12px;
  color: #999;
}
.coupon-right {
  margin-left: 20px;
}
</style>
