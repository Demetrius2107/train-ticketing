package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainPrice;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainPriceMapper</p>
 * <p>Description: 票价表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainPriceMapper {

  /**
   * 新增票价
   *
   * @param record 票价实体
   * @return 影响行数
   */
  int insert(TrainPrice record);

  /**
   * 按车次查询票价列表（按座位类型排序）
   *
   * @param trainId 车次ID
   * @return 票价列表
   */
  List<TrainPrice> selectByTrainId(@Param("trainId") Long trainId);

  /**
   * 按车次+座位类型查询（唯一性校验用）
   *
   * @param trainId  车次ID
   * @param seatType 座位类型
   * @return 票价，不存在返回 null
   */
  TrainPrice selectByTrainAndType(@Param("trainId") Long trainId, @Param("seatType") String seatType);

  /**
   * 按主键删除票价
   *
   * @param id 票价ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
