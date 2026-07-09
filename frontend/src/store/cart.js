import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  async function fetchList() {
    try {
      items.value = await request.get('/cart')
    } catch {
      items.value = []
    }
  }

  function updateItem(id, data) {
    const idx = items.value.findIndex(item => item.id === id)
    if (idx >= 0) {
      items.value[idx] = { ...items.value[idx], ...data }
    }
  }

  function removeItem(id) {
    items.value = items.value.filter(item => item.id !== id)
  }

  function getSelectedTotal() {
    return items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  }

  function clear() {
    items.value = []
  }

  return { items, fetchList, updateItem, removeItem, getSelectedTotal, clear }
})
