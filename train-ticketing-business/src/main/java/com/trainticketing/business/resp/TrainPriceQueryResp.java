package com.trainticketing.business.resp;

import java.math.BigDecimal;

/**
 * <p>Title: TrainPriceQueryResp</p>
 * <p>Description: 票价查询结果</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public class TrainPriceQueryResp {

    /** 主键ID */
    private Long id;

    /** 车次id */
    private Long trainId;

    /** 座位类型|枚举[SeatTypeEnum] */
    private String seatType;

    /** 票价（元） */
    private BigDecimal price;

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
        return "TrainPriceQueryResp{" +
            "id=" + id +
            ", trainId=" + trainId +
            ", seatType='" + seatType + '\'' +
            ", price=" + price +
            '}';
    }
}
