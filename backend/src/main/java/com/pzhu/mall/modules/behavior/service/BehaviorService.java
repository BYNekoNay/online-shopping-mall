package com.pzhu.mall.modules.behavior.service;

import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.PageLeaveDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendClickDTO;
import com.pzhu.mall.modules.behavior.dto.RecommendExposureDTO;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 记录用户行为（浏览/收藏/购买/评价）。
     *
     * @param userId       用户ID，未登录传 null
     * @param productId    商品ID
     * @param behaviorType 1=浏览, 2=收藏, 3=购买, 4=评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, Long productId, Integer behaviorType) {
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setProductId(productId);
        ub.setBehaviorType(behaviorType);
        BigDecimal weight = switch (behaviorType) {
            case 2 -> new BigDecimal("2.00");
            case 3 -> new BigDecimal("3.00");
            case 4 -> new BigDecimal("4.00");
            default -> new BigDecimal("1.00");
        };
        ub.setBehaviorWeight(weight);
        userBehaviorMapper.insert(ub);
    }

    /**
     * 记录推荐位曝光（轻量日志，不入 user_behavior，避免污染算法评分矩阵）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordRecommendExposure(RecommendExposureDTO dto) {
        log.info("[推荐-曝光] 用户={} 来源={} 商品数={}",
                dto.getUserId(), dto.getSource(), dto.getProductIds() != null ? dto.getProductIds().size() : 0);
        // 预留：后续可在 recommend_exposure_log 表持久化，供离线 CTR 评估使用
        // 当前仅打日志，避免引入新表导致 DDL 变更
    }

    /**
     * 记录推荐位点击（记为浏览行为 type=1，写入 user_behavior 供算法使用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordRecommendClick(RecommendClickDTO dto) {
        log.info("[推荐-点击] 用户={} 来源={} 商品={} 位置={}",
                dto.getUserId(), dto.getSource(), dto.getProductId(), dto.getPosition());
        // 点击记为浏览行为（type=1），纳入评分矩阵
        record(dto.getUserId(), dto.getProductId(), 1);
    }

    /**
     * 记录页面进入。
     */
    public Long recordPageEnter(PageViewDTO dto) {
        PageViewLog log = new PageViewLog();
        log.setUserId(dto.getUserId());
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
        if (log != null) {
            log.setLeaveTime(LocalDateTime.now());
            log.setStayDuration(stayDuration);
            pageViewLogMapper.updateById(log);
        }
    }
}
