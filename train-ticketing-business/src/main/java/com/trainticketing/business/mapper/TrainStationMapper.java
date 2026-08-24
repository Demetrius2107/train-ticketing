package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.TrainStation;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainStationMapper</p>
 * <p>Description: 车次经停站表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainStationMapper {

  /**
   * 新增经停站
   *
   * @param record 经停站实体
   * @return 影响行数
   */
  int insert(TrainStation record);

  /**
   * 按主键查询经停站（删除保护用）
   *
   * @param id 经停站ID
   * @return 经停站，不存在返回 null
   */
  TrainStation selectByPrimaryKey(@Param("id") Long id);

  /**
   * 按车次查询经停站列表（按站序排序）
   *
   * @param trainId 车次ID
   * @return 经停站列表
   */
  List<TrainStation> selectByTrainId(@Param("trainId") Long trainId);

  /**
   * 按车次+站序查询（站序唯一性校验用）
   *
   * @param trainId      车次ID
   * @param stationIndex 站序
   * @return 经停站，不存在返回 null
   */
  TrainStation selectByIndex(@Param("trainId") Long trainId, @Param("stationIndex") Integer stationIndex);

  /**
   * 按车次+车站查询（站名唯一性校验用）
   *
   * @param trainId   车次ID
   * @param stationId 车站ID
   * @return 经停站，不存在返回 null
   */
  TrainStation selectByStation(@Param("trainId") Long trainId, @Param("stationId") Long stationId);

  /**
   * 按主键删除经停站
   *
   * @param id 经停站ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
