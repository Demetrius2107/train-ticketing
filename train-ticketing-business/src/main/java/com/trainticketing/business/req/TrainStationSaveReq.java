package com.trainticketing.business.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * <p>Title: TrainStationSaveReq</p>
 * <p>Description: 车次经停站新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainStationSaveReq {

    /** 车次id */
    @NotNull(message = "[车次]不能为空")
    private Long trainId;

    /** 车站id */
    @NotNull(message = "[车站]不能为空")
    private Long stationId;

    /** 站序，从1开始 */
    @NotNull(message = "[站序]不能为空")
    @Min(value = 1, message = "[站序]从1开始")
    private Integer stationIndex;

    /** 到达时间，如 08:30（始发站不填） */
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "[到达时间]格式错误，如 08:30")
    private String arriveTime;

    /** 发车时间，如 08:32（终到站不填） */
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "[发车时间]格式错误，如 08:32")
    private String leaveTime;

    /** 停靠分钟数 */
    @Min(value = 0, message = "[停靠分钟]不能为负")
    private Integer stopMinutes;

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

    public String getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(String arriveTime) {
        this.arriveTime = arriveTime;
    }

    public String getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(String leaveTime) {
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
        return "TrainStationSaveReq{" +
            "trainId=" + trainId +
            ", stationId=" + stationId +
            ", stationIndex=" + stationIndex +
            ", arriveTime='" + arriveTime + '\'' +
            ", leaveTime='" + leaveTime + '\'' +
            ", stopMinutes=" + stopMinutes +
            '}';
    }
}
