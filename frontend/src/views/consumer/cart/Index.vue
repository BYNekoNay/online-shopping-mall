<template>
  <div class="cart-page">
    <div v-if="items.length === 0" class="empty-cart">
      <el-empty description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>
    <div v-else>
      <div class="cart-card">
        <div class="cart-header">
          <span class="cart-title">购物车 ({{ items.length }})</span>
          <div>
            <!-- D-4 全选/反选 -->
            <el-checkbox
              :model-value="allSelected"
              :indeterminate="partialSelected"
              style="margin-right: 16px"
              @change="toggleAll"
            >
              全选
            </el-checkbox>
            <el-button text @click="handleClear">清空购物车</el-button>
          </div>
        </div>

        <!-- 表头 -->
        <div class="cart-table-header">
          <span class="col-check">全选</span>
          <span class="col-product">商品信息</span>
          <span class="col-price">单价</span>
          <span class="col-quantity">数量</span>
          <span class="col-subtotal">小计</span>
          <span class="col-action">操作</span>
        </div>

        <div v-for="item in items" :key="item.id" class="cart-item">
          <div class="col-check">
            <el-checkbox :model-value="item.selected" @change="(val) => toggleSelect(item, val)" />
          </div>
          <div class="col-product">
            <img
              :src="resolvedItemImage(item)"
              :data-seed="item.productId"
              class="cart-item-image"
              :alt="item.productName"
              loading="lazy"
              @error="handleImgError"
            />
            <div class="cart-item-info">
              <div class="item-name">{{ item.productName }}</div>
              <div class="cart-item-spec" v-if="item.specText">{{ item.specText }}</div>
              <div v-if="!item.stockEnough" class="stock-warning">库存不足</div>
            </div>
          </div>
          <div class="col-price">¥{{ item.price }}</div>
          <div class="col-quantity">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.stock || 99"
              size="small"
              @change="updateQuantity(item)"
            />
          </div>
          <div class="col-subtotal">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
          <div class="col-action">
            <el-button text @click="removeItem(item)">删除</el-button>
          </div>
        </div>

        <div class="cart-footer">
          <div class="cart-total">
            已选 <strong>{{ selectedCount }}</strong> 件，合计：
            <span class="total-price">¥{{ selectedTotal.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" :disabled="selectedCount === 0" @click="goCheckout">
            结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/store/cart'
import request from '@/api/cart'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const router = useRouter()
const cartStore = useCartStore()
const items = computed(() => cartStore.items)
const selectedCount = computed(() => items.value.filter((i) => i.selected).length)
const selectedTotal = computed(() => cartStore.getSelectedTotal())
// D-4 全选状态
const allSelected = computed(() => items.value.length > 0 && items.value.every((i) => i.selected))
const partialSelected = computed(() => items.value.some((i) => i.selected) && !allSelected.value)

/** 购物车商品图兜底：占位/空图 → picsum。
 *  FRONT-05 修复：CartVO 返回字段为 mainImage（此前误用 productImage，图片恒走占位图兜底）。 */
function resolvedItemImage(item) {
  return resolveImg(item.mainImage || item.productImage || '', item.productId ?? item.productName ?? 'default', 160, 160)
}

/** 图片加载失败兜底处理。 */
function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 160, 160)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
}

// D-4 全选/取消全选（批量接口）
async function toggleAll(val) {
  const selected = val ? 1 : 0
  try {
    await request.selectAllCart(selected)
    items.value.forEach((i) => cartStore.updateItem(i.id, { selected }))
    ElMessage.success(selected ? '已全选' : '已取消全选')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function toggleSelect(item, val) {
  // H-18 修复：val 为变更后的值；原实现读 item.selected 旧值，导致提交状态恒反转
  const selected = val ? 1 : 0
  await request.updateCartItem(item.id, { selected })
  cartStore.updateItem(item.id, { selected })
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
  cartStore.clear()
  ElMessage.success('已清空')
}

function goCheckout() {
  router.push('/order/confirm')
}
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 24px;
}
.cart-card {
  background: #fff;
  border: 1px solid #eef0f4;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  padding: 24px;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}
.cart-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.cart-table-header {
  display: flex;
  align-items: center;
  padding: 12px 0;
  font-size: 13px;
  color: #94a3b8;
  border-bottom: 1px solid #f1f5f9;
}
.cart-table-header .col-check {
  width: 60px;
}
.cart-table-header .col-product {
  flex: 1;
}
.cart-table-header .col-price {
  width: 100px;
  text-align: center;
}
.cart-table-header .col-quantity {
  width: 140px;
  text-align: center;
}
.cart-table-header .col-subtotal {
  width: 110px;
  text-align: right;
}
.cart-table-header .col-action {
  width: 70px;
  text-align: center;
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f6f8fb;
  gap: 0;
}
.cart-item .col-check {
  width: 60px;
}
.cart-item .col-product {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.cart-item .col-price {
  width: 100px;
  text-align: center;
  color: #334155;
}
.cart-item .col-quantity {
  width: 140px;
  text-align: center;
}
.cart-item .col-subtotal {
  width: 110px;
  text-align: right;
  color: #ef4444;
  font-weight: 700;
}
.cart-item .col-action {
  width: 70px;
  text-align: center;
}
.cart-item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #eef0f4;
  background: #f8fafc;
  flex-shrink: 0;
}
.cart-item-info {
  min-width: 0;
}
.item-name {
  font-size: 14px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-item-spec {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 5px;
}
.stock-warning {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 5px;
}
.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px 0 0;
  gap: 20px;
}
.cart-total {
  font-size: 14px;
  color: #334155;
}
.cart-total strong {
  color: #ef4444;
}
.total-price {
  font-size: 26px;
  font-weight: 800;
  color: #ef4444;
}
.empty-cart {
  padding: 100px 0;
}
</style>
