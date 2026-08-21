package com.trainticketing.business.resp;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * <p>Title: DailyTrainQueryResp</p>
 * <p>Description: 每日车次（排班）查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class DailyTrainQueryResp {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 车次id
     */
    private Long trainId;

    /**
     * 运行日期
     */
    private LocalDate runDate;

    /**
     * 始发站id
     */
    private Long startStationId;

    /**
     * 终到站id
     */
    private Long endStationId;

    /**
     * 始发站发车时间
     */
    private LocalTime startTime;

    /**
     * 终到站到达时间
     */
    private LocalTime endTime;

    /**
     * 状态|枚举[DailyTrainStatusEnum]: 0停运 1运行
     */
    private String status;

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

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public Long getStartStationId() {
        return startStationId;
    }

    public void setStartStationId(Long startStationId) {
        this.startStationId = startStationId;
    }

    public Long getEndStationId() {
        return endStationId;
    }

    public void setEndStationId(Long endStationId) {
        this.endStationId = endStationId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DailyTrainQueryResp{" +
                "id=" + id +
                ", trainId=" + trainId +
                ", runDate=" + runDate +
                ", startStationId=" + startStationId +
                ", endStationId=" + endStationId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}
