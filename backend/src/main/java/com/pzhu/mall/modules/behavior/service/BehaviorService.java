package com.pzhu.mall.modules.behavior.service;

import com.pzhu.mall.modules.behavior.dto.PageViewDTO;
import com.pzhu.mall.modules.behavior.dto.PageLeaveDTO;
import com.pzhu.mall.modules.behavior.entity.UserBehavior;
import com.pzhu.mall.modules.behavior.mapper.UserBehaviorMapper;
import com.pzhu.mall.modules.behavior.entity.PageViewLog;
import com.pzhu.mall.modules.behavior.mapper.PageViewLogMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 用户行为服务。
 */
@Service
public class BehaviorService {

    @Resource
    private UserBehaviorMapper userBehaviorMapper;

    @Resource
    private PageViewLogMapper pageViewLogMapper;

    /**
     * 记录用户行为（浏览/收藏/购买/评价）。
     */
    public void record(Long userId, Long productId, Integer behaviorType) {
        UserBehavior ub = new UserBehavior();
        ub.setUserId(userId);
        ub.setProductId(productId);
        ub.setBehaviorType(behaviorType);
        ub.setBehaviorWeight(new java.math.BigDecimal("1.00"));
        userBehaviorMapper.insert(ub);
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
