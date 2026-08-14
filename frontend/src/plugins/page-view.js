/**
 * 页面访问埋点插件。
 *
 * 功能：
 * 1. 路由切换时记录页面进入（enter_time），上报 /api/behavior/page-view，拿回记录 id；
 * 2. 页面卸载 / 路由离开时回填 leave_time 与停留时长，使用 fetch + keepalive 保证
 *    浏览器标签关闭时请求仍可靠发出；SPA 站内路由跳转时同样回填上一页离开（M-29 修复）；
 * 3. 仅对"当前活跃页面"的重复触发去重（组件复用/重复导航），离开后再次进入同一页面
 *    正常上报（M-28 修复：原会话级去重会漏报合法的重访）。
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
  // M-28 修复：去重口径从"会话内同一 fullPath 不重复上报"改为"仅当前活跃页面不重复上报"。
  // 原实现的 pv_reported:* 标志写入 sessionStorage 且从不清除，导致用户离开后
  // 再次回到同一页面（如 首页→商品→首页）时第二次访问被永久漏报；
  // current_page_path 在离开上报时即被清除，离开后再进入同一页面可正常上报，
  // 仅对同页重复触发（组件复用、重复导航）去重。
  if (sessionStorage.getItem('current_page_path') === to.fullPath) return

  // F-06 修复：userId 由后端强制取登录态（M-26），前端不再传参
  const enterTime = new Date().toISOString()

  requestFn({
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
  })
}

// 页面离开时回填
function reportPageLeave(_requestFn) {
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
  const url = `/api/behavior/page-view/${id}/leave`

  // H-19 修复：leave 端点需要鉴权，原实现未带 Authorization 导致登录用户回填恒失败
  const headers = { 'Content-Type': 'application/json' }
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    if (user && user.token) {
      headers.Authorization = `Bearer ${user.token}`
    }
  } catch {
    // user 数据损坏时忽略，按匿名请求处理
  }

  // sendBeacon 仅支持 POST，端点定义为 PUT，统一使用 fetch + keepalive
  fetch(url, {
    method: 'PUT',
    headers,
    body: payload,
    keepalive: true,
  }).catch(() => {})

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
    // M-29 修复：SPA 站内路由跳转时先回填上一页的离开信息（停留时长）。
    // 原实现仅在标签关闭时回填 leave，站内跳转的上一页 stayDuration 永远缺失；
    // reportPageLeave 读取并清理的是"上一页面"的数据，必须先于新页面 enter 调用
    reportPageLeave(requestFn)
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
