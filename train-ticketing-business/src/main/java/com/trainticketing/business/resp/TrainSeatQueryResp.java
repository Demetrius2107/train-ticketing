package com.trainticketing.business.resp;

/**
 * <p>Title: TrainSeatQueryResp</p>
 * <p>Description: 座位查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainSeatQueryResp {

    /** 主键ID */
    private Long id;

    /** 车次id */
    private Long trainId;

    /** 车厢id */
    private Long carriageId;

    /** 排号 */
    private Integer seatIndex;

    /** 座位字母 */
    private String seatLabel;

    /** 座位类型|枚举[SeatTypeEnum] */
    private String seatType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Long getCarriageId() {
        return carriageId;
    }

    public void setCarriageId(Long carriageId) {
        this.carriageId = carriageId;
    }

    public Integer getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(Integer seatIndex) {
        this.seatIndex = seatIndex;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    @Override
    public String toString() {
        return "TrainSeatQueryResp{" +
            "id=" + id +
            ", trainId=" + trainId +
            ", carriageId=" + carriageId +
            ", seatIndex=" + seatIndex +
            ", seatLabel='" + seatLabel + '\'' +
            ", seatType='" + seatType + '\'' +
            '}';
    }
}
