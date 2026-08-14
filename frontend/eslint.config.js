// ESLint flat config（ESM，项目 package.json 为 "type": "module"）
import vuePlugin from 'eslint-plugin-vue'

const vueRecommended = vuePlugin.configs['flat/recommended']

export default [
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      'src/mock/**',
      'coverage/**',
      'public/**',
      '*.config.js',
      'vite.config.js',
      'vitest.config.js',
      '.npm-cache-local/**'
    ]
  },
  {
    files: ['**/*.js', '**/*.vue'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        window: 'readonly',
        document: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        navigator: 'readonly',
        console: 'readonly',
        fetch: 'readonly',
        Date: 'readonly',
        Math: 'readonly',
        JSON: 'readonly',
        String: 'readonly',
        Number: 'readonly',
        Array: 'readonly',
        Object: 'readonly',
        Boolean: 'readonly',
        Set: 'readonly',
        Map: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        Promise: 'readonly',
        Symbol: 'readonly',
        RegExp: 'readonly',
        Error: 'readonly',
        process: 'readonly'
      }
    },
    rules: {
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-undef': 'error',
      'no-const-assign': 'error',
      'no-dupe-keys': 'error',
      'no-dupe-args': 'error',
      'no-duplicate-case': 'error',
      'no-unreachable': 'error',
      'no-fallthrough': 'error',
      'no-redeclare': 'error',
      'no-var': 'error',
      'prefer-const': 'warn',
      'no-new-object': 'error',
      'no-array-constructor': 'error',
      'no-new-func': 'error',
      'no-param-reassign': 'off',
      'eqeqeq': ['warn', 'smart'],
      'no-debugger': 'warn',
      'no-alert': 'warn',
      'no-console': 'off'
    }
  },
  // Vue3 推荐规则（flat/recommended 已是数组，直接展开）
  ...(Array.isArray(vueRecommended) ? vueRecommended : [vueRecommended]).map((cfg) => ({
    ...cfg,
    rules: {
      ...(cfg.rules || {}),
      // 按项目实际放宽的规则
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'vue/no-v-html': 'warn',
      'vue/max-attributes-per-line': 'off',
      'vue/html-self-closing': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      // 属性顺序为纯风格偏好（Prettier 不管理该维度），关闭避免噪音；清理列为建议项
      'vue/attributes-order': 'off'
    }
  }))
]
