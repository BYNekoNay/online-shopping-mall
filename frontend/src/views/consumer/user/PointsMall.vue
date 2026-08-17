<template>
  <div class="points-mall">
    <el-card>
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
        <h3 style="margin: 0">积分商城</h3>
        <div>
          <el-tag type="warning" size="large">我的积分：{{ userStore.userId ? myPoints : '-' }}</el-tag>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="可兑换商品" name="goods">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="8" :md="6" v-for="g in goodsList" :key="g.id" style="margin-bottom: 16px">
              <el-card :body-style="{ padding: '12px' }">
                <div
                  style="
                    height: 120px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: linear-gradient(180deg, #f8fafc, #eef2f7);
                    border-radius: 8px;
                    overflow: hidden;
                  "
                >
                  <img
                    v-if="g.image"
                    :src="resolvedGoodsImage(g)"
                    :data-seed="g.id"
                    alt="商品图"
                    loading="lazy"
                    style="max-width: 100%; max-height: 100%; object-fit: cover"
                    @error="handleImgError"
                  />
                  <span v-else style="color: #999">无图片</span>
                </div>
                <div style="margin-top: 10px; font-weight: 500; font-size: 14px">{{ g.name }}</div>
                <div style="color: #999; font-size: 12px; margin-top: 4px">{{ g.description || '暂无描述' }}</div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px">
                  <span style="color: #e6a23c; font-weight: bold">{{ g.pointsCost }} 积分</span>
                  <el-tag size="small" :type="g.stock > 0 ? 'success' : 'danger'">库存 {{ g.stock }}</el-tag>
                </div>
                <el-button
                  type="warning"
                  style="width: 100%; margin-top: 10px"
                  :disabled="g.stock <= 0"
                  @click="exchange(g)"
                  >兑换</el-button
                >
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="goodsList.length === 0" description="暂无可兑换商品" />
        </el-tab-pane>

        <el-tab-pane label="兑换记录" name="logs">
          <el-table :data="logs" style="width: 100%" empty-text="暂无兑换记录">
            <el-table-column prop="goodsName" label="商品" />
            <el-table-column prop="pointsCost" label="消耗积分" width="120" />
            <el-table-column prop="createTime" label="兑换时间" width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPointsGoods, exchangeGoods, getExchangeLogs } from '@/api/points'
// FRONT-09 修复：积分余额接口在 api/coupon.js 导出（getPoints），不在 api/points.js
import { getPoints } from '@/api/coupon'
import { useUserStore } from '@/store/user'
import { resolveImg, buildFallbackUrl } from '@/utils/image'

const userStore = useUserStore()
const activeTab = ref('goods')
const goodsList = ref([])
const logs = ref([])
const myPoints = ref(0)

/** 积分商品图兜底。 */
function resolvedGoodsImage(g) {
  return resolveImg(g.image || '', g.id ?? g.name ?? 'default', 240, 240)
}

function handleImgError(event) {
  const img = event && event.target
  if (!img || !img.tagName || img.tagName.toLowerCase() !== 'img') return
  const fallback = buildFallbackUrl(img.getAttribute('data-seed') || 'default', 240, 240)
  if (img.getAttribute('src') === fallback) return
  img.setAttribute('src', fallback)
}

async function loadGoods() {
  try {
    const data = await getPointsGoods(1, 12)
    goodsList.value = data.records || []
  } catch {
    goodsList.value = []
  }
}

async function loadLogs() {
  try {
    logs.value = await getExchangeLogs(20)
  } catch {
    logs.value = []
  }
}

// FRONT-09 修复：加载当前用户积分余额（此前 myPoints 从未赋值，恒显示 0）
async function loadMyPoints() {
  try {
    const data = await getPoints()
    myPoints.value = (data && data.points) || 0
  } catch {
    myPoints.value = 0
  }
}

async function exchange(goods) {
  try {
    await ElMessageBox.confirm(`确认用 ${goods.pointsCost} 积分兑换「${goods.name}」？`, '积分兑换', {
      type: 'warning'
    })
    await exchangeGoods({ goodsId: goods.id, quantity: 1 })
    ElMessage.success('兑换成功')
    loadGoods()
    loadLogs()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('兑换失败：积分不足或库存不足')
  }
}

onMounted(() => {
  loadGoods()
  loadLogs()
  // FRONT-09 修复：加载积分余额（此前恒显示 0）
  loadMyPoints()
})
</script>
