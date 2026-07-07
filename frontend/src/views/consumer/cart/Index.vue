<template>
  <div class="cart-page">
    <div v-if="items.length === 0" class="empty-cart">
      <el-empty description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>
    <div v-else>
      <el-card>
        <template #header>
          <div class="cart-header">
            <span>购物车 ({{ items.length }})</span>
            <el-button type="text" @click="handleClear">清空购物车</el-button>
          </div>
        </template>
        <div v-for="item in items" :key="item.id" class="cart-item">
          <el-checkbox v-model="item.selected" @change="toggleSelect(item)" />
          <img :src="item.productImage" class="cart-item-image" />
          <div class="cart-item-info">
            <div>{{ item.productName }}</div>
            <div class="cart-item-spec" v-if="item.specDesc">{{ item.specDesc }}</div>
          </div>
          <div class="cart-item-price">¥{{ item.price }}</div>
          <div class="cart-item-quantity">
            <el-input-number v-model="item.quantity" :min="1" :max="item.stock || 99" size="small" @change="updateQuantity(item)" />
          </div>
          <div class="cart-item-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          <div v-if="!item.stockEnough" class="stock-warning">库存不足</div>
          <el-button type="text" @click="removeItem(item)">删除</el-button>
        </div>
        <div class="cart-footer">
          <div class="cart-total">
            已选 {{ selectedCount }} 件，合计：<span class="total-price">¥{{ selectedTotal.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" :disabled="selectedCount === 0" @click="goCheckout">
            结算
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/store/cart'
import request from '@/api/cart'

const router = useRouter()
const cartStore = useCartStore()
const items = computed(() => cartStore.items)
const selectedCount = computed(() => items.value.filter(i => i.selected).length)
const selectedTotal = computed(() => cartStore.getSelectedTotal())

async function toggleSelect(item) {
  await request.updateCartItem(item.id, { selected: item.selected ? 1 : 0 })
}

async function updateQuantity(item) {
  await request.updateCartItem(item.id, { quantity: item.quantity })
}

async function removeItem(item) {
  await request.deleteCartItem(item.id)
  cartStore.removeItem(item.id)
}

async function handleClear() {
  await ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
  for (const item of items.value) {
    await request.deleteCartItem(item.id)
  }
  cartStore.items = []
  ElMessage.success('Cleared')
}

function goCheckout() {
  router.push('/order/confirm')
}
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 20px auto;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #eee;
  gap: 15px;
}
.cart-item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.cart-item-info {
  flex: 1;
}
.cart-item-spec {
  color: #999;
  font-size: 12px;
  margin-top: 5px;
}
.cart-item-price {
  width: 100px;
  text-align: center;
  color: #666;
}
.cart-item-quantity {
  width: 120px;
  text-align: center;
}
.cart-item-subtotal {
  width: 100px;
  text-align: right;
  color: #f56c6c;
  font-weight: bold;
}
.stock-warning {
  color: #e6a23c;
  font-size: 12px;
  margin-right: 8px;
}
.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px 0;
  gap: 20px;
}
.total-price {
  font-size: 24px;
  color: #f56c6c;
}
.empty-cart {
  padding: 100px 0;
}
</style>
