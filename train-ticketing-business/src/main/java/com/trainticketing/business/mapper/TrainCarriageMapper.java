package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainCarriage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainCarriageMapper</p>
 * <p>Description: 车厢表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainCarriageMapper {

  /**
   * 新增车厢
   *
   * @param record 车厢实体
   * @return 影响行数
   */
  int insert(TrainCarriage record);

  /**
   * 按车次查询车厢列表（按厢号排序）
   *
   * @param trainId 车次ID
   * @return 车厢列表
   */
  List<TrainCarriage> selectByTrainId(@Param("trainId") Long trainId);

  /**
   * 按车次+厢号查询（厢号唯一性校验用）
   *
   * @param trainId        车次ID
   * @param carriageIndex  厢号
   * @return 车厢，不存在返回 null
   */
  TrainCarriage selectByIndex(@Param("trainId") Long trainId, @Param("carriageIndex") Integer carriageIndex);

  /**
   * 按主键删除车厢
   *
   * @param id 车厢ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
