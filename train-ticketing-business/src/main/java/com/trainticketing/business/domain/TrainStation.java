package com.trainticketing.business.domain;

import java.time.LocalTime;
import java.util.Date;

/**
 * <p>Title: TrainStation</p>
 * <p>Description: 车次经停站实体，与 train_station 表 1:1 对应</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainStation {

    /** 主键ID */
    private Long id;

    /** 车次id */
    private Long trainId;

    /** 车站id */
    private Long stationId;

    /** 站序，从1开始 */
    private Integer stationIndex;

    /** 到达时间（始发站为空） */
    private LocalTime arriveTime;

    /** 发车时间（终到站为空） */
    private LocalTime leaveTime;

    /** 停靠分钟数 */
    private Integer stopMinutes;

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

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public Integer getStationIndex() {
        return stationIndex;
    }

    public void setStationIndex(Integer stationIndex) {
        this.stationIndex = stationIndex;
    }

    public LocalTime getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(LocalTime arriveTime) {
        this.arriveTime = arriveTime;
    }

    public LocalTime getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(LocalTime leaveTime) {
        this.leaveTime = leaveTime;
    }

    public Integer getStopMinutes() {
        return stopMinutes;
    }

    public void setStopMinutes(Integer stopMinutes) {
        this.stopMinutes = stopMinutes;
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
        sb.append(", stationId=").append(stationId);
        sb.append(", stationIndex=").append(stationIndex);
        sb.append(", arriveTime=").append(arriveTime);
        sb.append(", leaveTime=").append(leaveTime);
        sb.append(", stopMinutes=").append(stopMinutes);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}
