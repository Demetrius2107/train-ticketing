package com.trainticketing.business.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * <p>Title: TrainPriceSaveReq</p>
 * <p>Description: 票价新增请求</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainPriceSaveReq {

    /** 车次id */
    @NotNull(message = "[车次]不能为空")
    private Long trainId;

    /** 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧 */
    @NotBlank(message = "[座位类型]不能为空")
    @Pattern(regexp = "^[1-5]$", message = "[座位类型]不合法")
    private String seatType;

    /** 票价（元） */
    @NotNull(message = "[票价]不能为空")
    @DecimalMin(value = "0.01", message = "[票价]必须大于0")
    private BigDecimal price;

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "TrainPriceSaveReq{" +
            "trainId=" + trainId +
            ", seatType='" + seatType + '\'' +
            ", price=" + price +
            '}';
    }
}
