package com.trainticketing.business.resp;

/**
 * <p>Title: SeatRemainingResp</p>
 * <p>Description: 区间余票查询结果（按座位类型统计）</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 * @since 1.0
 */
public class SeatRemainingResp {

    /**
     * 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧
     */
    private String seatType;

    /**
     * 剩余票数
     */
    private Long remainingCount;

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public Long getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(Long remainingCount) {
        this.remainingCount = remainingCount;
    }

    @Override
    public String toString() {
        return "SeatRemainingResp{" +
                "seatType='" + seatType + '\'' +
                ", remainingCount=" + remainingCount +
                '}';
    }
}
