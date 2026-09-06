package com.trainticketing.business.message;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>Title: OrderCreateMessage</p>
 * <p>Description: 异步下单出票消息体。设计要点：
 * 1. 显式携带出票所需的全部参数（含区间站序与单价），消费端零回查，选座失败分型简单；
 * 2. 不携带 memberId/乘车日期等订单行已有信息——订单行由生产者预插，消费者只补明细；
 * 3. orderId 为预生成雪花 ID，兼作消费幂等键。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-06
 * @since 1.0
 */
public class OrderCreateMessage {

    /**
     * 预生成订单id（消费幂等键）
     */
    private Long orderId;

    /**
     * 预生成订单号
     */
    private String orderNo;

    /**
     * 排班id
     */
    private Long dailyTrainId;

    /**
     * 座位类型|枚举[SeatTypeEnum]
     */
    private String seatType;

    /**
     * 出发站序（区间占用起点，生产者已校验合法性）
     */
    private Integer departIndex;

    /**
     * 到达站序（区间占用终点）
     */
    private Integer arriveIndex;

    /**
     * 票价单价（元），生产者已按车次+座位类型查好
     */
    private BigDecimal unitPrice;

    /**
     * 乘车人列表（下单快照）
     */
    private List<Passenger> passengers;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getDailyTrainId() {
        return dailyTrainId;
    }

    public void setDailyTrainId(Long dailyTrainId) {
        this.dailyTrainId = dailyTrainId;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    @Override
    public String toString() {
        return "OrderCreateMessage{" +
                "orderId=" + orderId +
                ", orderNo='" + orderNo + '\'' +
                ", dailyTrainId=" + dailyTrainId +
                ", seatType='" + seatType + '\'' +
                ", departIndex=" + departIndex +
                ", arriveIndex=" + arriveIndex +
                ", unitPrice=" + unitPrice +
                ", passengers=" + (passengers == null ? 0 : passengers.size()) + "人" +
                '}';
    }

    /**
     * <p>Title: Passenger</p>
     * <p>Description: 乘车人出票信息（快照写入订单明细）
     * 注意：idCard 为敏感字段，toString 已刻意排除</p>
     */
    public static class Passenger {

        /**
         * 乘车人id
         */
        private Long passengerId;

        /**
         * 姓名
         */
        private String name;

        /**
         * 身份证号（快照）
         */
        private String idCard;

        public Long getPassengerId() {
            return passengerId;
        }

        public void setPassengerId(Long passengerId) {
            this.passengerId = passengerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIdCard() {
            return idCard;
        }

        public void setIdCard(String idCard) {
            this.idCard = idCard;
        }
    }
}
