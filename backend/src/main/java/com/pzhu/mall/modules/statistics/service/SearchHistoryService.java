package com.pzhu.mall.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pzhu.mall.common.exception.BusinessException;
import com.pzhu.mall.common.enums.ErrorCode;
import com.pzhu.mall.modules.statistics.entity.SearchHistory;
import com.pzhu.mall.modules.statistics.mapper.SearchHistoryMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索历史服务。
 */
@Service
public class SearchHistoryService {

    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    /**
     * 记录搜索关键词。
     */
    public void record(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        SearchHistory history = new SearchHistory();
        history.setUserId(userId != null ? userId : 0L);
        history.setKeyword(keyword.trim());
        history.setCreateTime(LocalDateTime.now());
        searchHistoryMapper.insert(history);
    }

    /**
     * 查询当前用户的搜索历史（最近20条）。
     */
    public List<SearchHistory> listByUser(Long userId) {
        return searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getCreateTime)
                        .last("LIMIT 20")
        );
    }

    /**
     * 清空当前用户的搜索历史。
     */
    public void deleteByUser(Long userId) {
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
        );
    }
}
