<template>
  <div class="product-detail">
    <el-skeleton :loading="loading" animated>
      <template #template>
        <el-skeleton-item variant="image" style="width: 400px; height: 400px;" />
        <div style="flex: 1; padding-left: 30px;">
          <el-skeleton :rows="5" />
        </div>
      </template>
      <template #default>
        <div class="detail-content">
          <div class="image-section">
            <el-image :src="product.mainImage" fit="contain" style="width: 400px; height: 400px;" />
          </div>
          <div class="info-section">
            <h1>{{ product.name }}</h1>
            <div class="price-row">
              <span class="price">¥{{ product.price }}</span>
              <span class="original-price" v-if="product.originalPrice">¥{{ product.originalPrice }}</span>
            </div>
            <div class="stock-info">
              库存：{{ product.stock }} | 销量：{{ product.sales }}
            </div>
            <div class="promotion-tag" v-if="product.activePromotion">
              {{ formatPromotionTag(product.activePromotion) }}
            </div>
            <div class="sku-section" v-if="product.skuList && product.skuList.length > 0">
              <h4>选择规格</h4>
              <div class="sku-options">
                <el-radio-group v-model="selectedSkuId" @change="onSkuChange">
                  <el-radio v-for="sku in product.skuList" :key="sku.id" :label="sku.id" border>
                    {{ sku.specJson }} ¥{{ sku.price }}
                  </el-radio>
                </el-radio-group>
              </div>
            </div>
            <div class="actions">
              <el-button type="primary" size="large" :disabled="!canAddToCart" @click="addToCart">
                加入购物车
              </el-button>
            </div>
          </div>
        </div>
        <div class="similar-section">
          <h3>相似商品推荐</h3>
          <RecommendList mode="similar" :product-id="productId" />
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/product'
import { useCartStore } from '@/store/cart'
import RecommendList from '@/components/RecommendList.vue'

const route = useRoute()
const cartStore = useCartStore()
const productId = computed(() => Number(route.params.id))
const loading = ref(false)
const product = ref({})
const selectedSkuId = ref(null)

const canAddToCart = computed(() => {
  if (product.value.skuList && product.value.skuList.length > 0) {
    return selectedSkuId.value !== null
  }
  return product.value.stock > 0
})

function formatPromotionTag(promotion) {
  const typeMap = { 1: '折扣', 2: '满减', 3: '满赠', 4: '套餐' }
  return `${typeMap[promotion.type] || '促销'}: ${promotion.name}`
}

function onSkuChange(skuId) {
  selectedSkuId.value = skuId
}

async function addToCart() {
  try {
    await cartStore.fetchList()
    const skuId = selectedSkuId.value || (product.value.skuList && product.value.skuList[0]?.id) || null
    await request.addToCart({ productId: product.value.id, skuId, quantity: 1 })
    ElMessage.success('Added to cart')
    cartStore.fetchList()
  } catch {
    // error handled by interceptor
  }
}

onMounted(async () => {
  loading.value = true
  try {
    product.value = await request.getProduct(productId.value)
  } catch {
    ElMessage.error('Failed to load product')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.product-detail {
  max-width: 1200px;
  margin: 20px auto;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
}
.detail-content {
  display: flex;
  gap: 30px;
}
.image-section {
  flex-shrink: 0;
}
.info-section {
  flex: 1;
}
.info-section h1 {
  font-size: 22px;
  margin-bottom: 15px;
}
.price-row {
  margin: 20px 0;
}
.price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: bold;
}
.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
  margin-left: 10px;
}
.stock-info {
  color: #666;
  margin-bottom: 15px;
}
.promotion-tag {
  display: inline-block;
  padding: 5px 12px;
  background: #fdf6ec;
  color: #e6a23c;
  border-radius: 4px;
  margin-bottom: 15px;
}
.sku-section {
  margin: 20px 0;
}
.sku-options {
  margin-top: 10px;
}
.actions {
  margin-top: 30px;
}
.similar-section {
  margin-top: 40px;
}
.similar-section h3 {
  margin-bottom: 15px;
}
</style>
