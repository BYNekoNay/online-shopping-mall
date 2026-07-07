/**
 * 商品状态枚举（前端常量，与后端 ProductStatus 保持一致）。
 */
export const ProductStatus = {
  OFFLINE: 0,
  ONLINE: 1,
  PENDING: 2,
  REJECTED: 3,
}

export const ProductStatusLabel = {
  [ProductStatus.OFFLINE]: '已下架',
  [ProductStatus.ONLINE]: '已上架',
  [ProductStatus.PENDING]: '待审核',
  [ProductStatus.REJECTED]: '审核拒绝',
}

export const ProductStatusTagType = {
  [ProductStatus.OFFLINE]: 'info',
  [ProductStatus.ONLINE]: 'success',
  [ProductStatus.PENDING]: 'warning',
  [ProductStatus.REJECTED]: 'danger',
}
