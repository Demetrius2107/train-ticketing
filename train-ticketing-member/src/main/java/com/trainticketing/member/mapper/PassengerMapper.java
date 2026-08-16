package com.trainticketing.member.mapper;

import com.trainticketing.member.domain.Passenger;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>Title: PassengerMapper</p>
 * <p>Description: 乘车人表 Mapper</p>
 * <p>项目名称: TrainTicketing</p>
 *
 * @author wanqiu
 * @since 1.0
 * @createTime 2026-08-16
 * @updateTime 2026-08-16
 */
public interface PassengerMapper {

  /**
   * 新增乘车人
   *
   * @param record 乘车人实体
   * @return 影响行数
   */
  int insert(Passenger record);

  /**
   * 按主键查询乘车人
   *
   * @param id 乘车人ID
   * @return 乘车人，不存在返回 null
   */
  Passenger selectById(@Param("id") Long id);

  /**
   * 按会员查询乘车人列表
   *
   * @param memberId 会员ID
   * @return 乘车人列表
   */
  List<Passenger> selectByMemberId(@Param("memberId") Long memberId);

  /**
   * 按主键删除乘车人
   *
   * @param id 乘车人ID
   * @return 影响行数
   */
  int deleteById(@Param("id") Long id);
}
