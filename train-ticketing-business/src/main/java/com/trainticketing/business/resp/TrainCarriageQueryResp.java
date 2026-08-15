package com.trainticketing.business.resp;

/**
 * <p>Title: TrainCarriageQueryResp</p>
 * <p>Description: 车厢查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainCarriageQueryResp {

    /** 主键ID */
    private Long id;

    /** 车次id */
    private Long trainId;

    /** 车厢号 */
    private Integer carriageIndex;

    /** 座位类型|枚举[SeatTypeEnum] */
    private String seatType;

    /** 座位数 */
    private Integer seatCount;

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

    public Integer getCarriageIndex() {
        return carriageIndex;
    }

    public void setCarriageIndex(Integer carriageIndex) {
        this.carriageIndex = carriageIndex;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public Integer getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(Integer seatCount) {
        this.seatCount = seatCount;
    }

    @Override
    public String toString() {
        return "TrainCarriageQueryResp{" +
            "id=" + id +
            ", trainId=" + trainId +
            ", carriageIndex=" + carriageIndex +
            ", seatType='" + seatType + '\'' +
            ", seatCount=" + seatCount +
            '}';
    }
}
