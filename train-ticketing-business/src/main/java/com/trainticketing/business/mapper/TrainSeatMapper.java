package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainSeat;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainSeatMapper</p>
 * <p>Description: 座位表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainSeatMapper {

  /**
   * 批量新增座位（座位生成用）
   *
   * @param list 座位列表
   * @return 影响行数
   */
  int insertBatch(@Param("list") List<TrainSeat> list);

  /**
   * 统计车厢已生成座位数（幂等校验用）
   *
   * @param carriageId 车厢ID
   * @return 座位数
   */
  int selectCountByCarriageId(@Param("carriageId") Long carriageId);

  /**
   * 按车次查询全部座位（座位图用，按车厢/排/字母排序）
   *
   * @param trainId 车次ID
   * @return 座位列表
   */
  List<TrainSeat> selectByTrainId(@Param("trainId") Long trainId);
}
