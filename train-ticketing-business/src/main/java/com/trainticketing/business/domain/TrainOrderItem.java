package com.trainticketing.business.domain;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>Title: TrainOrderItem</p>
 * <p>Description: 订单明细实体，与 train_order_item 表 1:1 对应；
 * 一个乘车人一张票，depart_index/arrive_index 记录该座位占用的区间（余票模型核心）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainOrderItem {

    /** 主键ID */
    private Long id;

    /** 订单id */
    private Long orderId;

    /** 乘车人id */
    private Long passengerId;

    /** 乘车人姓名（下单时快照） */
    private String passengerName;

    /** 身份证号（下单时快照） */
    private String idCard;

    /** 当日座位id */
    private Long dailyTrainSeatId;

    /** 座位类型|枚举[SeatTypeEnum] */
    private String seatType;

    /** 票价（元） */
    private BigDecimal price;

    /** 出发站序（区间占用起点） */
    private Integer departIndex;

    /** 到达站序（区间占用终点） */
    private Integer arriveIndex;

    /** 新增时间 */
    private Date createTime;

    /** 修改时间 */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Long getDailyTrainSeatId() {
        return dailyTrainSeatId;
    }

    public void setDailyTrainSeatId(Long dailyTrainSeatId) {
        this.dailyTrainSeatId = dailyTrainSeatId;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getDepartIndex() {
        return departIndex;
    }

    public void setDepartIndex(Integer departIndex) {
        this.departIndex = departIndex;
    }

    public Integer getArriveIndex() {
        return arriveIndex;
    }

    public void setArriveIndex(Integer arriveIndex) {
        this.arriveIndex = arriveIndex;
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
        return "TrainOrderItem{" +
            "id=" + id +
            ", orderId=" + orderId +
            ", passengerId=" + passengerId +
            ", passengerName='" + passengerName + '\'' +
            ", idCard='" + idCard + '\'' +
            ", dailyTrainSeatId=" + dailyTrainSeatId +
            ", seatType='" + seatType + '\'' +
            ", price=" + price +
            ", departIndex=" + departIndex +
            ", arriveIndex=" + arriveIndex +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            '}';
    }
}
