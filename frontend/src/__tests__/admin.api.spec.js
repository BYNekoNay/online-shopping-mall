/**
 * F-T09~10 admin API 契约测试（批次4）。
 *
 * 验证管理端接口路径/方法/参数与后端 Admin*Controller 映射一致，
 * 重点回归 AD-01 角色分配接口（updateUserRole → PUT /admin/users/{id}/role）。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// vi.mock factory 会被提升，mock 函数必须用 vi.hoisted 声明（避免 TDZ）
const { getMock, putMock, postMock, deleteMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  putMock: vi.fn(),
  postMock: vi.fn(),
  deleteMock: vi.fn()
}))

vi.mock('@/utils/request', () => ({
  default: { get: getMock, put: putMock, post: postMock, delete: deleteMock }
}))

import {
  getUsers,
  updateUserStatus,
  updateUserRole,
  auditProduct,
  offlineProduct,
  auditShop,
  updateShopLevel
} from '@/api/admin'

describe('admin API 契约（F-T09~10）', () => {
  beforeEach(() => vi.clearAllMocks())

  it('F-T09 updateUserRole 映射 PUT /admin/users/{id}/role（AD-01 角色分配）', async () => {
    putMock.mockResolvedValue({ code: 0 })
    await updateUserRole(100, { role: 2 })

    expect(putMock).toHaveBeenCalledWith('/admin/users/100/role', { role: 2 })
  })

  it('F-T10 管理端核心接口路径/方法一致', async () => {
    getMock.mockResolvedValue({ code: 0 })
    putMock.mockResolvedValue({ code: 0 })

    await getUsers({ pageNum: 1 })
    expect(getMock).toHaveBeenCalledWith('/admin/users', { params: { pageNum: 1 } })

    await updateUserStatus(100, { status: 0 })
    expect(putMock).toHaveBeenCalledWith('/admin/users/100/status', { status: 0 })

    await auditProduct(50, { approved: true })
    expect(putMock).toHaveBeenCalledWith('/admin/products/50/audit', { approved: true })

    await offlineProduct(50)
    expect(putMock).toHaveBeenCalledWith('/admin/products/50/offline')

    await auditShop(8, { approved: true })
    expect(putMock).toHaveBeenCalledWith('/admin/shops/8/audit', { approved: true })

    await updateShopLevel(8, { level: 2 })
    expect(putMock).toHaveBeenCalledWith('/admin/shops/8/level', { level: 2 })
  })
})
