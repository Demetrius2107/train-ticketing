package com.trainticketing.business.domain;

import java.util.Date;

/**
 * <p>Title: DailyTrainSeat</p>
 * <p>Description: 当日座位（余票/售卖状态）实体，与 daily_train_seat 表 1:1 对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class DailyTrainSeat {

    /** 主键ID */
    private Long id;

    /** 每日车次id */
    private Long dailyTrainId;

    /** 座位档案id */
    private Long trainSeatId;

    /** 车厢id */
    private Long carriageId;

    /** 排号 */
    private Integer seatIndex;

    /** 座位字母 */
    private String seatLabel;

    /** 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧 */
    private String seatType;

    /** 售卖状态: 0可售 1已售 2锁定 */
    private String saleStatus;

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

    public Long getDailyTrainId() {
        return dailyTrainId;
    }

    public void setDailyTrainId(Long dailyTrainId) {
        this.dailyTrainId = dailyTrainId;
    }

    public Long getTrainSeatId() {
        return trainSeatId;
    }

    public void setTrainSeatId(Long trainSeatId) {
        this.trainSeatId = trainSeatId;
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

    public String getSaleStatus() {
        return saleStatus;
    }

    public void setSaleStatus(String saleStatus) {
        this.saleStatus = saleStatus;
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
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", dailyTrainId=").append(dailyTrainId);
        sb.append(", trainSeatId=").append(trainSeatId);
        sb.append(", carriageId=").append(carriageId);
        sb.append(", seatIndex=").append(seatIndex);
        sb.append(", seatLabel=").append(seatLabel);
        sb.append(", seatType=").append(seatType);
        sb.append(", saleStatus=").append(saleStatus);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}
