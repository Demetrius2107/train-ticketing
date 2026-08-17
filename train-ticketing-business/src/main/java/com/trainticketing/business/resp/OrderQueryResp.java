package com.trainticketing.business.resp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * <p>Title: OrderQueryResp</p>
 * <p>Description: 订单查询响应（订单头 + 明细列表）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class OrderQueryResp {

    /** 订单id */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 会员id */
    private Long memberId;

    /** 车次id */
    private Long trainId;

    /** 出发站id */
    private Long departStationId;

    /** 到达站id */
    private Long arriveStationId;

    /** 乘车日期 */
    private Date runDate;

    /** 状态|枚举[OrderStatusEnum] */
    private String status;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /** 支付时间 */
    private Date payTime;

    /** 退款时间 */
    private Date refundTime;

    /** 下单时间 */
    private Date createTime;

    /** 明细列表 */
    private List<OrderItemResp> items;

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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

    public List<OrderItemResp> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResp> items) {
        this.items = items;
    }

    /**
     * <p>Title: OrderItemResp</p>
     * <p>Description: 订单明细响应（乘车人快照 + 座位 + 区间占位）</p>
     */
    public static class OrderItemResp {

        /** 乘车人姓名 */
        private String passengerName;

        /** 身份证号 */
        private String idCard;

        /** 座位类型|枚举[SeatTypeEnum] */
        private String seatType;

        /** 票价（元） */
        private BigDecimal price;

        /** 出发站序 */
        private Integer departIndex;

        /** 到达站序 */
        private Integer arriveIndex;

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
    }
}
