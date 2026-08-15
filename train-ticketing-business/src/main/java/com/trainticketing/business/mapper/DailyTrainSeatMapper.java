package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.DailyTrainSeat;
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
}
