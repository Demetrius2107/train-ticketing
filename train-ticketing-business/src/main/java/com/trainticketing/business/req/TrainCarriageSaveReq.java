package com.trainticketing.business.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * <p>Title: TrainCarriageSaveReq</p>
 * <p>Description: 车厢新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class TrainCarriageSaveReq {

    /**
     * 车次id
     */
    @NotNull(message = "[车次]不能为空")
    private Long trainId;

    /**
     * 车厢号，从1开始
     */
    @NotNull(message = "[车厢号]不能为空")
    @Min(value = 1, message = "[车厢号]从1开始")
    private Integer carriageIndex;

    /**
     * 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧
     */
    @NotBlank(message = "[座位类型]不能为空")
    @Pattern(regexp = "^[1-5]$", message = "[座位类型]不合法")
    private String seatType;

    /**
     * 座位数
     */
    @NotNull(message = "[座位数]不能为空")
    @Min(value = 1, message = "[座位数]至少1个")
    private Integer seatCount;

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

    @Override
    public String toString() {
        return "TrainCarriageSaveReq{" +
                "trainId=" + trainId +
                ", carriageIndex=" + carriageIndex +
                ", seatType='" + seatType + '\'' +
                ", seatCount=" + seatCount +
                '}';
    }
}
