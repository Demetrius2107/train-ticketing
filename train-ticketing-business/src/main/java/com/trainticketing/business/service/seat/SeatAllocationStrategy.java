package com.trainticketing.business.service.seat;

import com.trainticketing.business.domain.DailyTrainSeat;
import java.util.List;

/**
 * <p>Title: SeatAllocationStrategy</p>
 * <p>Description: 选座策略接口。
 * 输入某区间+座位类型的全部可售座位（已加行锁），按策略选出 need 个座位。
 * 不同实现代表不同选座算法，可替换扩展（如贪心相邻、图着色最优、随机等）。</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-17
 * @updateTime 2026-08-17
 */
public interface SeatAllocationStrategy {

    /**
     * 从可售座位中选出 need 个
     *
     * @param availableSeats 全部可售座位（已按 carriage_id/seat_index/seat_label 排序，已 FOR UPDATE）
     * @param seatType       座位类型编码
     * @param need           需求座位数
     * @return 选中的座位列表；不足返回 null（由调用方判余票不足）
     */
    List<DailyTrainSeat> allocate(List<DailyTrainSeat> availableSeats, String seatType, int need);
}
