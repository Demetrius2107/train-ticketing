package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.Station;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: StationMapper</p>
 * <p>Description: 车站表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface StationMapper {

  /**
   * 新增车站
   *
   * @param record 车站实体
   * @return 影响行数
   */
  int insert(Station record);

  /**
   * 按名称查询车站（唯一性校验用）
   *
   * @param name 车站名称
   * @return 车站，不存在返回 null
   */
  Station selectByName(@Param("name") String name);

  /**
   * 按关键字模糊查询车站列表（名称/拼音全拼/拼音简拼/城市）
   *
   * @param keyword 查询关键字，为空返回全部
   * @return 车站列表
   */
  List<Station> selectList(@Param("keyword") String keyword);

  /**
   * 按主键删除车站
   *
   * @param id 车站ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
