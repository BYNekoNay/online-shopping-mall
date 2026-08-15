package com.pzhu.mall.modules.behavior.service;

import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.common.config.RedisKeyPrefix;
import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.PageLeaveDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendClickDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import com.pzhu.mall.security.LoginUserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 用户行为服务。
 */
@Service
public class BehaviorService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorService.class);

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private PageViewLogMapper pageViewLogMapper;

    @Resource
    private com.pzhu.mall.modules.behavior.mapper.RecommendExposureLogMapper recommendExposureLogMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private com.pzhu.mall.modules.recommend.service.RecommendCalculateService recommendCalculateService;

    /**
     * 记录用户行为（浏览/收藏/购买/评价）。
     *
     * @param userId       用户ID，未登录传 null
     * @param productId    商品ID
     * @param behaviorType 1=浏览, 2=收藏, 3=购买, 4=评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, Long productId, Integer behaviorType) {
        // F-03 修复：收藏（type=2）去重——同用户同商品已收藏则跳过，避免收藏列表重复
        if (behaviorType != null && behaviorType == 2 && userId != null && productId != null) {
            Long existing = userBehaviorMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserBehavior>()
                            .eq(UserBehavior::getUserId, userId)
                            .eq(UserBehavior::getProductId, productId)
                            .eq(UserBehavior::getBehaviorType, 2)
            );
            if (existing != null && existing > 0) {
                log.debug("[行为] 用户={} 已收藏商品={}，跳过重复收藏", userId, productId);
                return;
            }
        }
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setProductId(productId);
        // NP_NULL_ON_SOME_PATH 修复：behaviorType 为 null 时 switch 会 NPE，统一按浏览(1)兜底
        if (behaviorType == null) {
            behaviorType = 1;
        }
        ub.setBehaviorType(behaviorType);
        // M-10 修复：存储权重与推荐算法 RecommendCalculateService.getWeight() 口径对齐
        // （浏览=1.0 / 收藏=3.0 / 购买=5.0 / 评价=4.0），避免 DB 存储值与算法实际权重不一致
        BigDecimal weight = switch (behaviorType) {
            case 2 -> new BigDecimal("3.00");  // 收藏
            case 3 -> new BigDecimal("5.00");  // 购买
            case 4 -> new BigDecimal("4.00");  // 评价
            default -> new BigDecimal("1.00"); // 浏览
        };
        ub.setBehaviorWeight(weight);
        userBehaviorMapper.insert(ub);

        // L2-01 修复：购买行为后准实时刷新该用户推荐（创新点④"准实时"接线）。
        // 异步执行 + Redis 节流（同用户 10 分钟内最多触发一次），避免高频重算；
        // 失败不影响主流程（best-effort），行为已落库，定时任务仍会兜底。
        // SB-01 修复：behaviorType 已在上方 null 兜底（=1），此处去掉冗余空检
        if (behaviorType == 3 && userId != null) {
            triggerAsyncRecommendRefresh(userId);
        }
    }

    /**
     * L2-01 修复：异步触发单用户推荐刷新（Redis 节流：同用户 10 分钟窗口）。
     */
    private void triggerAsyncRecommendRefresh(Long userId) {
        try {
            // 节流：SET NX + TTL 10 分钟，成功才提交异步任务
            String throttleKey = RedisKeyPrefix.RECOMMEND + ":refresh:" + userId;
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(throttleKey, "1", Duration.ofMinutes(10));
            if (Boolean.TRUE.equals(acquired)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        recommendCalculateService.calculateForUser(userId);
                    } catch (Exception e) {
                        log.warn("[推荐-准实时] 用户={} 异步刷新失败（定时任务兜底）", userId, e);
                    }
                });
                log.info("[推荐-准实时] 用户={} 已提交异步推荐刷新", userId);
            } else {
                log.debug("[推荐-准实时] 用户={} 10分钟内已刷新过，跳过", userId);
            }
        } catch (Exception e) {
            // Redis 不可用等场景降级：本次不刷新，靠定时任务兜底
            log.warn("[推荐-准实时] 用户={} 提交刷新失败，降级跳过", userId, e);
        }
    }

    /**
     * 记录推荐位曝光（落库 recommend_exposure_log，clicked=0；不入 user_behavior，避免污染算法评分矩阵）。
     * <p>BE-02 修复：原实现仅打日志不落库，导致 CTR 统计（任务书 7.6）依赖预生成推荐数近似。
     * 现逐商品落库一条曝光记录，点击时回标 clicked=1，CTR = 点击数/曝光数。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordRecommendExposure(RecommendExposureDTO dto) {
        Long userId = dto.getUserId();
        String source = dto.getSource() != null ? dto.getSource() : "home-guess";
        List<Long> productIds = dto.getProductIds();
        log.info("[推荐-曝光] 用户={} 来源={} 商品数={}",
                userId, source, productIds != null ? productIds.size() : 0);
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long productId : productIds) {
            if (productId == null) {
                continue;
            }
            com.pzhu.mall.modules.behavior.entity.RecommendExposureLog record =
                    new com.pzhu.mall.modules.behavior.entity.RecommendExposureLog();
            record.setUserId(userId);
            record.setSource(source);
            record.setProductId(productId);
            record.setClicked(0);
            record.setCreateTime(now);
            try {
                recommendExposureLogMapper.insert(record);
            } catch (Exception e) {
                // 曝光埋点为 best-effort，失败不影响主流程
                log.warn("[推荐-曝光] 落库失败 productId={}", productId, e);
            }
        }
    }

    /**
     * 记录推荐位点击（记为浏览行为 type=1 入 user_behavior；并将对应曝光标记 clicked=1）。
     * <p>BE-02 修复：点击回标最近一条未点击的曝光记录（同用户+同商品+同来源），
     * 无曝光记录则插入一条 clicked=1（点击未曝光过的商品按已点击计），保证 CTR 分母=曝光数、分子=点击数。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordRecommendClick(RecommendClickDTO dto) {
        log.info("[推荐-点击] 用户={} 来源={} 商品={} 位置={}",
                dto.getUserId(), dto.getSource(), dto.getProductId(), dto.getPosition());
        // 点击记为浏览行为（type=1），纳入评分矩阵
        record(dto.getUserId(), dto.getProductId(), 1);

        // 回标曝光：同用户+同商品+同来源 最近一条 clicked=0 → clicked=1
        Long userId = dto.getUserId();
        Long productId = dto.getProductId();
        String source = dto.getSource() != null ? dto.getSource() : "home-guess";
        if (userId != null && productId != null) {
            int updated = recommendExposureLogMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.pzhu.mall.modules.behavior.entity.RecommendExposureLog>()
                            .set(com.pzhu.mall.modules.behavior.entity.RecommendExposureLog::getClicked, 1)
                            .eq(com.pzhu.mall.modules.behavior.entity.RecommendExposureLog::getUserId, userId)
                            .eq(com.pzhu.mall.modules.behavior.entity.RecommendExposureLog::getProductId, productId)
                            .eq(com.pzhu.mall.modules.behavior.entity.RecommendExposureLog::getSource, source)
                            .eq(com.pzhu.mall.modules.behavior.entity.RecommendExposureLog::getClicked, 0)
                            .last("LIMIT 1")
            );
            if (updated == 0) {
                // 无未点击曝光记录：插入一条 clicked=1（视为已点击）
                com.pzhu.mall.modules.behavior.entity.RecommendExposureLog record =
                        new com.pzhu.mall.modules.behavior.entity.RecommendExposureLog();
                record.setUserId(userId);
                record.setSource(source);
                record.setProductId(productId);
                record.setClicked(1);
                record.setCreateTime(LocalDateTime.now());
                try {
                    recommendExposureLogMapper.insert(record);
                } catch (Exception e) {
                    log.warn("[推荐-点击] 曝光日志插入失败 productId={}", productId, e);
                }
            }
        }
    }

    /**
     * 记录页面进入。
     */
    public Long recordPageEnter(PageViewDTO dto) {
        PageViewLog log = new PageViewLog();
        // M-26 修复：userId 只取登录态，不信任前端传入值，防止伪造他人页面访问日志（与 M-11 口径一致）
        log.setUserId(LoginUserContext.getCurrentUserId());
        log.setSessionId(dto.getSessionId());
        log.setPagePath(dto.getPagePath());
        log.setReferrerPage(dto.getReferrerPage());
        log.setEnterTime(LocalDateTime.now());
        pageViewLogMapper.insert(log);
        return log.getId();
    }

    /**
     * 回填页面离开信息。
     */
    public void recordPageLeave(Long id, Integer stayDuration) {
        PageViewLog log = pageViewLogMapper.selectById(id);
        if (log == null) {
            return;
        }
        // M-27 修复：校验日志归属，防止越权回填他人页面停留数据
        Long currentUserId = LoginUserContext.getCurrentUserId();
        boolean owned = currentUserId != null
                ? currentUserId.equals(log.getUserId())
                : log.getUserId() == null;
        if (!owned) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // M-27 修复：停留时长封顶（负数归零，上限 24h=86400 秒），防止脏数据/整型溢出
        int capped = stayDuration == null ? 0 : Math.min(Math.max(stayDuration, 0), 86400);
        log.setLeaveTime(LocalDateTime.now());
        log.setStayDuration(capped);
        pageViewLogMapper.updateById(log);
    }
}
