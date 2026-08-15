package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.DailyTrainSeat;
import com.trainticketing.business.resp.SeatRemainingResp;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: DailyTrainSeatMapper</p>
 * <p>Description: 当日座位表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface DailyTrainSeatMapper {

  /**
   * 批量新增当日座位（排班座位生成用）
   *
   * @param list 当日座位列表
   * @return 影响行数
   */
  int insertBatch(@Param("list") List<DailyTrainSeat> list);

  /**
   * 统计排班已生成当日座位数（幂等校验用）
   *
   * @param dailyTrainId 排班ID
   * @return 座位数
   */
  int selectCountByDailyTrainId(@Param("dailyTrainId") Long dailyTrainId);

  /**
   * 按排班查询当日座位列表
   *
   * @param dailyTrainId 排班ID
   * @return 当日座位列表
   */
  List<DailyTrainSeat> selectByDailyTrainId(@Param("dailyTrainId") Long dailyTrainId);

  /**
   * 按区间统计余票（区间占用模型）：
   * 余票 = 该排班内 sale_status='0' 且未被"区间重叠的已支付订单"占用的座位数。
   * 区间重叠判定：订单明细占用的 [departIndex, arriveIndex] 与查询区间重叠
   * 当且仅当 departIndex &lt;= 查询终点 且 arriveIndex &gt;= 查询起点。
   *
   * @param dailyTrainId 排班ID
   * @param departIndex  查询区间起点站序
   * @param arriveIndex  查询区间终点站序
   * @return 各座位类型余票
   */
  List<SeatRemainingResp> selectRemainingByInterval(@Param("dailyTrainId") Long dailyTrainId,
                                                    @Param("departIndex") Integer departIndex,
                                                    @Param("arriveIndex") Integer arriveIndex);
}
