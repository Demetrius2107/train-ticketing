package com.trainticketing.business.domain;

import java.util.Date;

/**
 * <p>Title: TrainCarriage</p>
 * <p>Description: 车厢实体，与 train_carriage 表 1:1 对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainCarriage {

    /** 主键ID */
    private Long id;

    /** 车次id */
    private Long trainId;

    /** 车厢号，从1开始 */
    private Integer carriageIndex;

    /** 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧 */
    private String seatType;

    /** 座位数 */
    private Integer seatCount;

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
        sb.append(", carriageIndex=").append(carriageIndex);
        sb.append(", seatType=").append(seatType);
        sb.append(", seatCount=").append(seatCount);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}
