/**
 * F-3 表单校验统一工具
 * 生成 Element Plus el-form rules 的通用规则集，消除各页面重复手写校验。
 */
import { ElMessage } from 'element-plus'

/** 通用校验规则生成器（Element Plus rules 数组项） */
export const rules = {
  /** 必填 */
  required(msg = '此项为必填项') {
    return { required: true, message: msg, trigger: 'blur' }
  },
  /** 必填（选择器/下拉用 change 触发） */
  requiredSelect(msg = '请选择') {
    return { required: true, message: msg, trigger: 'change' }
  },
  /** 用户名：3~20 位字母数字下划线 */
  username(msg = '用户名需为 3~20 位字母、数字或下划线') {
    return {
      pattern: /^[a-zA-Z0-9_]{3,20}$/,
      message: msg,
      trigger: 'blur'
    }
  },
  /** 手机号 */
  phone(msg = '请输入正确的 11 位手机号') {
    return {
      pattern: /^1[3-9]\d{9}$/,
      message: msg,
      trigger: 'blur'
    }
  },
  /** 邮箱 */
  email(msg = '请输入正确的邮箱地址') {
    return {
      type: 'email',
      message: msg,
      trigger: 'blur'
    }
  },
  /** 长度范围 */
  length(min, max, msg = `长度需在 ${min}~${max} 个字符之间`) {
    return { min, max, message: msg, trigger: 'blur' }
  },
  /** 正整数（数量/库存） */
  positiveInt(msg = '请输入正整数') {
    return {
      pattern: /^[1-9]\d*$/,
      message: msg,
      trigger: 'change'
    }
  },
  /** 非负数字（价格等，最多两位小数） */
  nonNegativeNumber(msg = '请输入正确的非负数字') {
    return {
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || value === '') {
          callback()
          return
        }
        const n = Number(value)
        if (Number.isNaN(n) || n < 0) {
          callback(new Error(msg))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  },
  /** 两次密码一致 */
  confirmPassword(getPassword) {
    return {
      validator: (_rule, value, callback) => {
        if (value !== getPassword()) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  }
}

/** 表单校验快捷方法：validate 通过返回 true，否则提示并返回 false */
export async function validateForm(formRef) {
  if (!formRef) return true
  try {
    await formRef.validate()
    return true
  } catch (e) {
    console.warn('[Form] 校验未通过:', e)
    return false
  }
}

/** 简洁提示（供各页面共用） */
export function tip(type, msg) {
  ElMessage[type]?.(msg)
}

export default { rules, validateForm, tip }
