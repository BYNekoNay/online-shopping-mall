/**
 * 分类图标工具：根据分类名生成确定性 emoji 图标。
 *
 * 背景：原首页分类卡片用 name.charAt(0) 首字当图标，观感单薄。
 * 本工具按关键词映射常见分类到 emoji，未命中时用名称哈希从兜底集合取一个，
 * 保证同一分类名始终得到同一图标（确定性、无额外依赖）。
 */

const EMOJI_RULES = [
  { keys: ['手机', '数码', '电子', '电脑', '智能', '办公'], emoji: '📱' },
  { keys: ['服装', '男装', '女装', '鞋', '服饰', '内衣', '童装'], emoji: '👕' },
  { keys: ['食品', '零食', '生鲜', '饮料', '酒', '粮油', '水果'], emoji: '🍎' },
  { keys: ['家居', '家装', '家具', '厨具', '日用', '家纺', '收纳'], emoji: '🏠' },
  { keys: ['美妆', '护肤', '彩妆', '香水', '个护'], emoji: '💄' },
  { keys: ['运动', '健身', '户外', '骑行', '瑜伽'], emoji: '⚽' },
  { keys: ['图书', '书', '教育', '文具', '文娱'], emoji: '📚' },
  { keys: ['母婴', '玩具', '婴儿', '孕产'], emoji: '🧸' },
  { keys: ['珠宝', '饰品', '手表', '眼镜', '配饰'], emoji: '💎' },
  { keys: ['汽车', '车品', '摩托', '车载'], emoji: '🚗' },
  { keys: ['家电', '冰箱', '电视', '空调', '洗衣机', '厨房电器'], emoji: '📺' },
  { keys: ['宠物', '猫', '狗'], emoji: '🐱' },
  { keys: ['医药', '健康', '保健', '医疗器械'], emoji: '💊' },
  { keys: ['礼品', '鲜花', '节庆'], emoji: '🎁' }
]

const FALLBACK_EMOJI = ['🛍️', '📦', '✨', '🎁', '🏷️', '🛒']

/**
 * 根据分类名获取确定性 emoji。
 * @param {string} name 分类名称
 * @returns {string} emoji 字符
 */
export function categoryEmoji(name = '') {
  const n = String(name || '').trim()
  for (const rule of EMOJI_RULES) {
    if (rule.keys.some((key) => n.includes(key))) return rule.emoji
  }
  // 未命中：名称哈希取模，保证同名称同图标
  let hash = 0
  for (let i = 0; i < n.length; i++) {
    hash = (hash * 31 + n.charCodeAt(i)) >>> 0
  }
  return FALLBACK_EMOJI[hash % FALLBACK_EMOJI.length]
}
