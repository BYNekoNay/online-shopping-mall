<template>
  <div class="merchant-product-edit">
    <el-card>
      <h3>{{ isEdit ? '编辑商品' : '发布商品' }}</h3>
      <el-form :model="form" label-width="120px" style="max-width: 800px; margin-top: 20px;">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="form.price" :min="0" :step="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :step="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="商品主图">
          <el-input v-model="form.mainImage" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-input v-model="form.images" placeholder="多张图片用逗号分隔" />
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.detail" type="textarea" :rows="4" placeholder="请输入商品详情" />
        </el-form-item>

        <!-- SKU 列表 -->
        <el-form-item label="规格 SKU">
          <div v-for="(sku, idx) in form.skus" :key="idx" class="sku-row">
            <el-input v-model="sku.specJson" placeholder="规格描述，如：颜色:红色;尺码:M" style="width: 220px;" />
            <el-input-number v-model="sku.price" :min="0" :step="0.01" :precision="2" placeholder="价格" />
            <el-input-number v-model="sku.stock" :min="0" placeholder="库存" />
            <el-input v-model="sku.image" placeholder="SKU图片URL" style="width: 180px;" />
            <el-button type="danger" size="small" @click="removeSku(idx)">删除</el-button>
          </div>
          <el-button type="primary" size="small" @click="addSku">添加规格</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/merchant'

const route = useRoute()
const router = useRouter()
const isEdit = ref(false)
const submitting = ref(false)
const categories = ref([])

const form = ref({
  name: '',
  categoryId: null,
  price: 0,
  originalPrice: null,
  stock: 0,
  mainImage: '',
  images: '',
  detail: '',
  skus: []
})

onMounted(async () => {
  // 加载分类列表
  try {
    const res = await request.getCategoriesTree ? [] : []
    const apiRes = await fetch('/api/products/categories/tree').then(r => r.json())
    categories.value = apiRes.records || apiRes || []
  } catch {
    categories.value = []
  }

  // 编辑模式：加载商品详情
  if (route.query.id) {
    isEdit.value = true
    try {
      const prodRes = await fetch(`/api/products/${route.query.id}`).then(r => r.json())
      const p = prodRes.records || prodRes
      form.value.name = p.name
      form.value.categoryId = p.categoryId
      form.value.price = p.price
      form.value.originalPrice = p.originalPrice
      form.value.stock = p.stock
      form.value.mainImage = p.mainImage
      form.value.images = p.images
      form.value.detail = p.detail
      form.value.skus = (p.skuList || []).map(s => ({
        specJson: s.specJson,
        price: s.price,
        stock: s.stock,
        image: s.image
      }))
    } catch {
      ElMessage.error('加载商品失败')
    }
  }
})

function addSku() {
  form.value.skus.push({ specJson: '', price: 0, stock: 0, image: '' })
}

function removeSku(idx) {
  form.value.skus.splice(idx, 1)
}

async function handleSubmit() {
  if (!form.value.name || !form.value.categoryId) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.value.name,
      categoryId: form.value.categoryId,
      price: form.value.price,
      originalPrice: form.value.originalPrice,
      stock: form.value.stock,
      mainImage: form.value.mainImage,
      images: form.value.images,
      detail: form.value.detail,
      skus: form.value.skus
    }
    if (isEdit.value) {
      await request.updateProduct(route.query.id, payload)
      ElMessage.success('更新成功')
    } else {
      await request.createProduct(payload)
      ElMessage.success('发布成功')
    }
    router.push('/merchant/products')
  } catch {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.sku-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
</style>
