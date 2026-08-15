package com.trainticketing.business.resp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * <p>Title: TrainTicketResp</p>
 * <p>Description: 车次余票查询结果（用户侧：车次信息 + 各座位类型区间余票）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainTicketResp {

    /** 排班ID */
    private Long id;

    /** 车次ID */
    private Long trainId;

    /** 车次编号，如 G1234 */
    private String trainCode;

    /** 运行日期 */
    private LocalDate runDate;

    /** 始发站发车时间 */
    private LocalTime startTime;

    /** 终到站到达时间 */
    private LocalTime endTime;

    /** 出发站id */
    private Long departStationId;

    /** 到达站id */
    private Long arriveStationId;

    /** 出发站序（区间占用起点） */
    private Integer departIndex;

    /** 到达站序（区间占用终点） */
    private Integer arriveIndex;

    /** 各座位类型区间余票 */
    private List<SeatRemainingResp> remainingList;

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

    public String getTrainCode() {
        return trainCode;
    }

    public void setTrainCode(String trainCode) {
        this.trainCode = trainCode;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
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

    public List<SeatRemainingResp> getRemainingList() {
        return remainingList;
    }

    public void setRemainingList(List<SeatRemainingResp> remainingList) {
        this.remainingList = remainingList;
    }

    @Override
    public String toString() {
        return "TrainTicketResp{" +
            "id=" + id +
            ", trainId=" + trainId +
            ", trainCode='" + trainCode + '\'' +
            ", runDate=" + runDate +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            ", departStationId=" + departStationId +
            ", arriveStationId=" + arriveStationId +
            ", departIndex=" + departIndex +
            ", arriveIndex=" + arriveIndex +
            ", remainingList=" + remainingList +
            '}';
    }
}
