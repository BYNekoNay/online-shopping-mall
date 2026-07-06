import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const activeCategory = ref(0)

  function setLoading(val) {
    loading.value = val
  }

  function setActiveCategory(id) {
    activeCategory.value = id
  }

  return { loading, activeCategory, setLoading, setActiveCategory }
})
