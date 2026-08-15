package com.trainticketing.business.resp;

import java.time.LocalTime;

/**
 * <p>Title: TrainQueryResp</p>
 * <p>Description: 车次查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainQueryResp {

    /** 主键ID */
    private Long id;

    /** 车次编号 */
    private String code;

    /** 车次类型|枚举[TrainTypeEnum] */
    private String type;

    /** 始发站id */
    private Long startStationId;

    /** 终到站id */
    private Long endStationId;

    /** 始发站发车时间 */
    private LocalTime startTime;

    /** 终到站到达时间 */
    private LocalTime endTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "TrainQueryResp{" +
            "id=" + id +
            ", code='" + code + '\'' +
            ", type='" + type + '\'' +
            ", startStationId=" + startStationId +
            ", endStationId=" + endStationId +
            ", startTime=" + startTime +
            ", endTime=" + endTime +
            '}';
    }
}
