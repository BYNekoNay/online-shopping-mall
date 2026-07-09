<template>
  <div class="user-addresses">
    <el-card>
      <div class="header">
        <h3>收货地址</h3>
        <el-button type="primary" @click="showForm = true; editingId = null; form = { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }">新增地址</el-button>
      </div>
      <el-dialog v-model="showForm" :title="editingId ? '编辑地址' : '新增地址'">
        <el-form :model="form" label-width="100px">
          <el-form-item label="收货人"><el-input v-model="form.receiver" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="省份">
            <el-select v-model="form.province" placeholder="请选择省份">
              <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
          <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
          <el-form-item label="区县"><el-input v-model="form.district" /></el-form-item>
          <el-form-item label="详细地址"><el-input v-model="form.detail" /></el-form-item>
          <el-form-item label="设为默认"><el-switch v-model="form.isDefault" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showForm = false">取消</el-button>
          <el-button type="primary" @click="saveAddress">确定</el-button>
        </template>
      </el-dialog>
      <el-table :data="addresses" style="width: 100%; margin-top: 15px;">
        <el-table-column prop="receiver" label="收货人" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="province" label="省份" />
        <el-table-column prop="city" label="城市" />
        <el-table-column prop="detail" label="详细地址" />
        <el-table-column prop="isDefault" label="默认">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success">是</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="text" @click="editAddress(row)">编辑</el-button>
            <el-button type="text" @click="deleteAddress(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/user'

const addresses = ref([])
const showForm = ref(false)
const editingId = ref(null)
const form = ref({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false })

const provinces = [
  '北京市','天津市','河北省','山西省','内蒙古自治区',
  '辽宁省','吉林省','黑龙江省',
  '上海市','江苏省','浙江省','安徽省','福建省','江西省','山东省',
  '河南省','湖北省','湖南省',
  '广东省','广西壮族自治区','海南省',
  '重庆市','四川省','贵州省','云南省','西藏自治区',
  '陕西省','甘肃省','青海省','宁夏回族自治区','新疆维吾尔自治区',
  '香港特别行政区','澳门特别行政区','台湾省'
]

onMounted(async () => {
  try {
    addresses.value = await request.getAddresses()
  } catch {
    addresses.value = []
  }
})

async function saveAddress() {
  try {
    if (editingId.value) {
      await request.updateAddress(editingId.value, form.value)
      ElMessage.success('已更新')
    } else {
      await request.addAddress(form.value)
      ElMessage.success('已添加')
    }
    showForm.value = false
    addresses.value = await request.getAddresses()
  } catch {
    // error handled
  }
}

function editAddress(row) {
  editingId.value = row.id
  form.value = { ...row }
  showForm.value = true
}

async function deleteAddress(id) {
  await request.deleteAddress(id)
  addresses.value = addresses.value.filter(a => a.id !== id)
  ElMessage.success('删除成功')
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
