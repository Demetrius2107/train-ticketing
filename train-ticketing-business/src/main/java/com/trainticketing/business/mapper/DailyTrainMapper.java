package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.DailyTrain;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: DailyTrainMapper</p>
 * <p>Description: 每日车次表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface DailyTrainMapper {

  /**
   * 新增每日车次（排班）
   *
   * @param record 每日车次实体
   * @return 影响行数
   */
  int insert(DailyTrain record);

  /**
   * 按车次+日期查询（唯一性校验用）
   *
   * @param trainId 车次ID
   * @param runDate 运行日期
   * @return 排班，不存在返回 null
   */
  DailyTrain selectByTrainAndDate(@Param("trainId") Long trainId, @Param("runDate") LocalDate runDate);

  /**
   * 按主键查询排班（当日座位生成时校验引用用）
   *
   * @param id 排班ID
   * @return 排班，不存在返回 null
   */
  DailyTrain selectById(@Param("id") Long id);

  /**
   * 按车次/日期动态查询排班列表
   *
   * @param trainId 车次ID，可空
   * @param runDate 运行日期，可空
   * @return 排班列表
   */
  List<DailyTrain> selectList(@Param("trainId") Long trainId, @Param("runDate") LocalDate runDate);

  /**
   * 更新排班状态（停运/恢复运行）
   *
   * @param id     排班ID
   * @param status 状态|枚举[DailyTrainStatusEnum]
   * @return 影响行数
   */
  int updateStatus(@Param("id") Long id, @Param("status") String status);

  /**
   * 按主键删除排班
   *
   * @param id 排班ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
