package com.trainticketing.business.resp;

/**
 * <p>Title: SeatRemainingBatchResp</p>
 * <p>Description: 区间余票批量查询结果（车次列表页用：一行 = 排班×座位类型 的余票），
 * 各排班的查询区间由 join train_station 按各自站序推得</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @createTime 2026-09-10
 * @since 1.0
 */
public class SeatRemainingBatchResp {

    /**
     * 排班ID
     */
    private Long dailyTrainId;

    /**
     * 座位类型|枚举[SeatTypeEnum]: 1商务座 2一等座 3二等座 4硬卧 5软卧
     */
    private String seatType;

    /**
     * 剩余票数
     */
    private Long remainingCount;

    public Long getDailyTrainId() {
        return dailyTrainId;
    }

    public void setDailyTrainId(Long dailyTrainId) {
        this.dailyTrainId = dailyTrainId;
    }

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
        return "SeatRemainingBatchResp{" +
                "dailyTrainId=" + dailyTrainId +
                ", seatType='" + seatType + '\'' +
                ", remainingCount=" + remainingCount +
                '}';
    }
}
