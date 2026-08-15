package com.trainticketing.business.mapper;

import com.trainticketing.business.domain.Train;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: TrainMapper</p>
 * <p>Description: 车次表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface TrainMapper {

  /**
   * 新增车次
   *
   * @param record 车次实体
   * @return 影响行数
   */
  int insert(Train record);

  /**
   * 按车次编号查询（唯一性校验用）
   *
   * @param code 车次编号
   * @return 车次，不存在返回 null
   */
  Train selectByCode(@Param("code") String code);

  /**
   * 按关键字模糊查询车次列表（车次编号）
   *
   * @param keyword 查询关键字，为空返回全部
   * @return 车次列表
   */
  List<Train> selectList(@Param("keyword") String keyword);

  /**
   * 按主键删除车次
   *
   * @param id 车次ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
