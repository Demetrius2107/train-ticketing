package com.trainticketing.business.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * <p>Title: TrainSaveReq</p>
 * <p>Description: 车次新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainSaveReq {

    /** 车次编号，如 G1234（全局唯一） */
    @NotBlank(message = "[车次编号]不能为空")
    @Pattern(regexp = "^[A-Z]\\d{1,4}$", message = "[车次编号]格式错误，如 G1234")
    @Size(max = 10, message = "[车次编号]最长10个字符")
    private String code;

    /** 车次类型|枚举[TrainTypeEnum]: 1高铁 2动车 3特快 4普快 */
    @NotBlank(message = "[车次类型]不能为空")
    @Pattern(regexp = "^[1-4]$", message = "[车次类型]不合法")
    private String type;

    /** 始发站id */
    @NotNull(message = "[始发站]不能为空")
    private Long startStationId;

    /** 终到站id */
    @NotNull(message = "[终到站]不能为空")
    private Long endStationId;

    /** 始发站发车时间，如 08:00 */
    @NotBlank(message = "[始发时间]不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "[始发时间]格式错误，如 08:00")
    private String startTime;

    /** 终到站到达时间，如 10:30 */
    @NotBlank(message = "[到达时间]不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$", message = "[到达时间]格式错误，如 10:30")
    private String endTime;

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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "TrainSaveReq{" +
            "code='" + code + '\'' +
            ", type='" + type + '\'' +
            ", startStationId=" + startStationId +
            ", endStationId=" + endStationId +
            ", startTime='" + startTime + '\'' +
            ", endTime='" + endTime + '\'' +
            '}';
    }
}
