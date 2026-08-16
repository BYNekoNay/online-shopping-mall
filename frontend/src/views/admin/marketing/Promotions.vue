<template>
  <div class="admin-promotions">
    <el-card>
      <div class="header">
        <h3>促销活动管理</h3>
        <el-button type="primary" @click="showCreateDialog">新建促销活动</el-button>
      </div>

      <AppTable :columns="promotionColumns" :data="list">
        <template #type="{ row }">
          <el-tag :type="typeTag(row.type)">{{ typeName(row.type) }}</el-tag>
        </template>
        <template #scope="{ row }">
          {{ scopeName(row.scope) }}
        </template>
        <template #status="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '进行中' : '已结束' }}</el-tag>
        </template>
        <template #action="{ row }">
          <el-button size="small" @click="editRow(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="offlineRow(row)" :disabled="row.status !== 1">下线</el-button>
          <el-button size="small" type="danger" @click="deleteRow(row)">删除</el-button>
        </template>
      </AppTable>
    </el-card>

    <!-- 创建/编辑弹窗 -->
    <AppDialog
      v-model="dialogVisible"
      :title="editing ? '编辑促销活动' : '新建促销活动'"
      width="500px"
      @confirm="submit"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="选择类型">
            <el-option label="限时折扣" :value="1" />
            <el-option label="满减" :value="2" />
            <el-option label="满赠" :value="3" />
            <el-option label="组合套餐" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="form.scope" placeholder="选择范围">
            <el-option label="全平台" value="PLATFORM" />
            <el-option label="店铺" value="SHOP" />
            <el-option label="分类" value="CATEGORY" />
            <el-option label="商品" value="PRODUCT" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围ID" v-if="form.scope !== 'PLATFORM'">
          <el-input-number v-model="form.scopeId" :min="0" />
        </el-form-item>
        <el-form-item label="折扣比例" v-if="form.type === 1">
          <el-input-number v-model="form.discountPercent" :min="0.1" :max="1" :step="0.1" :precision="1" />
          <span style="margin-left: 8px; color: #999">例：0.8 表示 8 折</span>
        </el-form-item>
        <el-form-item label="满减门槛" v-if="form.type === 2">
          <el-input-number v-model="form.threshold" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="减免金额" v-if="form.type === 2">
          <el-input-number v-model="form.reduce" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="套餐价" v-if="form.type === 4">
          <el-input-number v-model="form.packagePrice" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
    </AppDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppTable from '@/components/common/AppTable.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import request from '@/api/admin'

const list = ref([])
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({
  id: null,
  name: '',
  type: 1,
  scope: 'PLATFORM',
  scopeId: 0,
  startTime: '',
  endTime: '',
  discountPercent: 0.8,
  threshold: 100,
  reduce: 20,
  packagePrice: 299
})

const promotionColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '名称' },
  { label: '类型', slot: 'type', width: 100 },
  { label: '范围', slot: 'scope', width: 100 },
  { prop: 'startTime', label: '开始时间', width: 180 },
  { prop: 'endTime', label: '结束时间', width: 180 },
  { label: '状态', slot: 'status', width: 100 },
  { label: '操作', slot: 'action', width: 220, fixed: 'right' }
]

function typeName(t) {
  return { 1: '限时折扣', 2: '满减', 3: '满赠', 4: '组合套餐' }[t] || t
}
function typeTag(t) {
  return { 1: 'danger', 2: 'warning', 3: 'success', 4: 'info' }[t] || 'info'
}
function scopeName(s) {
  return { PLATFORM: '全平台', SHOP: '店铺', CATEGORY: '分类', PRODUCT: '商品' }[s] || s
}

async function load() {
  try {
    const data = await request.getPromotions()
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
    scope: 'PLATFORM',
    scopeId: 0,
    startTime: '',
    endTime: '',
    discountPercent: 0.8,
    threshold: 100,
    reduce: 20,
    packagePrice: 299
  }
  dialogVisible.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  try {
    const rule = JSON.parse(row.ruleJson || '{}')
    form.value.discountPercent = rule.discountPercent || 0.8
    form.value.threshold = rule.threshold || 100
    form.value.reduce = rule.reduce || 20
    form.value.packagePrice = rule.packagePrice || 299
  } catch {}
  dialogVisible.value = true
}

function buildRuleJson() {
  const { type, discountPercent, threshold, reduce, packagePrice } = form.value
  if (type === 1) return JSON.stringify({ discountPercent })
  if (type === 2) return JSON.stringify({ threshold, reduce })
  if (type === 4) return JSON.stringify({ packagePrice })
  return '{}'
}

async function submit() {
  const payload = {
    ...form.value,
    ruleJson: buildRuleJson()
  }
  delete payload.discountPercent
  delete payload.threshold
  delete payload.reduce
  delete payload.packagePrice
  try {
    if (editing.value && form.value.id) {
      await request.updatePromotion(form.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await request.createPromotion(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } catch {}
}

async function offlineRow(row) {
  try {
    await ElMessageBox.confirm('确认下线该促销活动？', '提示', { type: 'warning' })
    await request.offlinePromotion(row.id)
    ElMessage.success('已下线')
    load()
  } catch {}
}

async function deleteRow(row) {
  try {
    await ElMessageBox.confirm('确认删除该促销活动？', '提示', { type: 'warning' })
    await request.deletePromotion(row.id)
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
