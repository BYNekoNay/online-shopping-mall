/**
 * v-lazy-img 图片懒加载 + 兜底指令。
 *
 * 用法：
 *   <img v-lazy-img :data-src="item.mainImage" :data-seed="item.id" :alt="item.name" />
 *
 * 行为：
 * 1. 进入视口（含 200px 预加载区）前不设置 src，避免占位图/坏图直显；
 * 2. 进入视口后设置 data-src（原始地址）或 data-seed 对应的 picsum 兜底图；
 * 3. 图片加载失败（onerror）自动切换到 picsum 兜底图；
 * 4. 不支持 IntersectionObserver 的环境直接加载（退化为普通 img）。
 */

import { isPlaceholderUrl, buildFallbackUrl } from '@/utils/image'

function getSeed(el) {
  const seed = el.getAttribute('data-seed')
  if (seed) return seed
  const dataSrc = el.getAttribute('data-src') || ''
  const match = String(dataSrc).match(/\/(\d+)(?:[._-]|$)/)
  if (match) return match[1]
  return 'default'
}

function applyUrl(el, url) {
  el.setAttribute('src', url)
  el.removeAttribute('data-src')
}

function bindErrorFallback(el) {
  const onError = () => {
    const fallback = buildFallbackUrl(getSeed(el), 400, 400)
    if (el.getAttribute('src') === fallback) return
    applyUrl(el, fallback)
  }
  el.addEventListener('error', onError)
  el._lazyImgCleanup = () => el.removeEventListener('error', onError)
}

function load(el) {
  const dataSrc = el.getAttribute('data-src')
  if (isPlaceholderUrl(dataSrc)) {
    applyUrl(el, buildFallbackUrl(getSeed(el), 400, 400))
  } else {
    applyUrl(el, dataSrc)
  }
}

export default {
  mounted(el) {
    el.setAttribute('loading', 'lazy')
    // 先清掉可能存在的直显 src（占位/坏图），交由指令统一加载
    if (!el.getAttribute('data-src')) {
      const currentSrc = el.getAttribute('src')
      if (currentSrc) el.setAttribute('data-src', currentSrc)
      el.removeAttribute('src')
    }
    bindErrorFallback(el)

    if (typeof window !== 'undefined' && typeof IntersectionObserver !== 'undefined') {
      el._lazyImgObserver = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              load(el)
              if (el._lazyImgObserver) {
                el._lazyImgObserver.disconnect()
                el._lazyImgObserver = null
              }
            }
          })
        },
        { rootMargin: '200px 0px' }
      )
      el._lazyImgObserver.observe(el)
    } else {
      // 不支持 IntersectionObserver：直接加载（行为退化但功能完整）
      load(el)
    }
  },
  unmounted(el) {
    if (el._lazyImgObserver) {
      el._lazyImgObserver.disconnect()
      el._lazyImgObserver = null
    }
    if (el._lazyImgCleanup) {
      el._lazyImgCleanup()
      el._lazyImgCleanup = null
    }
  }
}
