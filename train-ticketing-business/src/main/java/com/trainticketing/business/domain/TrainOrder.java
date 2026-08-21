package com.trainticketing.business.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>Title: TrainOrder</p>
 * <p>Description: 订单实体，与 train_order 表 1:1 对应；核心模型为区间占用余票，
 * 订单明细通过 depart_index/arrive_index 记录每个座位的占用区间</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class TrainOrder {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 会员id
     */
    private Long memberId;

    /**
     * 每日车次id
     */
    private Long dailyTrainId;

    /**
     * 车次id
     */
    private Long trainId;

    /**
     * 出发站id
     */
    private Long departStationId;

    /**
     * 到达站id
     */
    private Long arriveStationId;

    /**
     * 乘车日期
     */
    private Date runDate;

    /**
     * 状态|枚举[OrderStatusEnum]: 0待支付 1已支付 2已取消 3已退票
     */
    private String status;

    /**
     * 订单总金额（元）
     */
    private BigDecimal totalAmount;

    /**
     * 支付过期时间（下单后10分钟）
     */
    private Date expireTime;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 退款时间
     */
    private Date refundTime;

    /**
     * 下单时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getDailyTrainId() {
        return dailyTrainId;
    }

    public void setDailyTrainId(Long dailyTrainId) {
        this.dailyTrainId = dailyTrainId;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Long getDepartStationId() {
        return departStationId;
    }

    public void setDepartStationId(Long departStationId) {
        this.departStationId = departStationId;
    }

    public Long getArriveStationId() {
        return arriveStationId;
    }

    public void setArriveStationId(Long arriveStationId) {
        this.arriveStationId = arriveStationId;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public Date getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(Date refundTime) {
        this.refundTime = refundTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "TrainOrder{" +
                "id=" + id +
                ", orderNo='" + orderNo + '\'' +
                ", memberId=" + memberId +
                ", dailyTrainId=" + dailyTrainId +
                ", trainId=" + trainId +
                ", departStationId=" + departStationId +
                ", arriveStationId=" + arriveStationId +
                ", runDate=" + runDate +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", expireTime=" + expireTime +
                ", payTime=" + payTime +
                ", refundTime=" + refundTime +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
