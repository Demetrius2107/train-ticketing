package com.trainticketing.business.resp;

import java.time.LocalTime;

/**
 * <p>Title: TrainStationQueryResp</p>
 * <p>Description: 车次经停站查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainStationQueryResp {

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

    @Override
    public String toString() {
        return "TrainStationQueryResp{" +
            "id=" + id +
            ", trainId=" + trainId +
            ", stationId=" + stationId +
            ", stationIndex=" + stationIndex +
            ", arriveTime=" + arriveTime +
            ", leaveTime=" + leaveTime +
            ", stopMinutes=" + stopMinutes +
            '}';
    }
}
