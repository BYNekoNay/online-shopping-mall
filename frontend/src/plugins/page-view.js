/**
 * 页面访问埋点插件。
 *
 * 功能：
 * 1. 路由切换时记录页面进入（enter_time），上报 /api/behavior/page-view，拿回记录 id；
 * 2. 页面卸载 / 路由离开时回填 leave_time 与停留时长，使用 navigator.sendBeacon 保证
 *    浏览器标签关闭时请求仍可靠发出；
 * 3. 相同路由重复进入（组件复用）不重复上报，仅上报一次。
 */
const PAGE_VIEW_SESSION_KEY = 'page_view_session_id'

function getOrCreateSessionId() {
  let sid = localStorage.getItem(PAGE_VIEW_SESSION_KEY)
  if (!sid) {
    sid = `session_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
    localStorage.setItem(PAGE_VIEW_SESSION_KEY, sid)
  }
  return sid
}

// 路由跳转时上报页面进入
function reportPageEnter(to, requestFn) {
  const reportedKey = `pv_reported:${to.fullPath}`
  if (sessionStorage.getItem(reportedKey)) return

  const userId = localStorage.getItem('userId') || null
  const enterTime = new Date().toISOString()

  requestFn({
    userId: userId ? Number(userId) : null,
    sessionId: getOrCreateSessionId(),
    pagePath: to.fullPath,
    referrerPage: sessionStorage.getItem('last_page_path') || '',
  }).then((id) => {
    // 存储当前页面信息，供 leave 使用
    sessionStorage.setItem('current_page_view_id', String(id))
    sessionStorage.setItem('current_page_path', to.fullPath)
    sessionStorage.setItem('current_page_enter_time', enterTime)
  }).catch(() => {
    // 进入上报失败不影响业务，静默忽略
  }).finally(() => {
    sessionStorage.setItem(reportedKey, '1')
  })
}

// 页面离开时回填
function reportPageLeave(requestFn) {
  const idStr = sessionStorage.getItem('current_page_view_id')
  if (!idStr) return

  const id = Number(idStr)
  if (!id) return

  const enterStr = sessionStorage.getItem('current_page_enter_time')
  let stayDuration = null
  if (enterStr) {
    const enterMs = new Date(enterStr).getTime()
    stayDuration = Math.round((Date.now() - enterMs) / 1000)
  }

  const payload = JSON.stringify({ stayDuration })

  // sendBeacon 用于标签关闭等卸载场景，请求会异步发出，不阻塞页面关闭
  const url = `/api/behavior/page-view/${id}/leave`
  const sent = navigator.sendBeacon && navigator.sendBeacon(url, payload)

  if (!sent) {
    // 降级：使用 fetch keepalive（部分浏览器支持）或普通 POST（页面关闭时可能被取消）
    fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: payload,
      keepalive: true,
    }).catch(() => {})
  }

  // 清理当前页数据，为下次进入做准备
  sessionStorage.removeItem('current_page_view_id')
  sessionStorage.removeItem('current_page_path')
  sessionStorage.removeItem('current_page_enter_time')
}

/**
 * 安装插件，传入 axios 封装的 request 方法（避免直接依赖路由对象导致测试耦合）。
 *
 * @param {import('vue-router').Router} router
 * @param {import('axios').AxiosInstance|Function} requestFn
 */
export default function pageViewPlugin(router, requestFn) {
  // 路由切换时记录来源页面，供下次进入的 referrerPage 使用
  router.beforeEach((_to, from) => {
    if (from.fullPath) {
      sessionStorage.setItem('last_page_path', from.fullPath)
    }
  })

  // 路由切换完成时上报页面进入
  router.afterEach((to) => {
    // 跳过 403 等非业务页面，避免噪音数据
    if (to.path === '/403' || to.path === '/login' || to.path === '/register') return
    reportPageEnter(to, requestFn)
  })

  // 页面卸载 / 标签关闭时回填离开信息
  window.addEventListener('beforeunload', () => {
    reportPageLeave(requestFn)
  })

  // 移动端微信/支付宝浏览器切换 tab 也会触发 pagehide，一并处理
  window.addEventListener('pagehide', () => {
    reportPageLeave(requestFn)
  })
}
