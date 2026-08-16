<template>
  <div class="admin-coupons">
    <el-card>
      <div class="header">
        <h3>优惠券管理</h3>
        <el-button type="primary" @click="showCreateDialog">新建优惠券</el-button>
      </div>

      <el-table :data="list" style="width: 100%; margin-top: 15px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ typeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="receivedCount" label="已领取" width="100" />
        <el-table-column prop="validFrom" label="开始时间" width="180" />
        <el-table-column prop="validTo" label="结束时间" width="180" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editRow(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="offlineRow(row)" :disabled="row.stock <= row.receivedCount"
              >下线</el-button
            >
            <el-button size="small" type="danger" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑优惠券' : '新建优惠券'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="选择类型">
            <el-option label="新人券" :value="1" />
            <el-option label="满减券" :value="2" />
            <el-option label="品类券" :value="3" />
            <el-option label="店铺券" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺ID" v-if="form.type === 3 || form.type === 4">
          <el-input-number v-model="form.shopId" :min="0" />
        </el-form-item>
        <el-form-item label="满减门槛">
          <el-input-number v-model="form.threshold" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="减免金额">
          <el-input-number v-model="form.discount" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.validTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/admin'

const list = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({
  id: null,
  name: '',
  type: 1,
  shopId: 0,
  stock: 100,
  threshold: 100,
  discount: 10,
  validFrom: '',
  validTo: ''
})

function typeName(t) {
  return { 1: '新人券', 2: '满减券', 3: '品类券', 4: '店铺券' }[t] || t
}
function typeTag(t) {
  return { 1: 'success', 2: 'primary', 3: 'warning', 4: 'info' }[t] || 'info'
}

async function load() {
  try {
    const data = await request.getCoupons()
    list.value = data || []
  } catch {
    list.value = []
  }
}

function showCreateDialog() {
  editing.value = false
  form.value = {
    id: null,
    name: '',
    type: 1,
    shopId: 0,
    stock: 100,
    threshold: 100,
    discount: 10,
    validFrom: '',
    validTo: ''
  }
  dialogVisible.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  // 解析 discountRule 为表单字段
  try {
    const rule = JSON.parse(row.discountRule || '{}')
    form.value.threshold = rule.threshold || 0
    form.value.discount = rule.discount || 0
  } catch {}
  dialogVisible.value = true
}

async function submit() {
  const discountRule = JSON.stringify({ threshold: form.value.threshold, discount: form.value.discount })
  const payload = { ...form.value, discountRule }
  delete payload.threshold
  delete payload.discount
  try {
    if (editing.value && form.value.id) {
      await request.updateCoupon(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await request.createCoupon(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } catch {}
}

async function offlineRow(row) {
  try {
    await ElMessageBox.confirm('确认下线该优惠券？', '提示', { type: 'warning' })
    await request.offlineCoupon(row.id)
    ElMessage.success('已下线')
    load()
  } catch {}
}

async function deleteRow(row) {
  try {
    await ElMessageBox.confirm('确认删除该优惠券？', '提示', { type: 'warning' })
    await request.deleteCoupon(row.id)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
