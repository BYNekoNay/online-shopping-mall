import { defineConfig } from '@playwright/test'

/**
 * Playwright E2E 全局配置（T01/T03/T04）。
 *
 * 约定：
 *  - testDir 指向 ./e2e/tests（全部 E2E 用例）；
 *  - baseURL 默认线上测试环境 http://8.160.181.12，可用环境变量 E2E_BASE_URL 覆盖；
 *  - 不配置 webServer：被测系统由 Nginx 反代持续运行（/api → 8080），E2E 直接打线上环境；
 *  - 失败保留 trace/screenshot/video，便于定位；
 *  - retries=1：线上偶发网络抖动容忍一次重试；workers=4 与唯一时间戳账号配合保证隔离。
 */
const BASE_URL = process.env.E2E_BASE_URL || 'http://8.160.181.12'

export default defineConfig({
  testDir: './tests',
  timeout: 120_000,
  expect: {
    timeout: 30_000,
  },
  fullyParallel: false,
  retries: 1,
  workers: 4,
  outputDir: 'C:/Users/lby0403/AppData/Local/Temp/mall-e2e-results',
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'C:/Users/lby0403/AppData/Local/Temp/mall-e2e-report' }],
  ],
  use: {
    baseURL: BASE_URL,
    headless: true,
    viewport: { width: 1440, height: 900 },
    locale: 'zh-CN',
    // video/trace 关闭：retain-on-failure 会在用例通过后删除录制产物，
    // 本机 sandbox safe-delete 会卡住 worker 退出（实测 300s 超时）。
    // screenshot 保留失败截图用于诊断（仅在失败时写入，不影响通过用例退出）。
    trace: 'off',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
})
