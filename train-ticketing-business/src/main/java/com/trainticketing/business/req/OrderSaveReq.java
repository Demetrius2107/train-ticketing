package com.trainticketing.business.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>Title: OrderSaveReq</p>
 * <p>Description: 下单请求（一个乘车人对应一条订单明细）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class OrderSaveReq {

    /** 会员id */
    @NotNull(message = "[会员id]不能为空")
    private Long memberId;

    /** 幂等键（前端生成，同一批下单请求携带相同值，防重复提交） */
    @NotBlank(message = "[幂等键]不能为空")
    private String idempotentKey;

    /** 排班id */
    @NotNull(message = "[排班id]不能为空")
    private Long dailyTrainId;

    /** 出发站id */
    @NotNull(message = "[出发站id]不能为空")
    private Long departStationId;

    /** 到达站id */
    @NotNull(message = "[到达站id]不能为空")
    private Long arriveStationId;

    /** 乘车日期 */
    @NotNull(message = "[乘车日期]不能为空")
    private LocalDate runDate;

    /** 座位类型|枚举[SeatTypeEnum] */
    @NotBlank(message = "[座位类型]不能为空")
    private String seatType;

    /** 乘车人列表（姓名+身份证，下单时快照） */
    @NotEmpty(message = "[乘车人]不能为空")
    private List<PassengerReq> passengers;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public void setIdempotentKey(String idempotentKey) {
        this.idempotentKey = idempotentKey;
    }

    public Long getDailyTrainId() {
        return dailyTrainId;
    }

    public void setDailyTrainId(Long dailyTrainId) {
        this.dailyTrainId = dailyTrainId;
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

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public List<PassengerReq> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<PassengerReq> passengers) {
        this.passengers = passengers;
    }

    @Override
    public String toString() {
        return "OrderSaveReq{" +
            "memberId=" + memberId +
            ", idempotentKey='" + idempotentKey + '\'' +
            ", dailyTrainId=" + dailyTrainId +
            ", departStationId=" + departStationId +
            ", arriveStationId=" + arriveStationId +
            ", runDate=" + runDate +
            ", seatType='" + seatType + '\'' +
            ", passengers=" + passengers +
            '}';
    }

    /**
     * <p>Title: PassengerReq</p>
     * <p>Description: 乘车人下单信息（快照写入订单明细）</p>
     */
    public static class PassengerReq {

        /** 乘车人id */
        private Long passengerId;

        /** 姓名 */
        @NotBlank(message = "[乘车人姓名]不能为空")
        private String name;

        /** 身份证号 */
        @NotBlank(message = "[身份证号]不能为空")
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

        @Override
        public String toString() {
            return "PassengerReq{" +
                "passengerId=" + passengerId +
                ", name='" + name + '\'' +
                ", idCard='" + idCard + '\'' +
                '}';
        }
    }
}
