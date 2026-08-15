<template>
  <div class="admin-points-goods">
    <el-card>
      <div class="header">
        <h3>积分商城管理</h3>
        <el-button type="primary" @click="openCreate">新增商品</el-button>
      </div>
      <el-table :data="goodsList" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="pointsCost" label="所需积分" width="100" />
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; justify-content: flex-end;"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑兑换商品' : '新增兑换商品'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-input v-model="form.image" placeholder="图片URL（可选）" />
        </el-form-item>
        <el-form-item label="所需积分" required>
          <el-input-number v-model="form.pointsCost" :min="1" />
        </el-form-item>
        <el-form-item label="库存" required>
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPointsGoodsList, createPointsGoods, updatePointsGoods, deletePointsGoods } from '@/api/admin'

const goodsList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: null, name: '', image: '', pointsCost: 100, stock: 0, description: '', status: 1 })

async function load() {
  try {
    const data = await getPointsGoodsList(pageNum.value, pageSize.value)
    goodsList.value = data.records || []
    total.value = data.total || 0
  } catch {
    goodsList.value = []
  }
}

function openCreate() {
  isEdit.value = false
  form.value = { id: null, name: '', image: '', pointsCost: 100, stock: 0, description: '', status: 1 }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

async function save() {
  if (!form.value.name || !form.value.pointsCost) {
    ElMessage.warning('请填写商品名称与所需积分')
    return
  }
  try {
    if (isEdit.value) {
      await updatePointsGoods(form.value.id, form.value)
    } else {
      await createPointsGoods(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除兑换商品「${row.name}」？`, '提示', { type: 'warning' })
    await deletePointsGoods(row.id)
    ElMessage.success('已删除')
    load()
  } catch {}
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
