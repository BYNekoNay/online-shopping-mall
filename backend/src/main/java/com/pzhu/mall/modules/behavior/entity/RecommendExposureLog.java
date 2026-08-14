package com.pzhu.mall.modules.behavior.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 推荐位曝光/点击日志（BE-02）。
 * <p>用于 CTR 真实统计：曝光上报插入 clicked=0 记录，点击上报将对应曝光标记 clicked=1；
 * CTR = 点击数 / 曝光数（任务书 7.6"推荐算法效果（点击率）"）。</p>
 */
@TableName("recommend_exposure_log")
public class RecommendExposureLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String source;
    private Long productId;
    private Integer clicked;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getClicked() { return clicked; }
    public void setClicked(Integer clicked) { this.clicked = clicked; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
