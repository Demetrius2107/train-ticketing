package com.trainticketing.business.domain;

import java.util.Date;

/**
 * <p>Title: TrainSeat</p>
 * <p>Description: 座位实体，与 train_seat 表 1:1 对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class TrainSeat {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 车次id
     */
    private Long trainId;

    /**
     * 车厢id
     */
    private Long carriageId;

    /**
     * 排号
     */
    private Integer seatIndex;

    /**
     * 座位字母: A B C D F
     */
    private String seatLabel;

    /**
     * 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧
     */
    private String seatType;

    /**
     * 新增时间
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
        sb.append(", trainId=").append(trainId);
        sb.append(", carriageId=").append(carriageId);
        sb.append(", seatIndex=").append(seatIndex);
        sb.append(", seatLabel=").append(seatLabel);
        sb.append(", seatType=").append(seatType);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}
